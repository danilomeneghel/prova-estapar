package com.estapar.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SectorInfo(
    val sector: String,
    val basePrice: Double,
    @field:JsonProperty("max_capacity")
    val maxCapacity: Int,
    @field:JsonProperty("open_hour")
    val openHour: String,
    @field:JsonProperty("close_hour")
    val closeHour: String,
    @field:JsonProperty("duration_limit_minutes")
    val durationLimitMinutes: Int
)

@Serdeable
data class SpotInfo(
    val id: Long?,
    val sector: String,
    val lat: Double,
    val lng: Double
)

@Serdeable
data class GarageInfoDTO(
    val garage: List<SectorInfo>,
    val spots: List<SpotInfo>
)