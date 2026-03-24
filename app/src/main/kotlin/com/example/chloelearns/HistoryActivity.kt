package com.example.chloelearns

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var font: Typeface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")

        val prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
        val raw = prefs.getString(MathGameActivity.HISTORY_KEY, "") ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFAFAFA.toInt())
        }

        // Header bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setBackgroundColor(0xFF3F51B5.toInt())
        }
        header.addView(TextView(this).apply {
            text = "\u2190  history"
            textSize = 22f
            typeface = font
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { finish() }
        })
        root.addView(header)

        if (raw.isBlank()) {
            root.addView(TextView(this).apply {
                text = "no games played yet!"
                textSize = 20f
                typeface = font
                setTextColor(0xFF9E9E9E.toInt())
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(48), dp(24), dp(24))
            })
        } else {
            root.addView(makeRow(this, "game", "mode", "score", "time", "date", isHeader = true))
            val lines = raw.split("\n").filter { it.isNotBlank() }
            for (line in lines) {
                val p = line.split("|")
                // new format: game|mode|correct|total|secs|date  (6 fields)
                // old format: mode|correct|total|secs|date        (5 fields) — skip
                if (p.size < 6) continue
                val game    = p[0]
                val mode    = p[1]
                val correct = p[2]
                val total   = p[3]
                val secs    = p[4].toLongOrNull() ?: 0L
                val date    = p[5]
                val timeStr = if (secs >= 60) "${secs / 60}m ${secs % 60}s" else "${secs}s"
                root.addView(makeRow(this, game, mode, "$correct / $total", timeStr, date))
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun makeRow(
        ctx: Context,
        game: String, mode: String, score: String, time: String, date: String,
        isHeader: Boolean = false
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(if (isHeader) 0xFFEEEEEE.toInt() else 0xFFFFFFFF.toInt())
        }

        val wrapper = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        wrapper.addView(row)
        wrapper.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(0xFFE0E0E0.toInt())
        })

        data class Col(val text: String, val weight: Float)
        val cols = listOf(Col(game, 1.4f), Col(mode, 1f), Col(score, 1f), Col(time, 1f), Col(date, 2f))

        for (col in cols) {
            row.addView(TextView(ctx).apply {
                text = col.text
                textSize = if (isHeader) 15f else 17f
                typeface = font
                setTextColor(0xFF212121.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, col.weight)
                if (isHeader) {
                    setTextColor(0xFF757575.toInt())
                } else if (col === cols[0]) {
                    setTextColor(if (game == MathGameActivity.GAME_ADDITION) 0xFF3F51B5.toInt() else 0xFF9C27B0.toInt())
                } else if (col === cols[1]) {
                    setTextColor(if (mode == MathGameActivity.MODE_EASY) 0xFF43A047.toInt() else 0xFFE53935.toInt())
                }
            })
        }
        return wrapper
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
