package com.altea.altea_app.model

// Línea del carrito: una entrada seleccionada con su evento, precio y cantidad
data class CartItem(
    val eventoId: String,      // Identificador del evento en el servidor
    val tipoEntradaId: String, // Identificador del tipo de entrada en el servidor
    val eventName: String,     // Nombre del evento
    val ticketName: String,    // Nombre del tipo de entrada
    val price: Double,         // Precio unitario de la entrada
    val quantity: Int,         // Cantidad de entradas de esta línea
    val asientoId: Int? = null,   // Butaca comprada (solo entradas numeradas)
    val seatLabel: String? = null // Etiqueta de la butaca (F1-2) para mostrarla
) {
    // Subtotal de esta línea del carrito
    val subtotal: Double get() = price * quantity
}
