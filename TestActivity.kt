package com.example.bombaloca

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout

class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Crear layout simple
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // Crear el TestMapView
        val testMapView = TestMapView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800 // Altura fija para ver bien
            )
        }
        
        layout.addView(testMapView)
        setContentView(layout)
    }
}
