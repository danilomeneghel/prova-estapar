package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class SpotStatusDTO(
    val ocupied: Boolean,
    val licensePlate: String?,
    val priceUntilNow: Double,
    val entryTime: Instant?,
    val timeParked: Instant?
)