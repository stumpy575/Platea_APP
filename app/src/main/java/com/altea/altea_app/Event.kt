package com.altea.altea_app

data class Event (
    val id: String,
    val name: String,
    val date: String,
    var price: Double,
    val img: String,
    val description: String,
    var tickets: List<Ticket>
)
