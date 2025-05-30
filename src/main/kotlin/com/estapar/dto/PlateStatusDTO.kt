package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class PlateStatusDTO(
    val licensePlate: String?,
    val priceUntilNow: Double,
    val entryTime: Instant?,
    val timeParked: Instant?,
    val lat: Double?,
    val lng: Double?
)