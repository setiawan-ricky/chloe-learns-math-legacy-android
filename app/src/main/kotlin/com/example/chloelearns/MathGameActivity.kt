package com.example.chloelearns

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
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
        const val MAX_HISTORY = 50
    }

    private var mode = MODE_EASY
    private var game = GAME_ADDITION
    private var timerSeconds = 30

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

        tvOperator.text = if (game == GAME_SUBTRACTION) " \u2212 " else " + "
        tvMode.text = "$game \u2022 $mode"
        tvMode.setTextColor(if (mode == MODE_EASY) Color.parseColor("#43A047") else Color.parseColor("#E53935"))

        findViewById<Button>(R.id.btnPlayAgain).setOnClickListener { startRound() }
        findViewById<Button>(R.id.btnQuit).setOnClickListener { finish() }

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
        for (digit in 0..9) {
            val id = resources.getIdentifier("btn$digit", "id", packageName)
            findViewById<Button>(id).setOnClickListener { appendDigit(digit.toString()) }
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { onBackspace() }
        findViewById<Button>(R.id.btnEnter).setOnClickListener { onEnter() }
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
            roundCorrect++
            totalScore++
            prefs.edit().putInt("math_score", totalScore).apply()
            updateScore()
            countDownTimer?.cancel()
            playRandom("audio/correct")
            showCorrectSplash()
        } else {
            playRandom("audio/incorrect")
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
                playRandom("audio/all-correct")
                showRandomCelebration()
                tvEndMessage.text = "Amazing!\nPerfect score! \uD83C\uDF89"
            }
            roundCorrect <= 1 -> {
                playRandom("audio/completion-bad")
                imgCelebration.visibility = View.GONE
                tvEndMessage.text = "Keep practising!\n$roundCorrect out of $roundSize correct"
            }
            else -> {
                playRandom("audio/completion")
                imgCelebration.visibility = View.GONE
                tvEndMessage.text = "Good job!\n$roundCorrect out of $roundSize correct"
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
                playRandom("audio/timeout")
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

    private fun updateScore() {
        tvScore.text = "Score: $totalScore"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }
}
