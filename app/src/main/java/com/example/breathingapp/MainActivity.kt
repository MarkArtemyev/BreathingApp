package com.example.breathingapp

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

data class BreathingPattern(
    val name: String,
    val durations: LongArray,
    val instructions: Array<String>,
    val colors: IntArray,
    val animationType: IntArray
)

class MainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var instructionTextView: TextView
    private lateinit var cyclesTextView: TextView
    private lateinit var startButton: Button
    private lateinit var finishButton: Button
    private lateinit var vibrationButton: Button
    private lateinit var musicButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var breathingCircle: ImageView

    private var running = false
    private var cyclesCompleted = 0
    private var currentTimer: CountDownTimer? = null
    private var currentPhase = 0
    private var currentAnimator: ValueAnimator? = null
    private var currentPattern: BreathingPattern? = null
    private var isVibrationOn = false
    private var isMusicOn = false
    private var mediaPlayer: MediaPlayer? = null

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val patternRelax = BreathingPattern(
        "Релакс (4-7-8)",
        longArrayOf(4000L, 7000L, 8000L),
        arrayOf("ВДЫХАЙ", "ЗАДЕРЖИ", "ВЫДЫХАЙ"),
        intArrayOf(R.color.breatheIn, R.color.hold, R.color.breatheOut),
        intArrayOf(0, 2, 1)
    )

    private val patternBox = BreathingPattern(
        "Квадрат (Focus)",
        longArrayOf(4000L, 4000L, 4000L, 4000L),
        arrayOf("ВДЫХАЙ", "ЗАДЕРЖИ", "ВЫДЫХАЙ", "ЗАДЕРЖИ"),
        intArrayOf(R.color.breatheIn, R.color.hold, R.color.breatheOut, R.color.hold),
        intArrayOf(0, 2, 1, 2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerTextView = findViewById(R.id.timerTextView)
        instructionTextView = findViewById(R.id.instructionTextView)
        cyclesTextView = findViewById(R.id.cyclesTextView)
        startButton = findViewById(R.id.startButton)
        finishButton = findViewById(R.id.finishButton)
        vibrationButton = findViewById(R.id.vibrationButton)
        musicButton = findViewById(R.id.musicButton)
        menuButton = findViewById(R.id.menuButton)
        breathingCircle = findViewById(R.id.breathingCircle)

        currentPattern = patternRelax

        startButton.setOnClickListener { toggleTimer() }
        finishButton.setOnClickListener { resetCycle() }
        vibrationButton.setOnClickListener { toggleVibration() }
        musicButton.setOnClickListener { toggleMusic() }
        menuButton.setOnClickListener { showMenu() }

        updateUIForPattern()
    }

    private fun showMenu() {
        val popup = PopupMenu(this, menuButton)
        popup.menu.add(0, 1, 0, "Релакс (4-7-8)")
        popup.menu.add(0, 2, 1, "Квадрат (Focus)")
        popup.menu.add(0, 3, 2, "⚙️ Свой режим")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> setPattern(patternRelax)
                2 -> setPattern(patternBox)
                3 -> showCustomDialog()
            }
            true
        }
        popup.show()
    }

    private fun setPattern(pattern: BreathingPattern) {
        resetCycle()
        currentPattern = pattern
        updateUIForPattern()
    }

    private fun updateUIForPattern() {
        val pattern = currentPattern ?: return
        instructionTextView.text = pattern.name
        timerTextView.text = (pattern.durations[0] / 1000).toString()
        instructionTextView.setTextColor(ContextCompat.getColor(this, R.color.textDefault))
        timerTextView.setTextColor(ContextCompat.getColor(this, R.color.textDefault))
    }

    private fun toggleTimer() {
        running = !running
        startButton.text = if (running) "ПАУЗА" else "НАЧАТЬ"

        if (running) {
            startPhase(currentPhase)
        } else {
            currentTimer?.cancel()
            currentAnimator?.pause()
        }
    }

    private fun startPhase(phase: Int) {
        val pattern = currentPattern ?: return
        currentPhase = phase
        val duration = pattern.durations[phase]
        val instruction = pattern.instructions[phase]
        val colorRes = pattern.colors[phase]
        val color = ContextCompat.getColor(this, colorRes)
        val animType = pattern.animationType[phase]

        instructionTextView.text = instruction
        instructionTextView.setTextColor(color)
        timerTextView.setTextColor(color)

        breathingCircle.setColorFilter(color)

        animateCircle(duration, animType)

        if (isVibrationOn) vibrateShort()

        currentTimer?.cancel()
        currentTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = ((millisUntilFinished / 1000) + 1).toString()
            }

            override fun onFinish() {
                val nextPhase = (currentPhase + 1) % pattern.durations.size
                if (nextPhase == 0 && running) {
                    cyclesCompleted++
                    cyclesTextView.text = "Циклов: $cyclesCompleted"
                }
                if (running) startPhase(nextPhase)
            }
        }.start()
    }

    private fun animateCircle(duration: Long, animType: Int) {
        if (currentAnimator != null && currentAnimator!!.isPaused) {
            currentAnimator?.resume()
            return
        }
        currentAnimator?.cancel()

        val currentScale = breathingCircle.scaleX
        var targetScale = currentScale

        when (animType) {
            0 -> targetScale = 1.0f
            1 -> targetScale = 0.5f
            2 -> targetScale = currentScale
        }

        if (targetScale == currentScale) return

        currentAnimator = ValueAnimator.ofFloat(currentScale, targetScale).apply {
            this.duration = duration
            addUpdateListener {
                val scale = it.animatedValue as Float
                breathingCircle.scaleX = scale
                breathingCircle.scaleY = scale
            }
            start()
        }
    }

    private fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun toggleVibration() {
        isVibrationOn = !isVibrationOn
        vibrationButton.text = if (isVibrationOn) "Вибрация: ON" else "Вибрация: OFF"
    }

    private fun toggleMusic() {
        if (isMusicOn) {
            mediaPlayer?.pause()
            isMusicOn = false
            musicButton.alpha = 0.5f
        } else {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
                mediaPlayer?.isLooping = true
            }
            mediaPlayer?.start()
            isMusicOn = true
            musicButton.alpha = 1.0f
        }
    }

    private fun resetCycle() {
        running = false
        currentTimer?.cancel()
        currentAnimator?.cancel()
        cyclesCompleted = 0
        currentPhase = 0

        cyclesTextView.text = "Циклов: 0"
        updateUIForPattern()
        startButton.text = "НАЧАТЬ"

        breathingCircle.scaleX = 0.5f
        breathingCircle.scaleY = 0.5f
        breathingCircle.clearColorFilter()
        breathingCircle.setColorFilter(ContextCompat.getColor(this, R.color.breatheIn))
    }

    private fun showCustomDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val input1 = EditText(this).apply { hint = "Вдох (сек)"; inputType = 2 }
        val input2 = EditText(this).apply { hint = "Пауза (сек)"; inputType = 2 }
        val input3 = EditText(this).apply { hint = "Выдох (сек)"; inputType = 2 }
        val input4 = EditText(this).apply { hint = "Пауза (сек)"; inputType = 2 }

        layout.addView(input1)
        layout.addView(input2)
        layout.addView(input3)
        layout.addView(input4)

        AlertDialog.Builder(this)
            .setTitle("Настрой ритм")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                val t1 = (input1.text.toString().toLongOrNull() ?: 4) * 1000
                val t2 = (input2.text.toString().toLongOrNull() ?: 0) * 1000
                val t3 = (input3.text.toString().toLongOrNull() ?: 4) * 1000
                val t4 = (input4.text.toString().toLongOrNull() ?: 0) * 1000

                val custom = BreathingPattern(
                    "Свой режим",
                    longArrayOf(t1, t2, t3, t4),
                    arrayOf("ВДОХ", "ДЕРЖИ", "ВЫДОХ", "ДЕРЖИ"),
                    intArrayOf(R.color.breatheIn, R.color.hold, R.color.breatheOut, R.color.hold),
                    intArrayOf(0, 2, 1, 2)
                )
                setPattern(custom)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (isMusicOn) mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentTimer?.cancel()
        mediaPlayer?.release()
    }
}