package com.example.bombaloca

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class IntroActivity : AppCompatActivity() {
    private lateinit var logoImage: ImageView
    private lateinit var bombaLocaImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var storyText: TextView
    private lateinit var startButton: Button
    private lateinit var exitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        // Inicializar vistas
        logoImage = findViewById(R.id.logoImage)
        bombaLocaImage = findViewById(R.id.bombaLocaImage)
        titleText = findViewById(R.id.titleText)
        storyText = findViewById(R.id.storyText)
        startButton = findViewById(R.id.startButton)
        exitButton = findViewById(R.id.exitButton)

        // Configurar el prólogo
        setupStory()

        // Configurar botones
        startButton.setOnClickListener {
            // Iniciar el juego principal
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Cerrar la actividad de intro
        }

        exitButton.setOnClickListener {
            // Salir de la aplicación
            finish()
        }
    }

    private fun setupStory() {
        titleText.text = "🚨 ALERTA NACIONAL 🚨"
        
        storyText.text = """
            ¡La temible BOMBA LOCA ha conectado una bomba gigante en toda Argentina! 💣
            
            Ha colocado cables especiales en cada provincia del país. Para desactivar la bomba, debes desconectar los cables en el orden correcto.
            
            🎯 TU MISIÓN:
            • Encuentra cada provincia cuando se te indique
            • ¡Cuidado! Si tocas la provincia equivocada, dañarás la bomba
            • Tienes solo 3 oportunidades antes de que...
            
            💥 ¡ARGENTINA EXPLOTE! 💥
            
            ¿Estás listo para salvar el país? 🇦🇷
        """.trimIndent()
    }
}
