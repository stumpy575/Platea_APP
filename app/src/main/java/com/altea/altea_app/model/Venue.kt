package com.altea.altea_app.model

// Venue del catálogo con sus zonas
data class Venue(
    val id: String,          // Identificador del venue (R1, R2...)
    val name: String,        // Nombre visible del venue
    val zones: List<Zone>    // Zonas que pertenecen al venue
)
