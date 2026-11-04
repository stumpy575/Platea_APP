package com.altea.altea_app

// Línea del carrito: una entrada seleccionada con su evento, precio y cantidad
data class CartItem(
    val eventName: String,   // Nombre del evento
    val ticketName: String,  // Nombre del tipo de entrada
    val price: Double,       // Precio unitario de la entrada
    val quantity: Int        // Cantidad de entradas de esta línea
) {
    // Subtotal de esta línea del carrito
    val subtotal: Double get() = price * quantity
}
