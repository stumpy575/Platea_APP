package com.altea.altea_app

data class Event (
    val name: String,
    val date: String,
    val price: Double,
    val img: String,
    val description: String,
    val tickets: List<Ticket>
)
