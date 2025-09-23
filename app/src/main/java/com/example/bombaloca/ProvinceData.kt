package com.example.bombaloca

import android.content.Context
import android.graphics.RectF
import org.json.JSONObject
import java.io.IOException

data class ProvinceShape(
    val name: String,
    val pathName: String,
    val svgPath: String,
    val boundingBox: RectF
)

object ProvinceDataLoader {
    private var cachedProvinces: List<ProvinceShape>? = null
    
    fun loadProvinceShapes(context: Context): List<ProvinceShape> {
        // Cache the data to avoid re-parsing
        if (cachedProvinces != null) {
            return cachedProvinces!!
        }
        
        try {
            val inputStream = context.assets.open("provinces_data.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            
            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val provincesArray = jsonObject.getJSONArray("provinces")
            
            val provinces = mutableListOf<ProvinceShape>()
            
            for (i in 0 until provincesArray.length()) {
                val provinceObj = provincesArray.getJSONObject(i)
                val boundingBoxObj = provinceObj.getJSONObject("boundingBox")
                
                val province = ProvinceShape(
                    name = provinceObj.getString("name"),
                    pathName = provinceObj.getString("pathName"),
                    svgPath = provinceObj.getString("svgPath"),
                    boundingBox = RectF(
                        boundingBoxObj.getDouble("left").toFloat(),
                        boundingBoxObj.getDouble("top").toFloat(),
                        boundingBoxObj.getDouble("right").toFloat(),
                        boundingBoxObj.getDouble("bottom").toFloat()
                    )
                )
                provinces.add(province)
            }
            
            cachedProvinces = provinces
            return provinces
            
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    fun getProvinceByPathName(context: Context, pathName: String): ProvinceShape? {
        return loadProvinceShapes(context).find { it.pathName == pathName }
    }
}
