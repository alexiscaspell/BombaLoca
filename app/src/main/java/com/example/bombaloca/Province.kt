package com.example.bombaloca

data class Province(
    val name: String,
    val pathName: String,
    var isConquered: Boolean = false,
    var isSelected: Boolean = false
) {
    companion object {
        fun getAllProvinces(): List<Province> {
            return listOf(
                Province("Buenos Aires", "buenos_aires"),
                Province("Catamarca", "catamarca"),
                Province("Chaco", "chaco"),
                Province("Chubut", "chubut"),
                Province("Córdoba", "cordoba"),
                Province("Corrientes", "corrientes"),
                Province("Entre Ríos", "entre_rios"),
                Province("Formosa", "formosa"),
                Province("Jujuy", "jujuy"),
                Province("La Pampa", "la_pampa"),
                Province("La Rioja", "la_rioja"),
                Province("Mendoza", "mendoza"),
                Province("Misiones", "misiones"),
                Province("Neuquén", "neuquen"),
                Province("Río Negro", "rio_negro"),
                Province("Salta", "salta"),
                Province("San Juan", "san_juan"),
                Province("San Luis", "san_luis"),
                Province("Santa Cruz", "santa_cruz"),
                Province("Santa Fe", "santa_fe"),
                Province("Santiago del Estero", "santiago_del_estero"),
                Province("Tierra del Fuego", "tierra_del_fuego"),
                Province("Tucumán", "tucuman")
            )
        }
    }
}
