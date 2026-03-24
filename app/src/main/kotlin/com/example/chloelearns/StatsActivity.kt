package com.example.chloelearns

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatsActivity : AppCompatActivity() {

    private lateinit var font: Typeface
    private lateinit var contentLayout: LinearLayout
    private var resetCount = 0
    private val resetTaps = 7

    data class Stat(val key: String, val game: String, val num1: Int, val num2: Int, val score: Int, val attempts: Int)
    data class Mistake(val key: String, val givenAnswer: String, val correctAnswer: Int, val date: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFAFAFA.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setBackgroundColor(0xFF3F51B5.toInt())
        }
        header.addView(TextView(this).apply {
            text = "\u2190  stats"
            textSize = 22f
            typeface = font
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { finish() }
        })
        root.addView(header)

        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(contentLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Footer with reset button
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        footer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(0xFFE0E0E0.toInt())
        })

        val resetHint = TextView(this).apply {
            textSize = 12f
            typeface = font
            setTextColor(0xFF9E9E9E.toInt())
            gravity = Gravity.CENTER
            text = "tap $resetTaps times to reset all scores and history"
            setPadding(0, dp(8), 0, 0)
        }

        val resetBtn = TextView(this).apply {
            text = "reset"
            textSize = 16f
            typeface = font
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFBDBDBD.toInt())
            setPadding(dp(28), dp(10), dp(28), dp(10))
            gravity = Gravity.CENTER
            setOnClickListener {
                resetCount++
                if (resetCount >= resetTaps) {
                    AlertDialog.Builder(this@StatsActivity)
                        .setTitle("reset all data")
                        .setMessage("reset all scores and history?")
                        .setNegativeButton("cancel") { _, _ -> resetCount = 0 }
                        .setPositiveButton("reset") { _, _ ->
                            val prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
                            prefs.edit()
                                .remove("math_score")
                                .remove(MathGameActivity.HISTORY_KEY)
                                .remove(MathGameActivity.QUESTION_STATS_KEY)
                                .remove(MathGameActivity.MISTAKE_LOG_KEY)
                                .apply()
                            resetCount = 0
                            resetHint.text = "tap $resetTaps times to reset all scores and history"
                            buildTable()
                        }
                        .show()
                } else {
                    resetHint.text = "tap $resetTaps times to reset all scores and history ($resetCount/$resetTaps)"
                }
            }
        }
        footer.addView(resetBtn)
        footer.addView(resetHint)
        root.addView(footer)

        setContentView(root)
        buildTable()
    }

    private fun accuracy(s: Stat): Int {
        if (s.attempts == 0) return 0
        val correct = (s.score + s.attempts) / 2
        return ((correct.toFloat() / s.attempts) * 100).toInt()
    }

    private fun buildTable() {
        contentLayout.removeAllViews()
        val stats = loadStats()

        if (stats.isEmpty()) {
            contentLayout.addView(TextView(this).apply {
                text = "no questions answered yet!"
                textSize = 20f
                typeface = font
                setTextColor(0xFF9E9E9E.toInt())
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(48), dp(24), dp(24))
            })
            return
        }

        val sorted = stats.sortedBy { accuracy(it) }

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val allMistakes = loadMistakes()
        list.addView(makeRow("question", "accuracy", "tries", isHeader = true))

        for (s in sorted) {
            val op = if (s.game == "Minus") " \u2212 " else " + "
            val ans = if (s.game == "Minus") s.num1 - s.num2 else s.num1 + s.num2
            val question = "${s.num1}$op${s.num2} = $ans"
            val pct = accuracy(s)
            val rowMistakes = allMistakes.filter { it.key == s.key }

            val detailContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(dp(24), dp(8), dp(16), dp(8))
                setBackgroundColor(0xFFFAFAFA.toInt())
            }

            // left red border via a wrapper
            val detailWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                visibility = View.GONE
            }
            val redBar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(0xFFE53935.toInt())
            }
            detailWrapper.addView(redBar)
            detailWrapper.addView(detailContainer, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

            if (rowMistakes.isEmpty()) {
                detailContainer.addView(TextView(this).apply {
                    text = "no mistakes recorded"
                    textSize = 14f
                    typeface = font
                    setTextColor(0xFF9E9E9E.toInt())
                    setPadding(0, dp(4), 0, dp(4))
                })
            } else {
                for (m in rowMistakes) {
                    val mRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(4), 0, dp(4))
                    }
                    val label = if (m.givenAnswer == "timeout") "timeout" else "answered ${m.givenAnswer}"
                    mRow.addView(TextView(this).apply {
                        text = label
                        textSize = 15f
                        typeface = font
                        setTextColor(0xFFE53935.toInt())
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    mRow.addView(TextView(this).apply {
                        text = m.date
                        textSize = 14f
                        typeface = font
                        setTextColor(0xFF9E9E9E.toInt())
                    })
                    detailContainer.addView(mRow)
                }
            }

            val rowView = makeRow(question, "$pct%", "${s.attempts}", scoreColor = accuracyColor(pct))
            rowView.setOnClickListener {
                if (detailWrapper.visibility == View.VISIBLE) {
                    detailWrapper.visibility = View.GONE
                } else {
                    detailWrapper.visibility = View.VISIBLE
                }
            }
            list.addView(rowView)
            list.addView(detailWrapper)
        }

        scroll.addView(list)
        contentLayout.addView(scroll)
    }

    private fun loadStats(): List<Stat> {
        val prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
        val raw = prefs.getString(MathGameActivity.QUESTION_STATS_KEY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 6) {
                val key = parts[0]
                val score = parts[1].toIntOrNull() ?: 0
                val attempts = parts[2].toIntOrNull() ?: 0
                val game = parts[3]
                val num1 = parts[4].toIntOrNull() ?: 0
                val num2 = parts[5].toIntOrNull() ?: 0
                Stat(key, game, num1, num2, score, attempts)
            } else null
        }
    }

    private fun loadMistakes(): List<Mistake> {
        val prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
        val raw = prefs.getString(MathGameActivity.MISTAKE_LOG_KEY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            // format: key|givenAnswer|correctAnswer|game|num1|num2|date
            if (p.size >= 7) {
                Mistake(p[0], p[1], p[2].toIntOrNull() ?: 0, p[6])
            } else null
        }
    }

    private fun accuracyColor(pct: Int): Int {
        return when {
            pct >= 80 -> 0xFF43A047.toInt()
            pct >= 60 -> 0xFF66BB6A.toInt()
            pct >= 40 -> 0xFFFFA726.toInt()
            pct >= 20 -> 0xFFEF5350.toInt()
            else      -> 0xFFE53935.toInt()
        }
    }

    private fun makeRow(question: String, score: String, tries: String, isHeader: Boolean = false, scoreColor: Int = 0xFF212121.toInt()): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(if (isHeader) 0xFFEEEEEE.toInt() else 0xFFFFFFFF.toInt())
        }

        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrapper.addView(row)
        wrapper.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(0xFFE0E0E0.toInt())
        })

        row.addView(TextView(this).apply {
            text = question
            textSize = 17f
            typeface = font
            setTextColor(if (isHeader) 0xFF757575.toInt() else 0xFF212121.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
        })

        row.addView(TextView(this).apply {
            text = score
            textSize = 17f
            typeface = font
            gravity = Gravity.CENTER
            setTextColor(if (isHeader) 0xFF757575.toInt() else scoreColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        row.addView(TextView(this).apply {
            text = tries
            textSize = 17f
            typeface = font
            gravity = Gravity.CENTER
            setTextColor(if (isHeader) 0xFF757575.toInt() else 0xFF212121.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        return wrapper
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
