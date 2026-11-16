package com.altea.altea_app.model

// Asiento de una zona numerada
data class Seat(
    val id: Int,        // Identificador del asiento en el servidor
    val row: Int,       // Número de fila
    val number: Int,    // Número de asiento dentro de la fila
    val state: String   // Estado del asiento (libre, vendida...)
) {
    // Solo se puede elegir una butaca si el servidor la da como libre
    val isAvailable: Boolean get() = state == STATE_FREE

    companion object {
        const val STATE_FREE = "libre"

        // Etiqueta visible de una butaca, por ejemplo F2-5
        fun label(row: Int, number: Int): String = "F$row-$number"
    }
}
