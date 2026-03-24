package com.example.chloelearns

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MathGameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_GAME = "game"
        const val MODE_EASY = "EASY"
        const val MODE_HARD = "HARD"
        const val GAME_ADDITION    = "Addition"
        const val GAME_SUBTRACTION = "Minus"
        const val HISTORY_KEY = "game_history"
        const val QUESTION_STATS_KEY = "question_stats"
        const val MISTAKE_LOG_KEY = "mistake_log"
        const val MAX_HISTORY = 50
        const val MAX_MISTAKES = 500
    }

    private var mode = MODE_EASY
    private var game = GAME_ADDITION
    private var timerSeconds = 30
    private var lang = "en"

    private var num1 = 0
    private var num2 = 0
    private var input = ""
    private var totalScore = 0
    private var roundCorrect = 0
    private var questionIndex = 0
    private val roundSize = 5
    private var roundInProgress = false
    private var roundStartTime = 0L

    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var prefs: SharedPreferences
    private lateinit var tvNum1: TextView
    private lateinit var tvNum2: TextView
    private lateinit var tvOperator: TextView
    private lateinit var tvAnswer: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvMode: TextView
    private lateinit var splashCorrect: View
    private lateinit var endScreen: View
    private lateinit var tvEndMessage: TextView
    private lateinit var imgCelebration: ImageView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_game)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_EASY
        game = intent.getStringExtra(EXTRA_GAME) ?: GAME_ADDITION
        timerSeconds = if (mode == MODE_EASY) 30 else 20

        prefs = getSharedPreferences("chloe_prefs", MODE_PRIVATE)
        lang = prefs.getString(MainActivity.PREF_LANG, "en") ?: "en"
        totalScore = prefs.getInt("math_score", 0)

        tvNum1        = findViewById(R.id.tvNum1)
        tvNum2        = findViewById(R.id.tvNum2)
        tvOperator    = findViewById(R.id.tvOperator)
        tvAnswer      = findViewById(R.id.tvAnswer)
        tvScore       = findViewById(R.id.tvScore)
        tvTimer       = findViewById(R.id.tvTimer)
        tvProgress    = findViewById(R.id.tvProgress)
        tvMode        = findViewById(R.id.tvMode)
        splashCorrect = findViewById(R.id.splashCorrect)
        endScreen     = findViewById(R.id.endScreen)
        tvEndMessage  = findViewById(R.id.tvEndMessage)
        imgCelebration = findViewById(R.id.imgCelebration)

        val font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")
        tvNum1.typeface = font
        tvNum2.typeface = font
        tvOperator.typeface = font
        tvAnswer.typeface = font
        tvScore.typeface = font
        tvTimer.typeface = font
        tvProgress.typeface = font
        tvMode.typeface = font
        tvEndMessage.typeface = font
        findViewById<Button>(R.id.btnPlayAgain).typeface = font
        findViewById<Button>(R.id.btnQuit).typeface = font

        tvOperator.text = if (game == GAME_SUBTRACTION) " \u2212 " else " + "
        val gameName = if (lang == "zh") (if (game == GAME_SUBTRACTION) "减法" else "加法") else game.lowercase()
        val modeName = if (lang == "zh") (if (mode == MODE_EASY) "简单" else "困难") else mode.lowercase()
        tvMode.text = "$gameName \u2022 $modeName"
        tvMode.setTextColor(if (mode == MODE_EASY) Color.parseColor("#43A047") else Color.parseColor("#E53935"))

        val btnPlay = findViewById<Button>(R.id.btnPlayAgain)
        val btnQuit = findViewById<Button>(R.id.btnQuit)
        btnPlay.setOnClickListener { startRound() }
        btnQuit.setOnClickListener { finish() }
        if (lang == "zh") {
            btnPlay.text = "再来一次"
            btnQuit.text = "退出"
            findViewById<Button>(R.id.btnEnter).text = "确定"
        }

        setupKeypad()
        startRound()
    }

    private fun startRound() {
        roundCorrect = 0
        questionIndex = 0
        roundInProgress = true
        roundStartTime = System.currentTimeMillis()
        endScreen.visibility = View.GONE
        updateScore()
        nextQuestion()
    }

    private fun setupKeypad() {
        val font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")
        for (digit in 0..9) {
            val id = resources.getIdentifier("btn$digit", "id", packageName)
            val btn = findViewById<Button>(id)
            btn.typeface = font
            btn.setOnClickListener { appendDigit(digit.toString()) }
        }
        val btnBack = findViewById<Button>(R.id.btnBackspace)
        btnBack.typeface = font
        btnBack.setOnClickListener { onBackspace() }
        val btnEnter = findViewById<Button>(R.id.btnEnter)
        btnEnter.typeface = font
        btnEnter.setOnClickListener { onEnter() }
    }

    private fun appendDigit(d: String) {
        if (!roundInProgress || input.length >= 2) return
        input += d
        tvAnswer.text = input
        tvAnswer.setTextColor(Color.parseColor("#212121"))
    }

    private fun onBackspace() {
        if (!roundInProgress || input.isEmpty()) return
        input = input.dropLast(1)
        tvAnswer.text = if (input.isEmpty()) "?" else input
        tvAnswer.setTextColor(Color.parseColor("#212121"))
    }

    private fun onEnter() {
        if (!roundInProgress || input.isEmpty()) return
        val answer = input.toIntOrNull() ?: return
        val correct = if (game == GAME_SUBTRACTION) num1 - num2 else num1 + num2

        if (answer == correct) {
            recordQuestion(true)
            roundCorrect++
            totalScore++
            prefs.edit().putInt("math_score", totalScore).apply()
            updateScore()
            countDownTimer?.cancel()
            playRandom("audio/$lang/correct")
            showCorrectSplash()
        } else {
            recordQuestion(false)
            recordMistake(answer)
            playRandom("audio/$lang/incorrect")
            tvAnswer.setTextColor(Color.parseColor("#F44336"))
            if (mode == MODE_HARD) {
                handler.postDelayed({ advanceQuestion() }, 800)
            } else {
                handler.postDelayed({
                    input = ""
                    tvAnswer.text = "?"
                    tvAnswer.setTextColor(Color.parseColor("#212121"))
                }, 600)
            }
        }
    }

    private fun showCorrectSplash() {
        splashCorrect.visibility = View.VISIBLE
        handler.postDelayed({
            splashCorrect.visibility = View.GONE
            advanceQuestion()
        }, 1000)
    }

    private fun advanceQuestion() {
        questionIndex++
        if (questionIndex >= roundSize) endRound() else nextQuestion()
    }

    private fun endRound() {
        roundInProgress = false
        countDownTimer?.cancel()
        saveHistory(roundCorrect, roundSize, System.currentTimeMillis() - roundStartTime)

        when {
            roundCorrect == roundSize -> {
                playRandom("audio/$lang/all-correct")
                showRandomCelebration()
                tvEndMessage.text = if (lang == "zh") "太厉害了！\n满分！\uD83C\uDF89" else "amazing!\nperfect score! \uD83C\uDF89"
            }
            roundCorrect <= 1 -> {
                playRandom("audio/$lang/completion-bad")
                imgCelebration.visibility = View.GONE
                tvEndMessage.text = if (lang == "zh") "继续加油！\n${roundSize}题对了${roundCorrect}题" else "keep practising!\n$roundCorrect out of $roundSize correct"
            }
            else -> {
                playRandom("audio/$lang/completion")
                imgCelebration.visibility = View.GONE
                tvEndMessage.text = if (lang == "zh") "做得好紫怡！\n${roundSize}题对了${roundCorrect}题" else "good job!\n$roundCorrect out of $roundSize correct"
            }
        }
        endScreen.visibility = View.VISIBLE
    }

    private fun nextQuestion() {
        if (game == GAME_SUBTRACTION) {
            // num1 >= num2, answer is 0–10
            num1 = Random.nextInt(1, 11)
            num2 = Random.nextInt(0, num1 + 1)
        } else {
            num1 = Random.nextInt(1, 11)
            num2 = Random.nextInt(1, 11)
        }
        input = ""
        tvNum1.text = num1.toString()
        tvNum2.text = num2.toString()
        tvAnswer.text = "?"
        tvAnswer.setTextColor(Color.parseColor("#212121"))
        tvProgress.text = "${questionIndex + 1} / $roundSize"
        startTimer()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        tvTimer.text = timerSeconds.toString()
        tvTimer.setTextColor(Color.parseColor("#757575"))
        countDownTimer = object : CountDownTimer(timerSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ((millisUntilFinished + 999) / 1000).toInt()
                tvTimer.text = seconds.toString()
                val warn = if (mode == MODE_EASY) 8 else 5
                tvTimer.setTextColor(
                    if (seconds <= warn) Color.parseColor("#F44336")
                    else Color.parseColor("#757575")
                )
            }
            override fun onFinish() {
                tvTimer.text = "0"
                recordQuestion(false)
                recordMistake(null)
                playRandom("audio/$lang/timeout")
                tvAnswer.setTextColor(Color.parseColor("#F44336"))
                handler.postDelayed({ advanceQuestion() }, 800)
            }
        }.start()
    }

    private fun showRandomCelebration() {
        try {
            val files = assets.list("images/celebration") ?: emptyArray()
            if (files.isEmpty()) { imgCelebration.visibility = View.GONE; return }
            val file = files[Random.nextInt(files.size)]
            val bmp = BitmapFactory.decodeStream(assets.open("images/celebration/$file"))
            imgCelebration.setImageBitmap(bmp)
            imgCelebration.visibility = View.VISIBLE
        } catch (_: Exception) {
            imgCelebration.visibility = View.GONE
        }
    }

    private fun playRandom(assetFolder: String) {
        try {
            val files = assets.list(assetFolder) ?: return
            if (files.isEmpty()) return
            val file = files[Random.nextInt(files.size)]
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val afd = assets.openFd("$assetFolder/$file")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
                setOnCompletionListener { it.release() }
            }
        } catch (_: Exception) {}
    }

    private fun saveHistory(correct: Int, total: Int, elapsedMs: Long) {
        val timestamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        val entry = "$game|$mode|$correct|$total|${elapsedMs / 1000}|$timestamp"
        val existing = prefs.getString(HISTORY_KEY, "") ?: ""
        val lines = if (existing.isBlank()) mutableListOf() else existing.split("\n").toMutableList()
        lines.add(0, entry)
        if (lines.size > MAX_HISTORY) lines.subList(MAX_HISTORY, lines.size).clear()
        prefs.edit().putString(HISTORY_KEY, lines.joinToString("\n")).apply()
    }

    private var pausedTimeLeft = -1L

    override fun onPause() {
        super.onPause()
        if (roundInProgress) {
            countDownTimer?.cancel()
            countDownTimer = null
            val displayed = tvTimer.text.toString().toIntOrNull() ?: 0
            pausedTimeLeft = displayed.toLong()
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        if (roundInProgress && pausedTimeLeft > 0) {
            resumeTimer(pausedTimeLeft)
            pausedTimeLeft = -1
        }
    }

    private fun resumeTimer(secondsLeft: Long) {
        tvTimer.text = secondsLeft.toString()
        val warn = if (mode == MODE_EASY) 8 else 5
        tvTimer.setTextColor(
            if (secondsLeft <= warn) Color.parseColor("#F44336")
            else Color.parseColor("#757575")
        )
        countDownTimer = object : CountDownTimer(secondsLeft * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ((millisUntilFinished + 999) / 1000).toInt()
                tvTimer.text = seconds.toString()
                tvTimer.setTextColor(
                    if (seconds <= warn) Color.parseColor("#F44336")
                    else Color.parseColor("#757575")
                )
            }
            override fun onFinish() {
                tvTimer.text = "0"
                recordQuestion(false)
                recordMistake(null)
                playRandom("audio/$lang/timeout")
                tvAnswer.setTextColor(Color.parseColor("#F44336"))
                handler.postDelayed({ advanceQuestion() }, 800)
            }
        }.start()
    }

    private fun recordQuestion(correct: Boolean) {
        val op = if (game == GAME_SUBTRACTION) "-" else "+"
        val key = "$game:$num1$op$num2"
        val raw = prefs.getString(QUESTION_STATS_KEY, "") ?: ""
        // format per line: key|score|attempts|game|num1|num2
        data class Entry(var score: Int, var attempts: Int, val game: String, val num1: Int, val num2: Int)
        val stats = mutableMapOf<String, Entry>()
        if (raw.isNotBlank()) {
            for (line in raw.split("\n").filter { it.isNotBlank() }) {
                val p = line.split("|")
                if (p.size >= 6) {
                    stats[p[0]] = Entry(
                        p[1].toIntOrNull() ?: 0,
                        p[2].toIntOrNull() ?: 0,
                        p[3],
                        p[4].toIntOrNull() ?: 0,
                        p[5].toIntOrNull() ?: 0
                    )
                }
            }
        }
        val entry = stats.getOrPut(key) { Entry(0, 0, game, num1, num2) }
        entry.score += if (correct) 1 else -1
        entry.attempts += 1
        val serialized = stats.entries.joinToString("\n") {
            "${it.key}|${it.value.score}|${it.value.attempts}|${it.value.game}|${it.value.num1}|${it.value.num2}"
        }
        prefs.edit().putString(QUESTION_STATS_KEY, serialized).apply()
    }

    private fun recordMistake(givenAnswer: Int?) {
        val op = if (game == GAME_SUBTRACTION) "-" else "+"
        val key = "$game:$num1$op$num2"
        val correctAns = if (game == GAME_SUBTRACTION) num1 - num2 else num1 + num2
        val timestamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        // format: key|givenAnswer|correctAnswer|game|num1|num2|date
        val entry = "$key|${givenAnswer ?: "timeout"}|$correctAns|$game|$num1|$num2|$timestamp"
        val raw = prefs.getString(MISTAKE_LOG_KEY, "") ?: ""
        val lines = if (raw.isBlank()) mutableListOf() else raw.split("\n").toMutableList()
        lines.add(0, entry)
        if (lines.size > MAX_MISTAKES) lines.subList(MAX_MISTAKES, lines.size).clear()
        prefs.edit().putString(MISTAKE_LOG_KEY, lines.joinToString("\n")).apply()
    }

    private fun updateScore() {
        tvScore.text = if (lang == "zh") "分数: $totalScore" else "score: $totalScore"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }
}
