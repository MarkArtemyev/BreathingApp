package com.example.breathingapp

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var instructionTextView: TextView
    private lateinit var cyclesTextView: TextView
    private lateinit var startButton: Button
    private lateinit var finishButton: Button

    // Кнопка вибрации (с текстом "Вибрация: вкл/выкл")
    private lateinit var vibrationButton: Button

    // Кнопка музыки (иконка, без текста)
    private lateinit var musicButton: ImageButton

    private lateinit var breathingCircle: ImageView

    private var running = false
    private var cyclesCompleted = 0
    private var currentTimer: CountDownTimer? = null
    private var currentPhase = 0

    private val phaseDurations = arrayOf(4000L, 7000L, 8000L)
    private val phaseInstructions = arrayOf("ВДЫХАЙ", "ЗАДЕРЖИ", "ВЫДЫХАЙ")
    private val phaseColors = arrayOf(R.color.breatheIn, R.color.hold, R.color.breatheOut)

    // Переменные для вибрации
    private var isVibrationOn = false

    // Инициализация вибратора (учитывая разные API)
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Переменные для музыки
    private var isMusicOn = false
    private var mediaPlayer: MediaPlayer? = null

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
        breathingCircle = findViewById(R.id.breathingCircle)

        startButton.setOnClickListener { toggleTimer() }
        finishButton.setOnClickListener { resetCycle() }
        vibrationButton.setOnClickListener { toggleVibration() }
        musicButton.setOnClickListener { toggleMusic() }
    }

    private fun toggleTimer() {
        running = !running
        startButton.text = if (running) getString(R.string.pause) else getString(R.string.start)

        if (running) startPhase(currentPhase)
        else currentTimer?.cancel()
    }

    private fun startPhase(phase: Int) {
        currentPhase = phase
        val duration = phaseDurations[phase]
        val instruction = phaseInstructions[phase]
        val color = ContextCompat.getColor(this, phaseColors[phase])

        instructionTextView.text = instruction
        instructionTextView.setTextColor(color)
        timerTextView.setTextColor(color)

        // Меняем фон пузыря в зависимости от фазы дыхания
        val drawableRes = when (phase) {
            0 -> R.drawable.breathing_circle_inhale
            1 -> R.drawable.breathing_circle_hold
            2 -> R.drawable.breathing_circle_exhale
            else -> R.drawable.breathing_circle_inhale
        }
        breathingCircle.setImageResource(drawableRes)

        animateCircle(duration, phase)

        // Включаем вибрацию, если она включена
        if (isVibrationOn) {
            vibrateShort()
        }

        currentTimer?.cancel()
        currentTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = ((millisUntilFinished / 1000) + 1).toString()
            }

            override fun onFinish() {
                currentPhase = (currentPhase + 1) % 3
                if (currentPhase == 0 && running) {
                    cyclesCompleted++
                    cyclesTextView.text = getString(R.string.cycles_completed, cyclesCompleted)
                }
                if (running) startPhase(currentPhase)
            }
        }.start()
    }

    private fun animateCircle(duration: Long, phase: Int) {
        val fromScale: Float
        val toScale: Float

        when (phase) {
            0 -> { // ВДЫХАЙ
                fromScale = 0.5f
                toScale = 1f
            }
            1 -> { // ЗАДЕРЖКА (без анимации)
                breathingCircle.scaleX = 1f
                breathingCircle.scaleY = 1f
                return
            }
            2 -> { // ВЫДЫХАЙ
                fromScale = 1f
                toScale = 0.5f
            }
            else -> return
        }

        ValueAnimator.ofFloat(fromScale, toScale).apply {
            this.duration = duration
            addUpdateListener {
                val scale = it.animatedValue as Float
                breathingCircle.scaleX = scale
                breathingCircle.scaleY = scale
            }
            start()
        }
    }

    // ======= Блок вибрации =======
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
        // Меняем текст кнопки
        vibrationButton.text = if (isVibrationOn) "Вибрация: вкл" else "Вибрация: выкл"
    }

    // ======= Блок музыки =======
    private fun toggleMusic() {
        if (isMusicOn) {
            // Выключаем музыку
            mediaPlayer?.pause()
            isMusicOn = false
            // Меняем иконку на "выкл"
            musicButton.setImageResource(R.drawable.ic_music_off)
        } else {
            // Включаем музыку
            if (mediaPlayer == null) {
                // Создаём и настраиваем плеер только 1 раз
                mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
                mediaPlayer?.isLooping = true
            }
            mediaPlayer?.start()
            isMusicOn = true
            // Меняем иконку на "вкл"
            musicButton.setImageResource(R.drawable.ic_music_on)
        }
    }

    // ======= Сброс цикла =======
    private fun resetCycle() {
        running = false
        currentTimer?.cancel()
        cyclesCompleted = 0
        currentPhase = 0

        cyclesTextView.text = getString(R.string.cycles_completed, cyclesCompleted)
        timerTextView.text = "0"
        instructionTextView.text = "ВДЫХАЙ"
        startButton.text = getString(R.string.start)

        breathingCircle.scaleX = 1f
        breathingCircle.scaleY = 1f

        if (running) {
            startPhase(0)
        }
    }

    // ======= Освобождаем ресурсы =======
    override fun onDestroy() {
        super.onDestroy()
        currentTimer?.cancel()

        // Останавливаем музыку, освобождаем плеер
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
