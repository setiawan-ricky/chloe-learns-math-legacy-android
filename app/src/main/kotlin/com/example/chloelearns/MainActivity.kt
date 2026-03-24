package com.example.chloelearns

import android.content.Intent
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var bounceZone: FrameLayout
    private lateinit var explosionBitmap: Bitmap
    private var charFiles: Array<String> = emptyArray()
    private val handler = Handler(Looper.getMainLooper())
    private val activePlayers = mutableListOf<MediaPlayer>()

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

    inner class BounceState(val img: ImageView) {
        var x = 0f
        var y = 0f
        var vx = 5f
        var vy = 4f
        var fileIndex = -1
        var exploding = false

        fun randomVelocity() {
            val speed = (3.5f + Random.nextFloat() * 2.5f) * 1.5f * 0.75f
            val angle = Random.nextFloat() * (2.0 * Math.PI).toFloat()
            vx = speed * cos(angle.toDouble()).toFloat()
            vy = speed * sin(angle.toDouble()).toFloat()
            if (abs(vx) < 2f) vx = if (vx >= 0) 2f else -2f
            if (abs(vy) < 2f) vy = if (vy >= 0) 2f else -2f
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

    private lateinit var b1: BounceState
    private lateinit var b2: BounceState

    private val bounceRunnable = object : Runnable {
        override fun run() {
            val img1 = b1.img
            if (img1.width == 0) { handler.postDelayed(this, 16); return }

            val maxX = (bounceZone.width  - img1.width ).toFloat().coerceAtLeast(0f)
            val maxY = (bounceZone.height - img1.height).toFloat().coerceAtLeast(0f)
            val sz   = img1.width.toFloat()   // both images same size

            for (b in listOf(b1, b2)) {
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

            // Elastic collision between the two images (equal mass, axis-aligned swap)
            if (!b1.exploding && !b2.exploding) {
                val dx = (b1.x + sz / 2f) - (b2.x + sz / 2f)
                val dy = (b1.y + sz / 2f) - (b2.y + sz / 2f)
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < sz && dist > 0f) {
                    // Normalize collision axis
                    val nx = dx / dist
                    val ny = dy / dist
                    // Relative velocity along axis
                    val dvx = b1.vx - b2.vx
                    val dvy = b1.vy - b2.vy
                    val dot = dvx * nx + dvy * ny
                    if (dot < 0f) {   // only resolve if approaching
                        b1.vx -= dot * nx
                        b1.vy -= dot * ny
                        b2.vx += dot * nx
                        b2.vy += dot * ny
                        // Separate to avoid sticking
                        val overlap = sz - dist
                        b1.x += nx * overlap / 2f
                        b1.y += ny * overlap / 2f
                        b2.x -= nx * overlap / 2f
                        b2.y -= ny * overlap / 2f
                        b1.img.x = b1.x
                        b1.img.y = b1.y
                        b2.img.x = b2.x
                        b2.img.y = b2.y
                    }
                }
            }

            handler.postDelayed(this, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val font = Typeface.createFromAsset(assets, "fonts/BubblegumSans-Regular.ttf")

        // Load blob button images
        val bmpGreen = BitmapFactory.decodeStream(assets.open("images/menu/btn-green.png"))
        val bmpRed   = BitmapFactory.decodeStream(assets.open("images/menu/btn-red.png"))
        val bmpBlue  = BitmapFactory.decodeStream(assets.open("images/menu/btn-blue.png"))

        findViewById<ImageView>(R.id.imgBtnAdditionEasy).setImageBitmap(bmpGreen)
        findViewById<ImageView>(R.id.imgBtnAdditionHard).setImageBitmap(bmpRed)
        findViewById<ImageView>(R.id.imgBtnMinusEasy).setImageBitmap(bmpGreen)
        findViewById<ImageView>(R.id.imgBtnMinusHard).setImageBitmap(bmpRed)
        findViewById<ImageView>(R.id.imgBtnHistory).setImageBitmap(bmpBlue)
        findViewById<ImageView>(R.id.imgBtnStats).setImageBitmap(bmpBlue)

        // Load title icons
        findViewById<ImageView>(R.id.imgHeart).setImageBitmap(
            BitmapFactory.decodeStream(assets.open("images/menu/heart.png")))
        findViewById<ImageView>(R.id.imgUnicorn).setImageBitmap(
            BitmapFactory.decodeStream(assets.open("images/menu/unicorn.png")))

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

        fun launchGame(game: String, mode: String) {
            startActivity(Intent(this, MathGameActivity::class.java)
                .putExtra(MathGameActivity.EXTRA_GAME, game)
                .putExtra(MathGameActivity.EXTRA_MODE, mode))
        }

        findViewById<FrameLayout>(R.id.btnAdditionEasy).setOnClickListener { playAssetSound("audio/menu/easy.mp3"); launchGame(MathGameActivity.GAME_ADDITION,    MathGameActivity.MODE_EASY) }
        findViewById<FrameLayout>(R.id.btnAdditionHard).setOnClickListener { playAssetSound("audio/menu/hard.mp3"); launchGame(MathGameActivity.GAME_ADDITION,    MathGameActivity.MODE_HARD) }
        findViewById<FrameLayout>(R.id.btnMinusEasy).setOnClickListener    { playAssetSound("audio/menu/easy.mp3"); launchGame(MathGameActivity.GAME_SUBTRACTION, MathGameActivity.MODE_EASY) }
        findViewById<FrameLayout>(R.id.btnMinusHard).setOnClickListener    { playAssetSound("audio/menu/hard.mp3"); launchGame(MathGameActivity.GAME_SUBTRACTION, MathGameActivity.MODE_HARD) }
        findViewById<FrameLayout>(R.id.btnHistory).setOnClickListener      { startActivity(Intent(this, HistoryActivity::class.java)) }
        findViewById<FrameLayout>(R.id.btnStats).setOnClickListener        { startActivity(Intent(this, StatsActivity::class.java)) }
        findViewById<LinearLayout>(R.id.titleRow).setOnClickListener       { playAssetSound("audio/chloe-learns-math.mp3") }
        findViewById<TextView>(R.id.txtAddition).setOnClickListener        { playAssetSound("audio/menu/addition.mp3") }
        findViewById<TextView>(R.id.txtMinus).setOnClickListener           { playAssetSound("audio/menu/minus.mp3") }

        charFiles = (assets.list("images/characters") ?: emptyArray())
            .filter { it.endsWith(".png") }.toTypedArray()
        explosionBitmap = BitmapFactory.decodeStream(assets.open("images/explosion.png"))

        b1 = BounceState(findViewById(R.id.imgBounce))
        b2 = BounceState(findViewById(R.id.imgBounce2))
        bounceZone = findViewById(R.id.bounceZone)

        b1.img.setOnClickListener { onTapped(b1, b2) }
        b2.img.setOnClickListener { onTapped(b2, b1) }

        bounceZone.post {
            val w = bounceZone.width.toFloat()
            val h = bounceZone.height.toFloat()
            val sz = b1.img.width.toFloat()

            b1.x = (w / 4f - sz / 2f).coerceIn(0f, (w - sz).coerceAtLeast(0f))
            b1.y = (h / 2f - sz / 2f).coerceAtLeast(0f)
            b2.x = (3f * w / 4f - sz / 2f).coerceIn(0f, (w - sz).coerceAtLeast(0f))
            b2.y = (h / 2f - sz / 2f).coerceAtLeast(0f)

            b1.img.x = b1.x; b1.img.y = b1.y
            b2.img.x = b2.x; b2.img.y = b2.y

            b1.loadCharacter(-1)
            b2.loadCharacter(b1.fileIndex)   // ensure different character

            b1.randomVelocity()
            b2.randomVelocity()

            handler.post(bounceRunnable)
        }
    }

    private fun onTapped(tapped: BounceState, other: BounceState) {
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
                        spawnCharacter(tapped, other.fileIndex)
                    }, 500)
                }
                .start()
        }, 1000)
    }

    private fun spawnCharacter(b: BounceState, skipIndex: Int) {
        val maxX = (bounceZone.width  - b.img.width ).toFloat().coerceAtLeast(10f)
        val maxY = (bounceZone.height - b.img.height).toFloat().coerceAtLeast(10f)

        // Position first, then make visible
        b.x = Random.nextFloat() * maxX
        b.y = Random.nextFloat() * maxY
        b.img.x = b.x
        b.img.y = b.y

        b.loadCharacter(skipIndex)
        b.img.alpha = 1f
        b.randomVelocity()
        b.exploding = false
    }

    override fun onResume() {
        super.onResume()
        if (b1.img.width > 0) handler.post(bounceRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(bounceRunnable)
        b1.img.animate().cancel()
        b2.img.animate().cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(bounceRunnable)
        activePlayers.forEach { it.release() }
        activePlayers.clear()
    }
}
