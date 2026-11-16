package com.altea.altea_app.model

// Evento de la cartelera con los datos que envía el servidor y sus tipos de entrada
data class Event (
    val id: String,            // Identificador del evento
    val venueId: String,       // Identificador del venue del evento
    val name: String,          // Nombre del evento
    val date: String,          // Fecha y hora tal como las envía el servidor
    val img: String,           // Nombre del archivo de imagen del evento
    val description: String,   // Descripción del evento
    var tickets: List<TicketType> = emptyList(),  // Tipos de entrada disponibles para el evento
    var venue: Venue? = null                      // Venue del evento con sus zonas
) {
    val zones: List<Zone>
        get() = venue?.zones ?: emptyList()

    val minPrice: Double
        get() = tickets.minOfOrNull { it.price } ?: 0.0
}
