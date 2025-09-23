#!/usr/bin/env python3
"""
Convierte el GeoJSON real de Argentina en un VectorDrawable de Android
con formas precisas de las provincias.
"""

import json
import math

def geo_to_canvas(lon, lat, bounds, canvas_width=400, canvas_height=600):
    """Convierte coordenadas geográficas a coordenadas de canvas"""
    min_lon, min_lat, max_lon, max_lat = bounds
    x = ((lon - min_lon) / (max_lon - min_lon)) * canvas_width
    y = canvas_height - ((lat - min_lat) / (max_lat - min_lat)) * canvas_height
    return x, y

def simplify_path(coords, tolerance=1.0):
    """Simplifica un path para reducir el número de puntos manteniendo la precisión en los límites"""
    if len(coords) <= 2:
        return coords
    
    # Reducir la simplificación para mantener más detalle en los límites
    # Usar un step más conservador para evitar gaps entre provincias
    step = max(1, len(coords) // 80)  # Aumentar a 80 puntos por provincia para más precisión
    simplified = []
    for i in range(0, len(coords), step):
        simplified.append(coords[i])
    
    # Asegurar que el último punto esté incluido
    if coords[-1] not in simplified:
        simplified.append(coords[-1])
    
    return simplified

def normalize_province_name(name):
    """Normaliza nombres de provincias para crear path names consistentes"""
    # Mapeo de nombres específicos
    name_mapping = {
        "Ciudad Autónoma de Buenos Aires": "caba",
        "Tierra del Fuego, Antártida e Islas del Atlántico Sur": "tierra_del_fuego"
    }
    
    if name in name_mapping:
        return name_mapping[name]
    
    # Normalización general
    normalized = name.lower()
    normalized = normalized.replace("á", "a").replace("é", "e").replace("í", "i")
    normalized = normalized.replace("ó", "o").replace("ú", "u").replace("ñ", "n")
    normalized = normalized.replace(" ", "_").replace(",", "").replace(".", "")
    normalized = normalized.replace("'", "").replace("-", "_")
    
    return normalized

def main():
    print("🇦🇷 Convirtiendo GeoJSON real de Argentina a VectorDrawable...")
    
    # Leer el GeoJSON
    with open('/Users/tostado/Downloads/ar.json', 'r', encoding='utf-8') as f:
        geojson_data = json.load(f)
    
    print(f"📊 Encontradas {len(geojson_data['features'])} provincias")
    
    # Calcular bounds globales
    all_coords = []
    for feature in geojson_data['features']:
        geometry = feature['geometry']
        if geometry['type'] == 'Polygon':
            coords = geometry['coordinates'][0]  # Exterior ring
            all_coords.extend(coords)
        elif geometry['type'] == 'MultiPolygon':
            # Para MultiPolygon, tomar el polígono más grande
            largest_polygon = max(geometry['coordinates'], key=lambda p: len(p[0]))
            coords = largest_polygon[0]  # Exterior ring del polígono más grande
            all_coords.extend(coords)
    
    lons = [c[0] for c in all_coords]
    lats = [c[1] for c in all_coords]
    bounds = (min(lons), min(lats), max(lons), max(lats))
    
    print(f"🗺️  Bounds: {bounds}")
    
    # Generar VectorDrawable
    xml_content = """<?xml version="1.0" encoding="utf-8"?>
<!-- Mapa real de Argentina generado desde GeoJSON oficial -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="400dp"
    android:height="600dp"
    android:viewportWidth="400"
    android:viewportHeight="600">

"""
    
    province_regions = {}  # Para generar las coordenadas de hit detection
    
    for feature in geojson_data['features']:
        # Obtener nombre de la provincia
        properties = feature.get('properties', {})
        province_name = properties.get('NAME_1') or properties.get('name') or properties.get('NAME') or "unknown"
        
        # Skip CABA si existe (ya que el usuario dijo que no es provincia)
        if "Ciudad Autónoma" in province_name or province_name == "Ciudad de Buenos Aires":
            print(f"⏭️  Saltando {province_name} (no es provincia)")
            continue
        
        path_name = normalize_province_name(province_name)
        
        geometry = feature['geometry']
        coords = []
        
        if geometry['type'] == 'Polygon':
            coords = geometry['coordinates'][0]  # Exterior ring
        elif geometry['type'] == 'MultiPolygon':
            # Para MultiPolygon, tomar el polígono más grande
            largest_polygon = max(geometry['coordinates'], key=lambda p: len(p[0]))
            coords = largest_polygon[0]  # Exterior ring del polígono más grande
        
        if not coords:
            print(f"⚠️  Sin coordenadas para {province_name}")
            continue
        
        # Simplificar el path para Android
        coords = simplify_path(coords, tolerance=2.0)
        
        # Convertir a coordenadas de canvas
        canvas_coords = [geo_to_canvas(lon, lat, bounds) for lon, lat in coords]
        
        # Calcular bounding box para hit detection
        xs = [c[0] for c in canvas_coords]
        ys = [c[1] for c in canvas_coords]
        bbox = (min(xs), min(ys), max(xs), max(ys))
        province_regions[path_name] = bbox
        
        # Crear path data
        if canvas_coords:
            path_data = f'M{canvas_coords[0][0]:.1f},{canvas_coords[0][1]:.1f}'
            for x, y in canvas_coords[1:]:
                path_data += f' L{x:.1f},{y:.1f}'
            path_data += ' Z'
            
            # Añadir al XML
            xml_content += f'    <!-- {province_name} -->\n'
            xml_content += f'    <path android:name="{path_name}"\n'
            xml_content += f'        android:fillColor="#E0E0E0"\n'
            xml_content += f'        android:strokeColor="#666666"\n'
            xml_content += f'        android:strokeWidth="0"\n'
            xml_content += f'        android:pathData="{path_data}"/>\n\n'
            
            print(f"✅ Procesada: {province_name} -> {path_name}")
    
    xml_content += "</vector>"
    
    # Guardar VectorDrawable
    output_path = "app/src/main/res/drawable/argentina_map_real.xml"
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(xml_content)
    
    print(f"🎨 VectorDrawable guardado en: {output_path}")
    
    # Generar código Kotlin para las regiones de hit detection
    kotlin_code = "    // Coordenadas reales extraídas del GeoJSON oficial de Argentina\n"
    kotlin_code += "    private val provinceRegions = mapOf(\n"
    
    for path_name, (min_x, min_y, max_x, max_y) in province_regions.items():
        kotlin_code += f'        "{path_name}" to RectF({min_x:.1f}f, {min_y:.1f}f, {max_x:.1f}f, {max_y:.1f}f),\n'
    
    kotlin_code = kotlin_code.rstrip(',\n') + '\n    )'
    
    print("\n🔧 Código Kotlin para ArgentinaMapView.kt:")
    print("=" * 60)
    print(kotlin_code)
    print("=" * 60)
    
    print(f"\n🎉 ¡Conversión completada! {len(province_regions)} provincias procesadas")
    print("📱 Ahora actualiza ArgentinaMapView.kt con las nuevas coordenadas")

if __name__ == "__main__":
    main()
