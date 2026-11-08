package com.altea.altea_app.model

// Entrada comprada que se muestra en la pantalla "Mis Entradas"
data class OwnedTicket(
    val locator: String,
    val eventName: String,
    val eventDate: String,
    val ticketName: String
)