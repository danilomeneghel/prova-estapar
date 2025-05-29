package com.estapar.model

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.persistence.*
import java.time.LocalDate

@Entity
data class Revenue(
    @Id
    @GeneratedValue
    var id: Long? = null,
    var sectorName: String,
    var amount: Double,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var date: LocalDate
) {
    constructor() : this(null, "", 0.0, LocalDate.now())
}