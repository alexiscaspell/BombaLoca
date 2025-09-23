package com.example.bombaloca

import android.content.Context
import android.graphics.*
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import kotlin.math.sqrt

class ArgentinaMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#666666")
    }

    private var provinces = Province.getAllProvinces().toMutableList()
    private var onProvinceClickListener: ((Province) -> Unit)? = null
    
    private val mapDrawable: VectorDrawableCompat? by lazy {
        VectorDrawableCompat.create(context.resources, R.drawable.argentina_map_real, null)
    }

    // Cargar datos de provincias desde archivo JSON
    private val provinceShapes by lazy { ProvinceDataLoader.loadProvinceShapes(context) }

    fun setOnProvinceClickListener(listener: (Province) -> Unit) {
        onProvinceClickListener = listener
    }

    fun updateProvinces(newProvinces: List<Province>) {
        provinces.clear()
        provinces.addAll(newProvinces)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // Ajustar coordenadas según el scaling del view
            val scaleX = width / 400f
            val scaleY = height / 600f

            // Encontrar la provincia más cercana usando sistema de proximidad inteligente
            val clickedProvince = findNearestProvince(x, y, scaleX, scaleY)
            if (clickedProvince != null) {
                onProvinceClickListener?.invoke(clickedProvince)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findNearestProvince(clickX: Float, clickY: Float, scaleX: Float, scaleY: Float): Province? {
        var nearestProvince: Province? = null
        var shortestDistance = Float.MAX_VALUE
        
        for (province in provinces) {
            val provinceShape = provinceShapes.find { it.pathName == province.pathName } ?: continue
            val region = provinceShape.boundingBox
            
            // Calcular el centro de la provincia
            val centerX = (region.left + region.right) / 2f * scaleX
            val centerY = (region.top + region.bottom) / 2f * scaleY
            
            // Calcular distancia del click al centro de la provincia
            val distance = sqrt(
                (clickX - centerX) * (clickX - centerX) + 
                (clickY - centerY) * (clickY - centerY)
            )
            
            // Calcular radio de tolerancia proporcional al tamaño de la provincia
            val provinceWidth = (region.right - region.left) * scaleX
            val provinceHeight = (region.bottom - region.top) * scaleY
            val provinceArea = provinceWidth * provinceHeight
            
            // Radio base mínimo + factor proporcional al área de la provincia
            val baseRadius = 40f // Radio mínimo para provincias pequeñas
            val areaFactor = sqrt(provinceArea) * 0.3f // Factor proporcional
            val toleranceRadius = baseRadius + areaFactor
            
            // Si está dentro del radio de tolerancia y es la más cercana
            if (distance <= toleranceRadius && distance < shortestDistance) {
                nearestProvince = province
                shortestDistance = distance
            }
        }
        
        return nearestProvince
    }

    private fun isPointInProvince(x: Float, y: Float, pathName: String, scaleX: Float, scaleY: Float): Boolean {
        // Buscar datos de la provincia en el JSON
        val provinceShape = provinceShapes.find { it.pathName == pathName } ?: return false
        val region = provinceShape.boundingBox
        
        var scaledRegion = RectF(
            region.left * scaleX,
            region.top * scaleY,
            region.right * scaleX,
            region.bottom * scaleY
        )
        
        // Expandir área clickeable para provincias muy pequeñas
        val minClickableSize = 50f // Aumentado para mejor usabilidad
        if (scaledRegion.width() < minClickableSize || scaledRegion.height() < minClickableSize) {
            val centerX = scaledRegion.centerX()
            val centerY = scaledRegion.centerY()
            val expandedWidth = maxOf(scaledRegion.width(), minClickableSize)
            val expandedHeight = maxOf(scaledRegion.height(), minClickableSize)
            
            scaledRegion = RectF(
                centerX - expandedWidth / 2,
                centerY - expandedHeight / 2,
                centerX + expandedWidth / 2,
                centerY + expandedHeight / 2
            )
        }
        
        return scaledRegion.contains(x, y)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaleX = width / 400f
        val scaleY = height / 600f

        // Renderizar TODAS las provincias con sus colores correspondientes
        for (province in provinces) {
            // Determinar el color de cada provincia según su estado
            val color = when {
                province.isConquered -> Color.parseColor("#4CAF50") // Verde para conquistadas
                province.isSelected -> Color.parseColor("#FF5722") // Rojo para selección incorrecta (temporal)
                else -> Color.parseColor("#E0E0E0") // Gris por defecto
            }
            
            // Dibujar cada provincia con su color específico
            drawProvinceShape(canvas, province.pathName, color, scaleX, scaleY)
        }

        // Dibujar nombres SOLO de provincias conquistadas (como confirmación visual)
        for (province in provinces) {
            // Solo mostrar nombre si está conquistada
            if (!province.isConquered) continue
            
            val provinceShape = provinceShapes.find { it.pathName == province.pathName } ?: continue
            val region = provinceShape.boundingBox
            val scaledRegion = RectF(
                region.left * scaleX,
                region.top * scaleY,
                region.right * scaleX,
                region.bottom * scaleY
            )

            // Mostrar nombre en provincias conquistadas (adaptativo según tamaño)
            if (scaledRegion.width() > 30 && scaledRegion.height() > 20) {
                // Calcular tamaño de texto adaptativo según el tamaño de la provincia
                val baseSize = 8f * minOf(scaleX, scaleY)
                val adaptiveSize = minOf(baseSize, scaledRegion.width() / 8f, scaledRegion.height() / 3f)
                
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = maxOf(adaptiveSize, 6f) // Mínimo 6f para legibilidad
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
                
                val centerX = scaledRegion.centerX()
                val centerY = when (province.pathName) {
                    "san_juan" -> scaledRegion.centerY() + 10f // Bajar texto de San Juan
                    "salta" -> scaledRegion.centerY() + 25f // Bajar mucho más el texto de Salta
                    else -> scaledRegion.centerY()
                }
                
                // Dibujar texto con sombra verde para mejor contraste sobre el verde
                val shadowPaint = Paint(textPaint).apply {
                    color = Color.WHITE
                    setShadowLayer(1.5f, 0f, 0f, Color.parseColor("#2E7D32"))
                }
                
                // Para provincias muy pequeñas, usar solo la primera palabra o abreviación
                val displayText = when {
                    scaledRegion.width() < 50 -> {
                        // Para provincias muy pequeñas, usar abreviaciones
                        when (province.name) {
                            "Santiago del Estero" -> "S. del Estero"
                            "Tierra del Fuego" -> "T. del Fuego"
                            "Entre Ríos" -> "E. Ríos"
                            else -> province.name.split(" ")[0] // Solo primera palabra
                        }
                    }
                    scaledRegion.width() < 80 -> {
                        // Para provincias medianas, usar nombres cortos
                        when (province.name) {
                            "Santiago del Estero" -> "Santiago\ndel Estero"
                            "Tierra del Fuego" -> "Tierra del\nFuego"
                            else -> province.name.replace(" ", "\n")
                        }
                    }
                    else -> province.name // Provincias grandes pueden mostrar nombre completo
                }
                
                // Dibujar texto (con salto de línea si es necesario)
                val lines = displayText.split("\n")
                if (lines.size > 1 && scaledRegion.height() > 35) {
                    val lineHeight = textPaint.textSize * 1.1f
                    val startY = centerY - (lines.size - 1) * lineHeight / 2
                    lines.forEachIndexed { index, line ->
                        canvas.drawText(line, centerX, startY + index * lineHeight, shadowPaint)
                    }
                } else {
                    // Para provincias muy pequeñas, solo una línea
                    canvas.drawText(lines[0], centerX, centerY, shadowPaint)
                }
            }
        }
    }

    private fun drawProvinceShape(canvas: Canvas, pathName: String, color: Int, scaleX: Float, scaleY: Float) {
        // Buscar datos de la provincia en el JSON
        val provinceShape = provinceShapes.find { it.pathName == pathName }
        
        if (provinceShape != null) {
            // Configurar paint para la provincia
            paint.color = color
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
            paint.alpha = 255

            // Configurar stroke más visible
            strokePaint.color = Color.parseColor("#444444")
            strokePaint.strokeWidth = 1.5f * minOf(scaleX, scaleY)

            // Crear y dibujar el path real de la provincia usando datos del JSON
            val path = parseSvgPath(provinceShape.svgPath, scaleX, scaleY)
            canvas.drawPath(path, paint)
            canvas.drawPath(path, strokePaint)
        } else {
            // Fallback: dibujar rectángulo si no se encuentra la provincia
            paint.color = color
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
            canvas.drawCircle(50f, 50f, 20f, paint)
        }
    }
    
    private fun parseSvgPath(pathData: String, scaleX: Float, scaleY: Float): Path {
        val path = Path()
        
        // Usar regex para encontrar comandos y coordenadas
        // Patrón: (M|L|Z) seguido opcionalmente por coordenadas
        val pattern = Regex("([MLZ])([0-9.,\\-\\s]*)")
        val matches = pattern.findAll(pathData)
        
        for (match in matches) {
            val command = match.groupValues[1]
            val coordsStr = match.groupValues[2].trim()
            
            when (command) {
                "M" -> {
                    // Move to
                    if (coordsStr.isNotEmpty()) {
                        val coords = coordsStr.split(",")
                        if (coords.size == 2) {
                            try {
                                val x = coords[0].toFloat() * scaleX
                                val y = coords[1].toFloat() * scaleY
                                path.moveTo(x, y)
                            } catch (e: NumberFormatException) {
                                // Ignorar coordenadas inválidas
                            }
                        }
                    }
                }
                "L" -> {
                    // Line to
                    if (coordsStr.isNotEmpty()) {
                        val coords = coordsStr.split(",")
                        if (coords.size == 2) {
                            try {
                                val x = coords[0].toFloat() * scaleX
                                val y = coords[1].toFloat() * scaleY
                                path.lineTo(x, y)
                            } catch (e: NumberFormatException) {
                                // Ignorar coordenadas inválidas
                            }
                        }
                    }
                }
                "Z" -> {
                    // Close path
                    path.close()
                }
            }
        }
        
        return path
    }

    fun flashProvince(provinceName: String, color: Int, duration: Long = 1000) {
        val province = provinces.find { it.pathName == provinceName }
        if (province != null) {
            province.isSelected = true
            invalidate()
            
            postDelayed({
                province.isSelected = false
                invalidate()
            }, duration)
        }
    }

    fun markProvinceAsConquered(provinceName: String) {
        val province = provinces.find { it.pathName == provinceName }
        if (province != null) {
            province.isConquered = true
            province.isSelected = false
            invalidate()
        }
    }
}