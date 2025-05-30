package com.estapar.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class SpotStatusDTO(
    val ocupied: Boolean,
    @field:JsonProperty("license_plate")
    val licensePlate: String?,
    @field:JsonProperty("price_until_now")
    val priceUntilNow: Double,
    @field:JsonProperty("entry_time")
    val entryTime: Instant?,
    @field:JsonProperty("time_parked")
    val timeParked: Instant?
)
