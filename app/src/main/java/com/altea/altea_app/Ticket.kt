package com.altea.altea_app

data class Ticket (
    val name: String,
    val description: String,
    val price: Double,
    var quantity: Int = 0
)
