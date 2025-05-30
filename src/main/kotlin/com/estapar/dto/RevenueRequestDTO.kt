package com.estapar.dto

import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDate

@Serdeable
data class RevenueRequestDTO(
    val date: LocalDate,
    val sector: String
)