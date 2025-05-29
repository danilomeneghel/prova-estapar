package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ParkedEventDTO(
    val eventType: String, 
    val licensePlate: String,
    val lat: Double,
    val lng: Double
)