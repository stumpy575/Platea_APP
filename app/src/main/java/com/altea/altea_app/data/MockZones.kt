package com.altea.altea_app.data

import com.altea.altea_app.model.Zone

// Datos temporales de zonas hasta que el servidor ofrezca el detalle del recinto.
object MockZones {

    private val zones = listOf(
        Zone(
            id = "Z1",
            name = "Platea",
            type = Zone.TYPE_NUMBERED,
            rows = 6,
            seatsPerRow = 10
        ),
        Zone(
            id = "Z2",
            name = "General",
            type = Zone.TYPE_GENERAL,
            rows = null,
            seatsPerRow = null
        )
    )

    // Devuelve la zona con ese id o null si no existe.
    fun getById(id: String): Zone? = zones.firstOrNull { it.id == id }
}
