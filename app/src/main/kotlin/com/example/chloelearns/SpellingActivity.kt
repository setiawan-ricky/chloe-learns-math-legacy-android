package com.example.chloelearns

import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SpellingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_EASY = "EASY"
        const val MODE_HARD = "HARD"
        const val TIMER_EASY = 30
        const val TIMER_HARD = 20
        val INCORRECT_HARD_EN = listOf(
            "audio/en/incorrect/almost-not-correct.mp3",
            "audio/en/incorrect/not-quite.mp3",
            "audio/en/incorrect/doesnt-seem-right.mp3",
            "audio/en/incorrect/so-close.mp3",
            "audio/en/incorrect/not-that-one.mp3",
            "audio/en/incorrect/oh-no.mp3",
            "audio/en/incorrect/oopsie.mp3",
            "audio/en/incorrect/not-this-time.mp3",
            "audio/en/incorrect/uh-oh.mp3",
        )
        val INCORRECT_HARD_ZH = listOf(
            "audio/zh/incorrect/cha-yi-dian.mp3",
            "audio/zh/incorrect/hao-xiang-bu-dui.mp3",
            "audio/zh/incorrect/you-dian-bu-dui.mp3",
            "audio/zh/incorrect/hen-jie-jin.mp3",
            "audio/zh/incorrect/bu-shi-zhe-ge.mp3",
            "audio/zh/incorrect/ai-ya.mp3",
            "audio/zh/incorrect/bu-dui-o.mp3",
            "audio/zh/incorrect/zhe-ci-bu-dui.mp3",
            "audio/zh/incorrect/zao-gao.mp3",
        )
        val ALL_WORDS = listOf(
            "the","of","and","a","to","in","is","you","that","it",
            "he","for","was","on","are","as","with","his","they","at",
            "be","this","from","I","have","or","by","one","had","not",
            "but","what","all","were","when","we","there","can","an","your",
            "which","their","said","if","do","will","each","about","how","up",
            "out","them","then","she","many","some","so","these","would","other",
            "into","has","more","her","two","like","him","see","time","could",
            "no","make","than","first","been","its","who","now","people","my",
            "made","over","did","down","only","way","find","use","may","water",
            "long","little","very","after","words","called","just","where","most","know",
            "cat","dog","hat","bed","run","man","sit","sun","top","red","bat","cap","pen"
        )
        const val ROUND_SIZE = 5
        const val TIMER_SECS = 30
        const val TIMER_WARN = 8
    }

    private var spellingMode = MODE_EASY
    private var isHard = false
    private var timerSecs = TIMER_EASY
    private var words = listOf<String>()
    private var questionIdx = 0
    private var roundCorrect = 0
    private var filled = mutableListOf<Char>()
    private var available = mutableListOf<Char?>()
    private var advancing = false
    private var lang = "en"

    private lateinit var prefs: SharedPreferences
    private var startTime = 0L
    private lateinit var font: Typeface
    private lateinit var blanksRow: LinearLayout
    private lateinit var lettersRow: LinearLayout
    private lateinit var txtProgress: TextView
    private lateinit var txtTimer: TextView
    private lateinit var gameArea: View
    private lateinit var endArea: LinearLayout
    private lateinit var txtEndMessage: TextView
    private lateinit var imgCelebration: ImageView
    private lateinit var btnHearWord: TextView
    private lateinit var btnHearSentence: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var audioSeqId = 0
    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val word: String get() = words.getOrElse(questionIdx) { "" }

    private fun playAssetSound(path: String) {
        audioSeqId++
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

    private fun playRandomFromFolder(folder: String) {
        try {
            val files = assets.list(folder) ?: return
            if (files.isEmpty()) return
            val file = files[Random.nextInt(files.size)]
            playAssetSound("$folder/$file")
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spelling)

        spellingMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_EASY
        isHard = spellingMode == MODE_HARD
        timerSecs = if (isHard) TIMER_HARD else TIMER_EASY

        prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
        lang = prefs.getString(MainActivity.PREF_LANG, "en") ?: "en"
        font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")

        blanksRow = findViewById(R.id.blanksRow)
        lettersRow = findViewById(R.id.lettersRow)
        txtProgress = findViewById<TextView>(R.id.txtBack).also { /* reuse header for progress */ }
        txtTimer = TextView(this) // placeholder, we'll add to layout

        btnHearWord = findViewById(R.id.btnHearWord)
        btnHearSentence = findViewById(R.id.btnHearSentence)
        val btnPlayAgain = findViewById<TextView>(R.id.btnPlayAgain)
        val btnQuit = findViewById<TextView>(R.id.btnQuit)
        val txtFeedback = findViewById<TextView>(R.id.txtFeedback)
        endArea = findViewById(R.id.doneRow)
        txtEndMessage = txtFeedback
        imgCelebration = ImageView(this) // we'll handle inline

        val txtBack = findViewById<TextView>(R.id.txtBack)
        txtBack.typeface = font
        btnHearWord.typeface = font
        btnHearSentence.typeface = font
        btnPlayAgain.typeface = font
        btnQuit.typeface = font
        txtFeedback.typeface = font

        if (lang == "zh") {
            txtBack.text = "← 拼写 • ${if (isHard) "困难" else "简单"}"
            btnHearWord.text = "🔊 单词"
            btnHearSentence.text = "📖 句子"
            btnPlayAgain.text = "再来一次"
            btnQuit.text = "退出"
        } else {
            txtBack.text = "← spelling • ${if (isHard) "hard" else "easy"}"
            btnHearWord.text = "🔊 word"
            btnHearSentence.text = "📖 sentence"
        }

        txtBack.setOnClickListener { countDownTimer?.cancel(); finish() }
        btnHearWord.setOnClickListener { playAssetSound("audio/en/spelling/${word}-word.mp3") }
        btnHearSentence.setOnClickListener { playAssetSound("audio/en/spelling/${word}-sentence.mp3") }
        btnPlayAgain.setOnClickListener { startRound() }
        btnQuit.setOnClickListener { finish() }

        // Add timer + progress to content area
        val contentArea = findViewById<LinearLayout>(R.id.contentArea)
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dp(24), dp(8), dp(24), dp(8))
            gravity = Gravity.CENTER_VERTICAL
        }
        txtProgress = TextView(this).apply {
            textSize = 28f; typeface = font; setTextColor(Color.parseColor("#3F51B5"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        txtTimer = TextView(this).apply {
            textSize = 34f; typeface = font; setTextColor(Color.parseColor("#757575"))
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(txtProgress)
        topRow.addView(txtTimer)
        contentArea.addView(topRow, 0)

        startRound()
    }

    private fun startRound() {
        words = ALL_WORDS.shuffled().take(ROUND_SIZE)
        questionIdx = 0
        roundCorrect = 0
        startTime = System.currentTimeMillis()
        endArea.visibility = View.GONE
        txtEndMessage.visibility = View.GONE
        lettersRow.visibility = View.VISIBLE
        startWord()
    }

    private fun startWord() {
        advancing = false
        filled.clear()
        val letters = word.toList().toMutableList()
        do { letters.shuffle() } while (letters.joinToString("") == word && word.length > 1)
        available = letters.map { it as Char? }.toMutableList()
        txtProgress.text = "${questionIdx + 1} / $ROUND_SIZE"
        buildBlanks()
        buildLetters()
        handler.postDelayed({
            startTimer()
            playWordSequence(word)
        }, 2000)
    }

    private fun playWordSequence(w: String) {
        val myId = ++audioSeqId
        val wordPath = "audio/en/spelling/${w}-word.mp3"
        val sentencePath = "audio/en/spelling/${w}-sentence.mp3"
        playAssetSoundWithCompletion(myId, wordPath) {
            if (audioSeqId != myId) return@playAssetSoundWithCompletion
            playAssetSoundWithCompletion(myId, sentencePath) {
                if (audioSeqId != myId) return@playAssetSoundWithCompletion
                playAssetSound(wordPath)
            }
        }
    }

    private fun playAssetSoundWithCompletion(seqId: Int, path: String, onComplete: () -> Unit) {
        if (audioSeqId != seqId) { onComplete(); return }
        try {
            val old = mediaPlayer
            val afd: AssetFileDescriptor = assets.openFd(path)
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setVolume(1f, 1f)
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            old?.release()
            mp.setOnCompletionListener {
                it.release()
                if (audioSeqId == seqId) onComplete()
            }
        } catch (_: Exception) { if (audioSeqId == seqId) onComplete() }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        txtTimer.text = timerSecs.toString()
        txtTimer.setTextColor(Color.parseColor("#757575"))
        countDownTimer = object : CountDownTimer(timerSecs * 1000L, 1000) {
            override fun onTick(ms: Long) {
                val secs = ((ms + 999) / 1000).toInt()
                txtTimer.text = secs.toString()
                txtTimer.setTextColor(if (secs <= TIMER_WARN) Color.parseColor("#F44336") else Color.parseColor("#757575"))
            }
            override fun onFinish() {
                txtTimer.text = "0"
                advancing = true
                recordSpellingResult(word, false)
                handler.postDelayed({ advanceQuestion() }, 800)
            }
        }.start()
    }

    private fun advanceQuestion() {
        questionIdx++
        if (questionIdx >= ROUND_SIZE) endRound() else startWord()
    }

    private fun endRound() {
        countDownTimer?.cancel()
        lettersRow.visibility = View.GONE
        saveHistory()

        val langFolder = "audio/$lang"
        val msg: String
        if (roundCorrect == ROUND_SIZE) {
            playRandomFromFolder("$langFolder/all-correct")
            msg = if (lang == "zh") "太厉害了！\n满分！🎉" else "amazing!\nperfect score! 🎉"
        } else if (roundCorrect <= 1) {
            playRandomFromFolder("$langFolder/completion-bad")
            msg = if (lang == "zh") "继续加油！\n${ROUND_SIZE}题对了${roundCorrect}题" else "keep practising!\n$roundCorrect out of $ROUND_SIZE correct"
        } else {
            playRandomFromFolder("$langFolder/completion")
            msg = if (lang == "zh") "做得好紫怡！\n${ROUND_SIZE}题对了${roundCorrect}题" else "good job!\n$roundCorrect out of $ROUND_SIZE correct"
        }
        txtEndMessage.text = msg
        txtEndMessage.setTextColor(Color.parseColor("#3F51B5"))
        txtEndMessage.textSize = 28f
        txtEndMessage.visibility = View.VISIBLE
        endArea.visibility = View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tileSize(): Int {
        val screenW = resources.displayMetrics.widthPixels
        val sidePad = dp(48)
        return minOf(dp(120), (screenW - sidePad * 2) / word.length)
    }

    private fun buildBlanks() {
        blanksRow.removeAllViews()
        val tile = tileSize()
        val blankW = (tile * 0.65).toInt()
        val fs = (tile * 0.8f / resources.displayMetrics.density)
        val underW = (blankW * 0.85).toInt()
        val underH = maxOf(dp(4), (tile * 0.067).toInt())
        for (i in word.indices) {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(blankW, LinearLayout.LayoutParams.WRAP_CONTENT)
                if (i < filled.size) {
                    isClickable = true; isFocusable = true
                    val idx = i
                    setOnClickListener { onFilledTap(idx) }
                }
            }
            box.addView(TextView(this).apply {
                text = if (i < filled.size) filled[i].toString() else ""
                textSize = fs; typeface = font
                setTextColor(Color.parseColor("#3F51B5"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, tile)
            })
            if (i >= filled.size) {
                box.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(underW, underH)
                    setBackgroundColor(Color.parseColor("#9E9E9E"))
                })
            }
            blanksRow.addView(box)
        }
    }

    private fun buildLetters() {
        lettersRow.removeAllViews()
        val tile = tileSize()
        val fs = (tile * 0.53f / resources.displayMetrics.density)
        for (i in available.indices) {
            val letter = available[i]
            lettersRow.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(tile, tile).apply {
                    marginEnd = if (i < available.size - 1) dp(10) else 0
                }
                gravity = Gravity.CENTER; textSize = fs; typeface = font
                if (letter != null) {
                    text = letter.toString()
                    setTextColor(Color.parseColor("#F57F17"))
                    setBackgroundColor(Color.parseColor("#FFF9C4"))
                    isClickable = true; isFocusable = true
                    setOnClickListener { onLetterTap(letter, i) }
                } else {
                    text = ""; setBackgroundColor(Color.parseColor("#EEEEEE"))
                }
            })
        }
    }

    private fun onLetterTap(letter: Char, index: Int) {
        if (advancing) return
        filled.add(letter)
        available[index] = null
        buildBlanks()
        buildLetters()

        if (filled.size == word.length) {
            countDownTimer?.cancel()
            if (filled.joinToString("") == word) {
                recordSpellingResult(word, true)
                roundCorrect++
                playAssetSound("audio/en/spelling/correct-${Random.nextInt(1, 7)}.mp3")
                advancing = true
                handler.postDelayed({ advanceQuestion() }, 1000)
            } else if (isHard) {
                recordSpellingResult(word, false)
                recordSpellingMistake(word, filled.joinToString(""))
                val clips = if (lang == "zh") INCORRECT_HARD_ZH else INCORRECT_HARD_EN
                playAssetSound(clips[Random.nextInt(clips.size)])
                advancing = true
                handler.postDelayed({ advanceQuestion() }, 1500)
            } else {
                recordSpellingMistake(word, filled.joinToString(""))
                playAssetSound("audio/en/spelling/not-correct.mp3")
                advancing = true
                handler.postDelayed({
                    advancing = false
                    filled.clear()
                    val letters = word.toList().toMutableList()
                    do { letters.shuffle() } while (letters.joinToString("") == word && word.length > 1)
                    available = letters.map { it as Char? }.toMutableList()
                    buildBlanks()
                    buildLetters()
                    startTimer()
                }, 1500)
            }
        }
    }

    private fun onFilledTap(index: Int) {
        if (advancing) return
        val removed = filled.subList(index, filled.size).toList()
        filled = filled.subList(0, index).toMutableList()
        for (l in removed) {
            val emptyIdx = available.indexOf(null)
            if (emptyIdx != -1) available[emptyIdx] = l
        }
        buildBlanks()
        buildLetters()
    }

    private fun recordSpellingResult(word: String, correct: Boolean) {
        val key = "Spelling:$word"
        val raw = prefs.getString(MathGameActivity.QUESTION_STATS_KEY, "") ?: ""
        data class Entry(var score: Int, var attempts: Int)
        val stats = mutableMapOf<String, Entry>()
        if (raw.isNotBlank()) {
            for (line in raw.split("\n").filter { it.isNotBlank() }) {
                val p = line.split("|")
                if (p.size >= 3) {
                    stats[p[0]] = Entry(p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0)
                }
            }
        }
        val entry = stats.getOrPut(key) { Entry(0, 0) }
        entry.score += if (correct) 1 else -1
        entry.attempts += 1
        val serialized = stats.entries.joinToString("\n") { "${it.key}|${it.value.score}|${it.value.attempts}|Spelling|0|0" }
        prefs.edit().putString(MathGameActivity.QUESTION_STATS_KEY, serialized).apply()
    }

    private fun recordSpellingMistake(word: String, attempt: String) {
        val key = "Spelling:$word"
        val timestamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        val entry = "$key|$attempt|$word|Spelling|0|0|$timestamp"
        val raw = prefs.getString(MathGameActivity.MISTAKE_LOG_KEY, "") ?: ""
        val lines = if (raw.isBlank()) mutableListOf() else raw.split("\n").toMutableList()
        lines.add(0, entry)
        if (lines.size > MathGameActivity.MAX_MISTAKES) lines.subList(MathGameActivity.MAX_MISTAKES, lines.size).clear()
        prefs.edit().putString(MathGameActivity.MISTAKE_LOG_KEY, lines.joinToString("\n")).apply()
    }

    private fun saveHistory() {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val timestamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        val entry = "Spelling|$spellingMode|$roundCorrect|$ROUND_SIZE|$elapsed|$timestamp"
        val raw = prefs.getString(MathGameActivity.HISTORY_KEY, "") ?: ""
        val lines = if (raw.isBlank()) mutableListOf() else raw.split("\n").toMutableList()
        lines.add(0, entry)
        if (lines.size > MathGameActivity.MAX_HISTORY) lines.subList(MathGameActivity.MAX_HISTORY, lines.size).clear()
        prefs.edit().putString(MathGameActivity.HISTORY_KEY, lines.joinToString("\n")).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }
}
