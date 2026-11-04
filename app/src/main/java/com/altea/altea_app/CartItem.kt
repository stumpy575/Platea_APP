package com.altea.altea_app

data class CartItem(
    val eventName: String,
    val ticketName: String,
    val price: Double,
    val quantity: Int
) {
    // Subtotal de esta línea del carrito
    val subtotal: Double get() = price * quantity
}
