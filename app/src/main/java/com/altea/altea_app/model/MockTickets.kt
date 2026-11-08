package com.altea.altea_app.model

// Datos de ejemplo de "Mis Entradas" mientras el servidor no ofrece la consulta.
// Se sustituirán por la respuesta real en una entrega posterior.
object MockTickets {

    fun create(): List<OwnedTicket> = listOf(
        OwnedTicket(
            locator = "ALT-2026-0001",
            eventName = "El Rey León, El Musical",
            eventDate = "Sábado, 20:30h",
            ticketName = "Platea General"
        ),
        OwnedTicket(
            locator = "ALT-2026-0002",
            eventName = "El Rey León, El Musical",
            eventDate = "Sábado, 20:30h",
            ticketName = "VIP Gold Experience"
        ),
        OwnedTicket(
            locator = "ALT-2026-0003",
            eventName = "Monólogos de la Chocita",
            eventDate = "Viernes, 22:00h",
            ticketName = "Entrada Promocional"
        ),
        OwnedTicket(
            locator = "ALT-2026-0004",
            eventName = "Filarmónica: Noche Clásica",
            eventDate = "Domingo, 19:00h",
            ticketName = "Platea General"
        ),
        OwnedTicket(
            locator = "ALT-2026-0005",
            eventName = "La Casa de Bernarda Alba",
            eventDate = "Sábado, 18:00h",
            ticketName = "Entrada Promocional"
        )
    )
}