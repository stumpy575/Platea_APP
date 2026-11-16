package com.altea.altea_app.model

// Asiento de una zona numerada
data class Seat(
    val id: Int,        // Identificador del asiento en el servidor
    val row: Int,       // Número de fila
    val number: Int,    // Número de asiento dentro de la fila
    val state: String   // Estado del asiento (libre, vendida...)
)
