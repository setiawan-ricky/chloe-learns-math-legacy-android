package com.example.chloelearns

import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class LscwcActivity : AppCompatActivity() {

    companion object {
        val ALL_WORDS = SpellingActivity.ALL_WORDS
        const val HISTORY_SIZE = 3
    }

    private var word = ""
    private var hidden = false
    private var lang = "en"
    private val recent = mutableListOf<String>()
    private lateinit var font: Typeface
    private lateinit var wordRow: LinearLayout
    private lateinit var btnHide: TextView
    private var mediaPlayer: MediaPlayer? = null

    private fun playAssetSound(path: String) {
        try {
            val old = mediaPlayer
            val afd: AssetFileDescriptor = assets.openFd(path)
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setVolume(1f, 1f)
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener { it.release() }
            mediaPlayer = mp
            old?.release()
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
        lang = prefs.getString(MainActivity.PREF_LANG, "en") ?: "en"
        font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FAFAFA"))
        }

        // Header
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#3F51B5"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val txtBack = TextView(this).apply {
            text = if (lang == "zh") "← 看说盖写查" else "← look say cover write check"
            textSize = 22f; typeface = font
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        }
        header.addView(txtBack)
        root.addView(header)

        // Content
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        wordRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(40) }
        }
        content.addView(wordRow)

        // Audio buttons
        val audioRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(32) }
        }
        val btnWord = makeAudioBtn(if (lang == "zh") "🔊 单词" else "🔊 word") {
            playAssetSound("audio/en/spelling/${word}-word.mp3")
        }
        val btnSentence = makeAudioBtn(if (lang == "zh") "📖 句子" else "📖 sentence") {
            playAssetSound("audio/en/spelling/${word}-sentence.mp3")
        }
        audioRow.addView(btnWord)
        audioRow.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), 0) })
        audioRow.addView(btnSentence)
        content.addView(audioRow)

        // Action buttons
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        btnHide = makeActionBtn(if (lang == "zh") "盖住" else "hide", "#FFA726", "#E65100") {
            hidden = !hidden
            buildWord()
            btnHide.text = if (hidden) {
                if (lang == "zh") "显示" else "show"
            } else {
                if (lang == "zh") "盖住" else "hide"
            }
        }
        val btnNext = makeActionBtn(if (lang == "zh") "下一个" else "next", "#42A5F5", "#1565C0") {
            nextWord()
        }
        actionRow.addView(btnHide)
        actionRow.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20), 0) })
        actionRow.addView(btnNext)
        content.addView(actionRow)

        root.addView(content)
        setContentView(root)

        pickFirst()
    }

    private fun pickFirst() {
        word = ALL_WORDS[Random.nextInt(ALL_WORDS.size)]
        recent.clear()
        recent.add(word)
        hidden = false
        buildWord()
    }

    private fun nextWord() {
        val pool = ALL_WORDS.filter { it !in recent }
        word = pool[Random.nextInt(pool.size)]
        if (recent.size >= HISTORY_SIZE) recent.removeAt(0)
        recent.add(word)
        hidden = false
        buildWord()
        btnHide.text = if (lang == "zh") "盖住" else "hide"
    }

    private fun buildWord() {
        wordRow.removeAllViews()
        val screenW = resources.displayMetrics.widthPixels
        val sidePad = dp(48)
        val maxTile = (screenW - sidePad * 2) / word.length
        val tile = minOf(dp(120), maxTile)
        val boxW = (tile * 0.65).toInt()
        val fs = (tile * 0.8f / resources.displayMetrics.density)

        for (i in word.indices) {
            val box = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(boxW, tile)
            }
            if (hidden) {
                box.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(Color.parseColor("#BDBDBD"))
                })
            } else {
                box.addView(TextView(this).apply {
                    text = word[i].toString()
                    textSize = fs; typeface = font
                    setTextColor(Color.parseColor("#3F51B5"))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT)
                })
            }
            wordRow.addView(box)
        }
    }

    private fun makeAudioBtn(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label; textSize = 28f; typeface = font
            setTextColor(Color.parseColor("#1565C0"))
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            setPadding(dp(32), dp(18), dp(32), dp(18))
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun makeActionBtn(label: String, bg: String, border: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label; textSize = 26f; typeface = font
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(bg))
            setPadding(dp(36), dp(16), dp(36), dp(16))
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
