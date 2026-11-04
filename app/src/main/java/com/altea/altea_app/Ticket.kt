package com.altea.altea_app

// Tipo de entrada de un evento con su precio y las entradas seleccionadas
data class Ticket (
    val id: String,        // Identificador del tipo de entrada en el servidor
    val name: String,      // Nombre del tipo de entrada (Platea, Dempeus, VIP...)
    val price: Double,     // Precio unitario de la entrada
    var quantity: Int = 0  // Entradas seleccionadas por el usuario
)
