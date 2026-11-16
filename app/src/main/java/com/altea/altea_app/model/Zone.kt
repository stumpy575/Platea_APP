package com.altea.altea_app.model

// Zona de un venue. rows y seatsPerRow son null en zonas de acceso general.
data class Zone(
    val id: String,          // Identificador de la zona (Z1, Z2...)
    val name: String,        // Nombre visible de la zona
    val type: String,        // Tipo de zona: numerada o general
    val rows: Int?,          // Número de filas (null si no hay asientos)
    val seatsPerRow: Int?    // Asientos por fila (null si no hay asientos)
) {
    // Una zona numerada tiene filas y asientos asignados; una general no.
    val isNumbered: Boolean get() = type == TYPE_NUMBERED

    companion object {
        const val TYPE_NUMBERED = "numerada"
        const val TYPE_GENERAL = "general"
    }
}
