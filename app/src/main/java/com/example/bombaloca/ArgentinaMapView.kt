package com.example.bombaloca

import android.content.Context
import android.graphics.*
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
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
    
    // Constantes de escala compartidas entre onDraw y handleProvinceClick
    companion object {
        private const val BASE_SCALE_X_DIVISOR = 85f
        private const val BASE_SCALE_Y_DIVISOR = 160f
    }
    
    // Función auxiliar para calcular escalas de manera consistente
    private fun calculateScales(): Pair<Float, Float> {
        val baseScaleX = width / BASE_SCALE_X_DIVISOR
        val baseScaleY = height / BASE_SCALE_Y_DIVISOR
        return Pair(baseScaleX * zoomLevel, baseScaleY * zoomLevel)
    }

    private var provinces = Province.getAllProvinces().toMutableList()
    private var onProvinceClickListener: ((Province) -> Unit)? = null
    
    private val mapDrawable: VectorDrawableCompat? by lazy {
        VectorDrawableCompat.create(context.resources, R.drawable.argentina_map_real, null)
    }

    // Cargar datos de provincias desde archivo JSON
    private val provinceShapes by lazy { ProvinceDataLoader.loadProvinceShapes(context) }
    
    // Variables para zoom y pan interactivo
    private var zoomLevel = 0.48f  // Zoom inicial más pequeño (equivale a 4 clicks de zoom out: 0.58f / 1.2f ≈ 0.48f)
    private var panX = -80f  // Posición inicial hacia la derecha (equivale a 2 clicks de flecha derecha: 0f - 40f - 40f = -80f)
    private var panY = -80f  // Posición inicial más abajo (equivale a 2 clicks de flecha abajo)
    
    // Variables para gestos
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = -1
    
    // Provincia de referencia para centrar el mapa
    private val centerProvincePathName = "la_pampa"  // Usamos La Pampa como centro de Argentina
    
    /**
     * Calcula el offset necesario para centrar la provincia de referencia en la pantalla
     */
    private fun calculateCenterOffset(scaleX: Float, scaleY: Float): Pair<Float, Float> {
        val centerProvince = provinceShapes.find { it.pathName == centerProvincePathName }
        
        return if (centerProvince != null) {
            // Obtener el centro de la provincia de referencia
            val provinceCenterX = (centerProvince.boundingBox.left + centerProvince.boundingBox.right) / 2f
            val provinceCenterY = (centerProvince.boundingBox.top + centerProvince.boundingBox.bottom) / 2f
            
            // Calcular donde estaría el centro de la provincia en coordenadas de pantalla
            val provinceScreenX = provinceCenterX * scaleX
            val provinceScreenY = provinceCenterY * scaleY
            
            // Calcular el centro de la pantalla
            val screenCenterX = width / 2f
            val screenCenterY = height / 2f
            
            // Calcular el offset necesario para centrar la provincia
            val offsetX = screenCenterX - provinceScreenX
            val offsetY = screenCenterY - provinceScreenY
            
            Pair(offsetX, offsetY)
        } else {
            // Fallback si no se encuentra la provincia de referencia
            Pair(0f, 0f)
        }
    }
    
    // Detector de gestos de escala (zoom)
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            zoomLevel *= scaleFactor
            
            // Limitar el zoom entre 0.5x y 3.0x
            zoomLevel = zoomLevel.coerceIn(0.5f, 3.0f)
            
            invalidate()
            return true
        }
    })

    fun setOnProvinceClickListener(listener: (Province) -> Unit) {
        onProvinceClickListener = listener
    }
    
    // Métodos para controlar zoom y pan desde UI externa
    fun zoomIn() {
        zoomLevel = (zoomLevel * 1.2f).coerceAtMost(3.0f)
        invalidate()
    }
    
    fun zoomOut() {
        zoomLevel = (zoomLevel / 1.2f).coerceAtLeast(0.2f)
        invalidate()
    }
    
    fun panLeft() {
        panX += 40f  // Invertido para que funcione correctamente
        invalidate()
    }
    
    fun panRight() {
        panX -= 40f  // Invertido para que funcione correctamente
        invalidate()
    }
    
    fun panUp() {
        panY += 40f  // Invertido para que funcione correctamente
        invalidate()
    }
    
    fun panDown() {
        panY -= 40f  // Invertido para que funcione correctamente
        invalidate()
    }
    
    fun resetPosition() {
        zoomLevel = 0.48f  // Usar el mismo zoom inicial (4 clicks de zoom out)
        panX = -80f  // Usar la misma posición inicial (2 clicks de flecha derecha)
        panY = -80f  // Usar la misma posición inicial (2 clicks de flecha abajo)
        invalidate()
    }

    fun updateProvinces(newProvinces: List<Province>) {
        provinces.clear()
        provinces.addAll(newProvinces)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Solo manejar clicks simples para seleccionar provincias
        if (event.action == MotionEvent.ACTION_UP) {
            handleProvinceClick(event.x, event.y)
        }
        return true
    }
    
    private fun handleProvinceClick(x: Float, y: Float) {
        // Calcular las mismas escalas y offsets que en onDraw
        val (scaleX, scaleY) = calculateScales()
        
        // Calcular offset para centrar La Pampa en pantalla (mismo cálculo que en onDraw)
        val (centerOffsetX, centerOffsetY) = calculateCenterOffset(scaleX, scaleY)
        
        val offsetX = centerOffsetX + panX
        val offsetY = centerOffsetY + panY

        val clickedProvince = findNearestProvince(x, y, scaleX, scaleY, offsetX, offsetY)
        if (clickedProvince != null) {
            onProvinceClickListener?.invoke(clickedProvince)
        }
    }

    private fun findNearestProvince(clickX: Float, clickY: Float, scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float): Province? {
        var nearestProvince: Province? = null
        var shortestDistance = Float.MAX_VALUE
        
        for (province in provinces) {
            val provinceShape = provinceShapes.find { it.pathName == province.pathName } ?: continue
            val region = provinceShape.boundingBox
            
            // Calcular el centro de la provincia
            val centerX = (region.left + region.right) / 2f * scaleX + offsetX
            val centerY = (region.top + region.bottom) / 2f * scaleY + offsetY
            
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

        // Ajustar escala y posición del mapa con zoom y pan interactivos
        val (scaleX, scaleY) = calculateScales()
        
        // Calcular offset para centrar La Pampa en pantalla
        val (centerOffsetX, centerOffsetY) = calculateCenterOffset(scaleX, scaleY)
        
        val offsetX = centerOffsetX + panX
        val offsetY = centerOffsetY + panY

        // Renderizar todas las provincias con sus colores y stroke anti-gap
        for (province in provinces) {
            // Determinar el color de cada provincia según su estado
            val color = when {
                province.isConquered -> Color.parseColor("#4CAF50") // Verde para conquistadas
                province.isSelected -> Color.parseColor("#FF5722") // Rojo para selección incorrecta (temporal)
                else -> Color.parseColor("#E0E0E0") // Gris por defecto
            }
            
            // Dibujar cada provincia con stroke anti-gap
            drawProvinceWithAntiGap(canvas, province.pathName, color, scaleX, scaleY, offsetX, offsetY)
        }

        // Dibujar nombres SOLO de provincias conquistadas (como confirmación visual)
        for (province in provinces) {
            // Solo mostrar nombre si está conquistada
            if (!province.isConquered) continue
            
            val provinceShape = provinceShapes.find { it.pathName == province.pathName } ?: continue
            val region = provinceShape.boundingBox
            val scaledRegion = RectF(
                region.left * scaleX + offsetX,
                region.top * scaleY + offsetY,
                region.right * scaleX + offsetX,
                region.bottom * scaleY + offsetY
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

    private fun drawProvinceWithAntiGap(canvas: Canvas, pathName: String, color: Int, scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
        val provinceShape = provinceShapes.find { it.pathName == pathName } ?: return
        
        // Configurar paint para relleno
        paint.color = color
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        paint.alpha = 255

        // Configurar stroke visible para separaciones entre provincias
        val strokePaint = Paint().apply {
            this.color = Color.parseColor("#333333") // Gris oscuro para separaciones visibles
            strokeWidth = 1.5f * minOf(scaleX, scaleY) // Stroke moderado para separaciones
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Crear y dibujar el path
        val path = parseSvgPath(provinceShape.svgPath, scaleX, scaleY, offsetX, offsetY)
        
        // Dibujar el relleno
        canvas.drawPath(path, paint)
        // Luego el stroke para separaciones visibles
        canvas.drawPath(path, strokePaint)
    }

    private fun drawProvinceShape(canvas: Canvas, pathName: String, color: Int, scaleX: Float, scaleY: Float, expandPath: Boolean = false, offsetX: Float = 0f, offsetY: Float = 0f) {
        // Buscar datos de la provincia en el JSON
        val provinceShape = provinceShapes.find { it.pathName == pathName }
        
        if (provinceShape != null) {
            // Configurar paint para la provincia
            paint.color = color
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
            paint.alpha = 255

            // Configurar stroke muy delgado para evitar gaps visibles
            strokePaint.color = Color.parseColor("#666666")
            strokePaint.strokeWidth = 0.5f * minOf(scaleX, scaleY)

            // Crear y dibujar el path real de la provincia usando datos del JSON
            val path = parseSvgPath(provinceShape.svgPath, scaleX, scaleY, offsetX, offsetY)
            
            if (expandPath) {
                // Para la primera pasada, usar stroke más grueso para cubrir gaps
                val expandedStroke = Paint().apply {
                    isAntiAlias = true
                    strokeWidth = 2.0f * minOf(scaleX, scaleY)
                    style = Paint.Style.STROKE
                    this.color = color
                }
                canvas.drawPath(path, expandedStroke)
            }
            
            canvas.drawPath(path, paint)
            if (!expandPath) {
                canvas.drawPath(path, strokePaint)
            }
        } else {
            // Fallback: dibujar rectángulo si no se encuentra la provincia
            paint.color = color
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
            canvas.drawCircle(50f, 50f, 20f, paint)
        }
    }
    
    private fun parseSvgPath(pathData: String, scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float): Path {
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
                                val x = coords[0].toFloat() * scaleX + offsetX
                                val y = coords[1].toFloat() * scaleY + offsetY
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
                                val x = coords[0].toFloat() * scaleX + offsetX
                                val y = coords[1].toFloat() * scaleY + offsetY
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