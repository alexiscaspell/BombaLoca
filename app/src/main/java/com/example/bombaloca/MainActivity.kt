package com.example.bombaloca

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var questionTextView: TextView
    private lateinit var argentinaMapView: ArgentinaMapView
    private lateinit var livesCountText: TextView
    private lateinit var restartButton: Button
    private lateinit var exitButton: Button
    private lateinit var explosionImageView: ImageView
    private lateinit var victoryTextView: TextView
    
    // Controles de zoom y pan
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private lateinit var resetButton: Button
    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    
    private var provinces = mutableListOf<Province>()
    private var pendingProvinces = mutableListOf<Province>()
    private var currentProvince: Province? = null
    private var lives = 3
    private var conqueredCount = 0
    
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var vibrator: Vibrator
    private var isGameActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        questionTextView = findViewById(R.id.questionTextView)
        argentinaMapView = findViewById(R.id.argentinaMapView)
        livesCountText = findViewById(R.id.livesCountText)
        restartButton = findViewById(R.id.restartButton)
        exitButton = findViewById(R.id.exitButton)
        explosionImageView = findViewById(R.id.explosionImageView)
        victoryTextView = findViewById(R.id.victoryTextView)
        
        // Initialize control buttons
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        resetButton = findViewById(R.id.resetButton)
        upButton = findViewById(R.id.upButton)
        downButton = findViewById(R.id.downButton)
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)

        // Initialize vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Set up map click listener
        argentinaMapView.setOnProvinceClickListener { province ->
            if (isGameActive) {
                onProvinceClicked(province)
            }
        }

        // Set up button listeners
        restartButton.setOnClickListener {
            restartGame()
        }

        exitButton.setOnClickListener {
            finish()
        }
        
        // Set up map control listeners
        zoomInButton.setOnClickListener {
            argentinaMapView.zoomIn()
        }
        
        zoomOutButton.setOnClickListener {
            argentinaMapView.zoomOut()
        }
        
        resetButton.setOnClickListener {
            argentinaMapView.resetPosition()
        }
        
        upButton.setOnClickListener {
            argentinaMapView.panUp()
        }
        
        downButton.setOnClickListener {
            argentinaMapView.panDown()
        }
        
        leftButton.setOnClickListener {
            argentinaMapView.panLeft()
        }
        
        rightButton.setOnClickListener {
            argentinaMapView.panRight()
        }

        // Start the game
        startGame()
    }

    private fun onProvinceClicked(province: Province) {
        if (!isGameActive || currentProvince == null) return
        
        if (province.pathName == currentProvince!!.pathName) {
            // Correct answer!
            handleCorrectAnswer(province)
        } else {
            // Wrong answer!
            handleWrongAnswer(province)
        }
    }
    
    private fun handleCorrectAnswer(province: Province) {
        playCorrectAnswerEffect()
        
        // Mark province as conquered
        argentinaMapView.markProvinceAsConquered(province.pathName)
        province.isConquered = true
        conqueredCount++
        
        // Remove from pending list
        pendingProvinces.removeIf { it.pathName == province.pathName }
        
        // Check if game is won
        if (conqueredCount >= provinces.size) {
            showVictory()
        } else {
            // Continue to next province
            selectNextProvince()
        }
    }
    
    private fun handleWrongAnswer(province: Province) {
        playWrongAnswerEffect()
        
        // Flash the clicked province red
        argentinaMapView.flashProvince(province.pathName, android.graphics.Color.RED)
        
        // Lose a life
        lives--
        updateLivesDisplay()
        
        // Check if game over
        if (lives <= 0) {
            showGameOver()
        }
    }
    
    private fun playWrongAnswerEffect() {
        if (!isGameActive) return
        
        // Vibrate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }

        // Play sound
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.wrong_answer)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
            mediaPlayer = null
        }
        mediaPlayer?.start()
    }

    private fun playCorrectAnswerEffect() {
        if (!isGameActive) return
        
        // Play sound
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.correct_answer)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
            mediaPlayer = null
        }
        mediaPlayer?.start()
    }

    private fun startGame() {
        // Reset game state
        isGameActive = true
        lives = 3
        conqueredCount = 0
        
        // Initialize provinces
        provinces = Province.getAllProvinces().toMutableList()
        pendingProvinces = provinces.toMutableList()
        
        // Reset UI
        updateLivesDisplay()
        hideEndGameElements()
        
        // Update map
        argentinaMapView.updateProvinces(provinces)
        
        // Select first province
        selectNextProvince()
    }
    
    private fun selectNextProvince() {
        if (pendingProvinces.isEmpty()) return
        
        // Select random province from pending list
        currentProvince = pendingProvinces.random()
        questionTextView.text = "Busca: ${currentProvince!!.name}"
    }
    
    private fun updateLivesDisplay() {
        livesCountText.text = lives.toString()
        
        // Cambiar color según las vidas restantes
        livesCountText.setTextColor(when(lives) {
            3 -> android.graphics.Color.parseColor("#4CAF50") // Verde
            2 -> android.graphics.Color.parseColor("#FF9800") // Naranja  
            1 -> android.graphics.Color.parseColor("#F44336") // Rojo
            else -> android.graphics.Color.parseColor("#9E9E9E") // Gris
        })
    }
    
    private fun showVictory() {
        isGameActive = false
        
        // Show victory animation
        victoryTextView.visibility = View.VISIBLE
        val scaleX = ObjectAnimator.ofFloat(victoryTextView, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(victoryTextView, "scaleY", 0f, 1.2f, 1f)
        val alpha = ObjectAnimator.ofFloat(victoryTextView, "alpha", 0f, 1f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 1000
        animatorSet.interpolator = OvershootInterpolator()
        animatorSet.start()
        
        // Show restart/exit buttons after animation
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                showEndGameButtons()
            }
        })
    }
    
    private fun showGameOver() {
        isGameActive = false
        
        // Show explosion animation
        explosionImageView.visibility = View.VISIBLE
        val scaleX = ObjectAnimator.ofFloat(explosionImageView, "scaleX", 0f, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(explosionImageView, "scaleY", 0f, 1.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(explosionImageView, "alpha", 0f, 1f, 0.8f)
        val rotation = ObjectAnimator.ofFloat(explosionImageView, "rotation", 0f, 360f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha, rotation)
        animatorSet.duration = 1500
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
        
        // Update question text
        questionTextView.text = "💥 ¡GAME OVER! 💥\nTe quedaste sin vidas"
        
        // Show restart/exit buttons after animation
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                showEndGameButtons()
            }
        })
    }
    
    private fun showEndGameButtons() {
        restartButton.visibility = View.VISIBLE
        exitButton.visibility = View.VISIBLE
    }
    
    private fun hideEndGameElements() {
        victoryTextView.visibility = View.GONE
        victoryTextView.alpha = 0f
        explosionImageView.visibility = View.GONE
        explosionImageView.alpha = 0f
        restartButton.visibility = View.GONE
        exitButton.visibility = View.GONE
    }

    private fun restartGame() {
        startGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        isGameActive = false
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
