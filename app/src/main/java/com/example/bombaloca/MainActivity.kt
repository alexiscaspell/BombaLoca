package com.example.bombaloca

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var questionTextView: TextView
    private lateinit var argentinaMapView: ArgentinaMapView
    private lateinit var bombStateIcon: ImageView
    private lateinit var restartButton: Button
    private lateinit var exitButton: Button
    private lateinit var explosionImageView: ImageView
    private lateinit var explosionImageView2: ImageView
    private lateinit var explosionImageView3: ImageView
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
    private var explosionPlayer: MediaPlayer? = null  // Reproductor separado para explosiones
    private lateinit var vibrator: Vibrator
    private var isGameActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        questionTextView = findViewById(R.id.questionTextView)
        argentinaMapView = findViewById(R.id.argentinaMapView)
        bombStateIcon = findViewById(R.id.bombStateIcon)
        restartButton = findViewById(R.id.restartButton)
        exitButton = findViewById(R.id.exitButton)
        explosionImageView = findViewById(R.id.explosionImageView)
        explosionImageView2 = findViewById(R.id.explosionImageView2)
        explosionImageView3 = findViewById(R.id.explosionImageView3)
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
        // Flash the clicked province red
        argentinaMapView.flashProvince(province.pathName, android.graphics.Color.RED)
        
        // Lose a life
        lives--
        updateLivesDisplay()
        
        // Check if game over
        if (lives <= 0) {
            // Game over - only explosion sound, no error sound
            showGameOver()
        } else {
            // Still have lives - play error sound
            playWrongAnswerEffect()
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
        questionTextView.text = "🎯 Desconecta el cable de: ${currentProvince!!.name}"
    }
    
    private fun updateLivesDisplay() {
        // Cambiar el icono de la bomba según las vidas restantes
        val bombDrawable = when(lives) {
            3 -> R.drawable.bomb_healthy  // Bomba sana
            2 -> R.drawable.bomb_damaged  // Bomba dañada
            1 -> R.drawable.bomb_critical // Bomba crítica
            else -> R.drawable.bomb_critical // Bomba crítica (sin vidas)
        }
        bombStateIcon.setImageResource(bombDrawable)
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
        
        // Stop only the regular game audio, not the explosion
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Stop any previous explosion audio
        explosionPlayer?.release()
        explosionPlayer = null
        
        // Play nuclear explosion sound with dedicated player
        try {
            explosionPlayer = MediaPlayer.create(this, R.raw.nuclear_explosion)
            explosionPlayer?.let { player ->
                // Set volume to maximum for dramatic effect
                player.setVolume(1.0f, 1.0f)
                
                // Don't release immediately - let it play completely
                player.setOnCompletionListener { mp ->
                    // Only release after the audio completes naturally
                    mp.release()
                    explosionPlayer = null
                }
                
                player.setOnErrorListener { mp, what, extra ->
                    mp.release()
                    explosionPlayer = null
                    true
                }
                
                // Ensure the player is prepared before starting
                player.prepareAsync()
                player.setOnPreparedListener { mp ->
                    mp.start()
                }
            }
        } catch (e: Exception) {
            // Handle audio error gracefully
            explosionPlayer = null
        }
        
        // Show multiple explosion GIFs using Glide with staggered timing
        showExplosionSequence()
        
        // Update question text
        questionTextView.text = "💥🇦🇷 ¡EXPLOTÓ ARGENTINA! 🇦🇷💥\n¡La Bomba Loca ganó esta vez!"
        
        // Show restart/exit buttons after giving enough time for the 7-second audio
        Handler(Looper.getMainLooper()).postDelayed({
            showEndGameButtons()
        }, 8000) // 8 seconds to ensure the 7-second audio plays completely
    }
    
    private fun showEndGameButtons() {
        restartButton.visibility = View.VISIBLE
        exitButton.visibility = View.VISIBLE
    }
    
    private fun hideEndGameElements() {
        victoryTextView.visibility = View.GONE
        victoryTextView.alpha = 0f
        
        // Hide all explosion views
        explosionImageView.visibility = View.GONE
        explosionImageView.alpha = 0f
        explosionImageView2.visibility = View.GONE
        explosionImageView2.alpha = 0f
        explosionImageView3.visibility = View.GONE
        explosionImageView3.alpha = 0f
        
        // Clear any GIFs from Glide
        Glide.with(this).clear(explosionImageView)
        Glide.with(this).clear(explosionImageView2)
        Glide.with(this).clear(explosionImageView3)
        
        // Stop game audio
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Stop explosion audio separately
        explosionPlayer?.release()
        explosionPlayer = null
        
        restartButton.visibility = View.GONE
        exitButton.visibility = View.GONE
    }

    private fun restartGame() {
        // Stop explosion audio immediately when restarting
        explosionPlayer?.release()
        explosionPlayer = null
        
        // Hide end game elements and start new game
        hideEndGameElements()
        startGame()
    }
    
    private fun showExplosionSequence() {
        // Explosión central - inmediata y más grande
        showSingleExplosion(explosionImageView, 0, 1.4f)
        
        // Explosión superior izquierda - después de 200ms
        showSingleExplosion(explosionImageView2, 200, 1.1f)
        
        // Explosión inferior derecha - después de 400ms
        showSingleExplosion(explosionImageView3, 400, 1.2f)
    }
    
    private fun showSingleExplosion(imageView: ImageView, delayMs: Long, maxScale: Float) {
        Handler(Looper.getMainLooper()).postDelayed({
            imageView.visibility = View.VISIBLE
            
            // Load GIF using Glide
            try {
                Glide.with(this)
                    .asGif()
                    .load("file:///android_asset/explosion.gif")
                    .into(imageView)
            } catch (e: Exception) {
                // Fallback to static explosion icon if GIF fails
                imageView.setImageResource(R.drawable.explosion_icon)
            }
            
            // Animate the explosion appearance
            val scaleX = ObjectAnimator.ofFloat(imageView, "scaleX", 0f, maxScale)
            val scaleY = ObjectAnimator.ofFloat(imageView, "scaleY", 0f, maxScale)
            val alpha = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
            val rotation = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f * (0.5f + Math.random().toFloat()))
            
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY, alpha, rotation)
            animatorSet.duration = 600 + (Math.random() * 400).toLong() // Variación aleatoria
            animatorSet.interpolator = OvershootInterpolator()
            animatorSet.start()
        }, delayMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        isGameActive = false
        
        // Clean up both media players to prevent memory leaks
        mediaPlayer?.release()
        mediaPlayer = null
        
        explosionPlayer?.release()
        explosionPlayer = null
    }
}
