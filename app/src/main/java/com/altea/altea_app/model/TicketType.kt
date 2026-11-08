package com.altea.altea_app.model

// Tipo de entrada de un evento con su precio y las entradas seleccionadas
data class TicketType (
    val id: String,        // Identificador del tipo de entrada en el servidor
    val name: String,      // Nombre del tipo de entrada (Platea, Dempeus, VIP...)
    val price: Double      // Precio unitario de la entrada
) {
    var quantity: Int = 0
}
