package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class ExitEventDTO(
    val licensePlate: String,
    val exitTime: Instant
)