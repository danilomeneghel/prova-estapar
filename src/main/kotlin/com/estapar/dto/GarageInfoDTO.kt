package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class SectorInfo(
    val sector: String,
    val basePrice: Double,
    val max_capacity: Int,
    val open_hour: String,
    val close_hour: String,
    val duration_limit_minutes: Int?
)

@Serdeable
data class SpotInfo(
    val id: Long,
    val sector: String,
    val lat: Double,
    val lng: Double
)

@Serdeable
data class GarageInfoDTO(
    val garage: List<SectorInfo>,
    val spots: List<SpotInfo>
)