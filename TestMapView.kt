package com.example.bombaloca

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TestMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        alpha = 255
    }

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#000000")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Fondo blanco para ver mejor
        canvas.drawColor(Color.WHITE)

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        
        // Escala para ajustar el mapa al view
        val scaleX = width / 400f
        val scaleY = height / 600f

        // Probar con Buenos Aires (una provincia grande)
        paint.color = Color.parseColor("#4CAF50") // Verde
        drawTestProvince(canvas, "buenos_aires", scaleX, scaleY)

        // Probar con Tucumán (una provincia pequeña)
        paint.color = Color.parseColor("#F44336") // Rojo
        drawTestProvince(canvas, "tucuman", scaleX, scaleY)

        // Probar con Salta (provincia mediana)
        paint.color = Color.parseColor("#2196F3") // Azul
        drawTestProvince(canvas, "salta", scaleX, scaleY)
    }

    private fun drawTestProvince(canvas: Canvas, pathName: String, scaleX: Float, scaleY: Float) {
        // Usar los mismos paths que en ArgentinaMapView
        val provincePaths = mapOf(
            "buenos_aires" to "M266.8,206.9 L279.9,214.4 L287.2,215.3 L288.4,216.7 L291.6,217.2 L301.0,221.2 L305.0,222.8 L303.2,225.3 L303.7,226.9 L302.0,230.5 L309.7,233.8 L316.7,236.0 L328.9,243.9 L325.3,253.3 L326.7,258.0 L334.4,262.9 L338.1,262.1 L339.2,273.1 L323.7,288.7 L322.2,293.2 L310.5,299.9 L292.2,304.7 L253.0,310.0 L242.2,310.7 L235.7,310.1 L224.9,306.8 L225.8,309.7 L225.6,311.9 L226.0,314.3 L229.1,315.6 L230.9,317.7 L227.9,316.0 L228.6,317.6 L231.0,320.8 L226.4,325.1 L225.4,329.2 L223.4,333.0 L225.0,337.1 L227.5,339.7 L225.8,340.2 L227.7,340.9 L218.2,347.2 L212.8,343.9 L204.6,297.3 L204.7,238.4 L254.0,213.7 L256.2,212.3 L261.8,213.8 L264.5,210.4 L265.8,208.5 L267.1,207.3 Z",
            "tucuman" to "M151.0,80.6 L158.0,81.2 L158.2,80.7 L158.5,78.7 L158.7,77.4 L159.6,77.6 L163.6,78.2 L165.7,77.4 L166.4,78.0 L171.0,80.4 L172.9,80.8 L174.2,80.8 L176.3,79.9 L182.6,81.2 L182.1,85.9 L180.9,88.2 L179.7,90.4 L178.8,90.5 L177.3,94.3 L176.7,95.0 L175.1,98.6 L173.7,100.1 L171.0,102.6 L172.8,103.3 L172.5,103.9 L172.0,104.3 L171.0,104.4 L172.2,108.1 L171.6,108.3 L170.4,110.2 L169.3,110.2 L167.6,110.7 L165.8,109.7 L162.9,110.9 L161.1,112.9 L158.6,110.4 L157.0,108.2 L155.3,108.4 L154.7,106.8 L153.8,106.1 L153.1,104.4 L152.4,101.1 L149.3,100.2 L148.2,99.7 L150.2,97.5 L151.0,96.4 L154.5,93.0 L154.8,90.8 L155.1,89.0 L154.7,88.6 L151.1,86.5 L149.1,85.7 L149.8,83.3 L150.2,81.5 Z",
            "salta" to "M102.0,60.8 L103.2,57.5 L101.7,56.1 L102.7,51.2 L107.7,46.6 L124.8,40.5 L129.1,36.6 L132.5,39.0 L136.7,41.7 L144.6,41.7 L144.0,31.7 L147.9,29.4 L152.4,33.8 L151.7,37.4 L152.1,38.9 L154.1,39.6 L160.4,47.2 L163.0,47.7 L168.9,48.1 L175.1,48.8 L182.2,48.6 L185.7,47.5 L187.9,31.0 L182.3,32.0 L178.3,30.2 L174.7,30.7 L171.5,26.7 L171.2,21.8 L166.6,17.4 L165.3,14.5 L167.4,8.4 L178.5,7.0 L181.1,10.3 L183.4,14.2 L185.1,18.3 L191.1,8.2 L193.7,3.9 L216.3,3.9 L219.2,8.0 L220.7,9.6 L225.8,47.2 L183.4,78.1 L173.7,80.9 L166.1,77.6 L159.6,77.6 L158.2,80.1 L149.2,79.2 L144.9,82.7 L140.0,78.5 L136.7,70.5 L142.2,68.8 L141.6,65.0 Z"
        )

        // Usar los mismos bounding boxes
        val provinceRegions = mapOf(
            "buenos_aires" to RectF(204.6f, 206.9f, 339.2f, 347.2f),
            "tucuman" to RectF(140.2f, 70.4f, 190.6f, 120.9f),
            "salta" to RectF(101.7f, 3.9f, 225.8f, 82.7f)
        )

        val pathData = provincePaths[pathName]
        val region = provinceRegions[pathName]

        if (pathData != null) {
            // Intentar con path SVG
            val path = parseSvgPath(pathData, scaleX, scaleY)
            canvas.drawPath(path, paint)
            canvas.drawPath(path, strokePaint)
        } else if (region != null) {
            // Fallback: rectángulo
            val scaledRegion = RectF(
                region.left * scaleX,
                region.top * scaleY,
                region.right * scaleX,
                region.bottom * scaleY
            )
            canvas.drawRect(scaledRegion, paint)
            canvas.drawRect(scaledRegion, strokePaint)
        }

        // Dibujar texto para identificar
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        
        if (region != null) {
            val centerX = (region.left + region.right) / 2 * scaleX
            val centerY = (region.top + region.bottom) / 2 * scaleY
            canvas.drawText(pathName, centerX, centerY, textPaint)
        }
    }

    private fun parseSvgPath(pathData: String, scaleX: Float, scaleY: Float): Path {
        val path = Path()
        val commands = pathData.split(" ")
        var i = 0
        
        while (i < commands.size) {
            when (commands[i]) {
                "M" -> {
                    // Move to
                    if (i + 1 < commands.size) {
                        val coords = commands[i + 1].split(",")
                        if (coords.size == 2) {
                            val x = coords[0].toFloat() * scaleX
                            val y = coords[1].toFloat() * scaleY
                            path.moveTo(x, y)
                        }
                        i += 2
                    } else i++
                }
                "L" -> {
                    // Line to
                    if (i + 1 < commands.size) {
                        val coords = commands[i + 1].split(",")
                        if (coords.size == 2) {
                            val x = coords[0].toFloat() * scaleX
                            val y = coords[1].toFloat() * scaleY
                            path.lineTo(x, y)
                        }
                        i += 2
                    } else i++
                }
                "Z" -> {
                    // Close path
                    path.close()
                    i++
                }
                else -> {
                    // Skip unknown commands or handle coordinates directly
                    i++
                }
            }
        }
        
        return path
    }
}
