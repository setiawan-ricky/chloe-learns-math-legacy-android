package com.example.chloelearns

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREF_NAME = "chloe_prefs"
        const val PREF_LANG = "app_language"
    }

    private lateinit var bounceZone: FrameLayout
    private lateinit var explosionBitmap: Bitmap
    private var charFiles: Array<String> = emptyArray()
    private val handler = Handler(Looper.getMainLooper())
    private val activePlayers = mutableListOf<MediaPlayer>()
    private lateinit var prefs: SharedPreferences
    private var lang: String = "en"

    private lateinit var flagUs: ImageView
    private lateinit var flagCn: ImageView

    private fun playAssetSound(path: String) {
        try {
            val afd: AssetFileDescriptor = assets.openFd(path)
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { p ->
                activePlayers.remove(p)
                p.release()
            }
            mp.prepare()
            mp.start()
            activePlayers.add(mp)
        } catch (_: Exception) {}
    }

    private fun langAudio(relativePath: String): String = "audio/$lang/$relativePath"

    private fun setLang(newLang: String) {
        lang = newLang
        prefs.edit().putString(PREF_LANG, lang).apply()
        updateFlagAlpha()
        updateLabels()
    }

    private fun updateFlagAlpha() {
        flagUs.alpha = if (lang == "en") 1f else 0.35f
        flagCn.alpha = if (lang == "zh") 1f else 0.35f
    }

    private fun updateGamesToday() {
        val raw = prefs.getString(MathGameActivity.HISTORY_KEY, "") ?: ""
        if (raw.isBlank()) {
            findViewById<TextView>(R.id.txtTodayCount).text = "0"
            return
        }
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("America/Los_Angeles")
        val todayStr = sdf.format(Date())
        val count = raw.split("\n").filter { it.isNotBlank() }.count { line ->
            // format: game|mode|correct|total|secs|date
            val p = line.split("|")
            if (p.size >= 6) {
                val datePart = p[5].replace(Regex("\\s+\\d{1,2}:\\d{2}$"), "")
                datePart == todayStr
            } else false
        }
        findViewById<TextView>(R.id.txtTodayCount).text = count.toString()
    }

    private fun updateLabels() {
        if (lang == "zh") {
            findViewById<TextView>(R.id.txtChloe).text = "紫怡"
            findViewById<TextView>(R.id.txtLearnsMath).text = "学数学"
            findViewById<TextView>(R.id.txtAddition).text = "加法"
            findViewById<TextView>(R.id.txtMinus).text = "减法"
            findViewById<TextView>(R.id.txtBtnAdditionEasy).text = "简单"
            findViewById<TextView>(R.id.txtBtnAdditionHard).text = "困难"
            findViewById<TextView>(R.id.txtBtnMinusEasy).text = "简单"
            findViewById<TextView>(R.id.txtBtnMinusHard).text = "困难"
            findViewById<TextView>(R.id.txtBtnHistory).text = "历史"
            findViewById<TextView>(R.id.txtBtnStats).text = "统计"
            findViewById<TextView>(R.id.txtTodayLabel).text = "今天玩的次数"
        } else {
            findViewById<TextView>(R.id.txtChloe).text = "chloe "
            findViewById<TextView>(R.id.txtLearnsMath).text = "learns math"
            findViewById<TextView>(R.id.txtAddition).text = "addition"
            findViewById<TextView>(R.id.txtMinus).text = "minus"
            findViewById<TextView>(R.id.txtBtnAdditionEasy).text = "easy"
            findViewById<TextView>(R.id.txtBtnAdditionHard).text = "hard"
            findViewById<TextView>(R.id.txtBtnMinusEasy).text = "easy"
            findViewById<TextView>(R.id.txtBtnMinusHard).text = "hard"
            findViewById<TextView>(R.id.txtBtnHistory).text = "history"
            findViewById<TextView>(R.id.txtBtnStats).text = "stats"
            findViewById<TextView>(R.id.txtTodayLabel).text = "times played today"
        }
    }

    inner class BounceState(val img: ImageView) {
        var x = 0f
        var y = 0f
        var vx = 5f
        var vy = 4f
        var fileIndex = -1
        var exploding = false

        fun randomVelocity() {
            val speed = (3.5f + Random.nextFloat() * 2.5f) * 1.5f * 0.75f * 0.5f
            val angle = Random.nextFloat() * (2.0 * Math.PI).toFloat()
            vx = speed * cos(angle.toDouble()).toFloat()
            vy = speed * sin(angle.toDouble()).toFloat()
            if (abs(vx) < 1f) vx = if (vx >= 0) 1f else -1f
            if (abs(vy) < 1f) vy = if (vy >= 0) 1f else -1f
        }

        fun loadCharacter(skipIndex: Int) {
            if (charFiles.isEmpty()) return
            var idx: Int
            do { idx = Random.nextInt(charFiles.size) } while (idx == skipIndex && charFiles.size > 1)
            fileIndex = idx
            val bmp = BitmapFactory.decodeStream(assets.open("images/characters/${charFiles[idx]}"))
            img.setImageBitmap(bmp)
        }
    }

    private val bouncers = mutableListOf<BounceState>()

    private val bounceRunnable = object : Runnable {
        override fun run() {
            if (bouncers.isEmpty() || bouncers[0].img.width == 0) { handler.postDelayed(this, 16); return }

            val sz = bouncers[0].img.width.toFloat()
            val maxX = (bounceZone.width  - sz).coerceAtLeast(0f)
            val maxY = (bounceZone.height - sz).coerceAtLeast(0f)

            for (b in bouncers) {
                if (b.exploding) continue
                b.x += b.vx
                b.y += b.vy
                if (b.x <= 0f)   { b.x = 0f;   b.vx =  abs(b.vx) }
                if (b.y <= 0f)   { b.y = 0f;   b.vy =  abs(b.vy) }
                if (b.x >= maxX) { b.x = maxX; b.vx = -abs(b.vx) }
                if (b.y >= maxY) { b.y = maxY; b.vy = -abs(b.vy) }
                b.img.x = b.x
                b.img.y = b.y
            }

            for (i in bouncers.indices) {
                for (j in i + 1 until bouncers.size) {
                    val a = bouncers[i]
                    val b = bouncers[j]
                    if (a.exploding || b.exploding) continue
                    val dx = (a.x + sz / 2f) - (b.x + sz / 2f)
                    val dy = (a.y + sz / 2f) - (b.y + sz / 2f)
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist >= sz || dist == 0f) continue
                    val nx = dx / dist
                    val ny = dy / dist
                    val dot = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny
                    if (dot >= 0f) continue
                    a.vx -= dot * nx; a.vy -= dot * ny
                    b.vx += dot * nx; b.vy += dot * ny
                    val overlap = sz - dist
                    a.x += nx * overlap / 2f; a.y += ny * overlap / 2f
                    b.x -= nx * overlap / 2f; b.y -= ny * overlap / 2f
                    a.img.x = a.x; a.img.y = a.y
                    b.img.x = b.x; b.img.y = b.y
                }
            }

            handler.postDelayed(this, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        lang = prefs.getString(PREF_LANG, "en") ?: "en"

        val font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")

        // Flags
        flagUs = findViewById(R.id.imgFlagUs)
        flagCn = findViewById(R.id.imgFlagCn)
        flagUs.setImageBitmap(BitmapFactory.decodeStream(assets.open("images/menu/flag-us.png")))
        flagCn.setImageBitmap(BitmapFactory.decodeStream(assets.open("images/menu/flag-cn.png")))
        flagUs.setOnClickListener { setLang("en") }
        flagCn.setOnClickListener { setLang("zh") }
        updateFlagAlpha()

        // Load blob button images
        val bmpGreen  = BitmapFactory.decodeStream(assets.open("images/menu/btn-green.png"))
        val bmpRed    = BitmapFactory.decodeStream(assets.open("images/menu/btn-red.png"))
        val bmpBlue   = BitmapFactory.decodeStream(assets.open("images/menu/btn-blue.png"))
        val bmpOrange = BitmapFactory.decodeStream(assets.open("images/menu/btn-orange.png"))

        findViewById<ImageView>(R.id.imgBtnAdditionEasy).setImageBitmap(bmpGreen)
        findViewById<ImageView>(R.id.imgBtnAdditionHard).setImageBitmap(bmpRed)
        findViewById<ImageView>(R.id.imgBtnMinusEasy).setImageBitmap(bmpGreen)
        findViewById<ImageView>(R.id.imgBtnMinusHard).setImageBitmap(bmpRed)
        findViewById<ImageView>(R.id.imgBtnHistory).setImageBitmap(bmpOrange)
        findViewById<ImageView>(R.id.imgBtnStats).setImageBitmap(bmpBlue)


        // Load title icons
        findViewById<ImageView>(R.id.imgHeart).setImageBitmap(
            BitmapFactory.decodeStream(assets.open("images/menu/heart.png")))
        findViewById<ImageView>(R.id.imgBlackboard).setImageBitmap(
            BitmapFactory.decodeStream(assets.open("images/menu/blackboard.png")))

        // Apply font to all text
        fun applyFont(id: Int) { findViewById<TextView>(id).typeface = font }
        applyFont(R.id.txtChloe)
        applyFont(R.id.txtLearnsMath)
        applyFont(R.id.txtAddition)
        applyFont(R.id.txtMinus)
        applyFont(R.id.txtBtnAdditionEasy)
        applyFont(R.id.txtBtnAdditionHard)
        applyFont(R.id.txtBtnMinusEasy)
        applyFont(R.id.txtBtnMinusHard)
        applyFont(R.id.txtBtnHistory)
        applyFont(R.id.txtBtnStats)
        applyFont(R.id.txtTodayCount)
        applyFont(R.id.txtTodayLabel)

        fun launchGame(game: String, mode: String) {
            startActivity(Intent(this, MathGameActivity::class.java)
                .putExtra(MathGameActivity.EXTRA_GAME, game)
                .putExtra(MathGameActivity.EXTRA_MODE, mode))
        }

        findViewById<FrameLayout>(R.id.btnAdditionEasy).setOnClickListener { playAssetSound(langAudio("menu/easy.mp3")); launchGame(MathGameActivity.GAME_ADDITION,    MathGameActivity.MODE_EASY) }
        findViewById<FrameLayout>(R.id.btnAdditionHard).setOnClickListener { playAssetSound(langAudio("menu/hard.mp3")); launchGame(MathGameActivity.GAME_ADDITION,    MathGameActivity.MODE_HARD) }
        findViewById<FrameLayout>(R.id.btnMinusEasy).setOnClickListener    { playAssetSound(langAudio("menu/easy.mp3")); launchGame(MathGameActivity.GAME_SUBTRACTION, MathGameActivity.MODE_EASY) }
        findViewById<FrameLayout>(R.id.btnMinusHard).setOnClickListener    { playAssetSound(langAudio("menu/hard.mp3")); launchGame(MathGameActivity.GAME_SUBTRACTION, MathGameActivity.MODE_HARD) }
        findViewById<FrameLayout>(R.id.btnHistory).setOnClickListener      { startActivity(Intent(this, HistoryActivity::class.java)) }
        findViewById<FrameLayout>(R.id.btnStats).setOnClickListener        { startActivity(Intent(this, StatsActivity::class.java)) }
        findViewById<LinearLayout>(R.id.titleRow).setOnClickListener       { playAssetSound(langAudio("chloe-learns-math.mp3")) }
        findViewById<TextView>(R.id.txtAddition).setOnClickListener        { playAssetSound(langAudio("menu/addition.mp3")) }
        findViewById<TextView>(R.id.txtMinus).setOnClickListener           { playAssetSound(langAudio("menu/minus.mp3")) }
        findViewById<LinearLayout>(R.id.todayCounter).setOnClickListener   { playAssetSound(langAudio("menu/games-today.mp3")) }

        updateLabels()
        updateGamesToday()

        charFiles = (assets.list("images/characters") ?: emptyArray())
            .filter { it.endsWith(".png") }.toTypedArray()
        explosionBitmap = BitmapFactory.decodeStream(assets.open("images/explosion.png"))

        bounceZone = findViewById(R.id.bounceZone)
        val bounceIds = listOf(R.id.imgBounce, R.id.imgBounce2, R.id.imgBounce3)
        for (id in bounceIds) {
            bouncers.add(BounceState(findViewById(id)))
        }
        for (b in bouncers) {
            b.img.setOnClickListener { onTapped(b) }
        }

        bounceZone.post {
            val w = bounceZone.width.toFloat()
            val h = bounceZone.height.toFloat()
            val sz = bouncers[0].img.width.toFloat()
            val maxX = (w - sz).coerceAtLeast(0f)
            val maxY = (h - sz).coerceAtLeast(0f)

            val positions = listOf(
                (w / 4f - sz / 2f).coerceIn(0f, maxX) to (h / 2f - sz / 2f).coerceAtLeast(0f),
                (3f * w / 4f - sz / 2f).coerceIn(0f, maxX) to (h / 2f - sz / 2f).coerceAtLeast(0f),
                (w / 2f - sz / 2f).coerceIn(0f, maxX) to (h / 4f - sz / 2f).coerceAtLeast(0f)
            )

            val usedIndices = mutableListOf<Int>()
            for ((i, b) in bouncers.withIndex()) {
                b.x = positions[i].first
                b.y = positions[i].second
                b.img.x = b.x; b.img.y = b.y
                b.loadCharacter(usedIndices.lastOrNull() ?: -1)
                usedIndices.add(b.fileIndex)
                b.randomVelocity()
            }

            handler.post(bounceRunnable)
        }
    }

    private fun onTapped(tapped: BounceState) {
        if (tapped.exploding) return
        tapped.exploding = true

        tapped.img.setImageBitmap(explosionBitmap)
        playAssetSound("audio/laser.mp3")
        tapped.img.alpha = 1f

        handler.postDelayed({
            tapped.img.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    handler.postDelayed({
                        val skipIndices = bouncers.filter { it !== tapped && !it.exploding }.map { it.fileIndex }
                        spawnCharacter(tapped, skipIndices)
                    }, 500)
                }
                .start()
        }, 1000)
    }

    private fun spawnCharacter(b: BounceState, skipIndices: List<Int>) {
        val maxX = (bounceZone.width  - b.img.width ).toFloat().coerceAtLeast(10f)
        val maxY = (bounceZone.height - b.img.height).toFloat().coerceAtLeast(10f)

        // Position first, then make visible
        b.x = Random.nextFloat() * maxX
        b.y = Random.nextFloat() * maxY
        b.img.x = b.x
        b.img.y = b.y

        b.loadCharacter(skipIndices.firstOrNull() ?: -1)
        b.img.alpha = 1f
        b.randomVelocity()
        b.exploding = false
    }

    override fun onResume() {
        super.onResume()
        if (bouncers.isNotEmpty() && bouncers[0].img.width > 0) handler.post(bounceRunnable)
        updateGamesToday()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(bounceRunnable)
        for (b in bouncers) b.img.animate().cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(bounceRunnable)
        activePlayers.forEach { it.release() }
        activePlayers.clear()
    }
}
