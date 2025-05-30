package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class EntryEventDTO(
    val licensePlate: String,
    val entryTime: Instant
)