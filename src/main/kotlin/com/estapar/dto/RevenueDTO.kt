package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class RevenueDTO(
    val amount: Double,
    val currency: String,
    val timestamp: String
)