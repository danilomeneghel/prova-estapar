package com.estapar.model

import com.fasterxml.jackson.annotation.JsonFormat
import javax.persistence.*
import java.time.LocalDate

@Entity
class Revenue(
    @Id
    @GeneratedValue
    val id: Long? = null,
    var sectorName: String,
    var amount: Double,
    @JsonFormat(pattern = "yyyy-MM-dd")
    var date: LocalDate
) {
    constructor() : this(null, "", 0.0, LocalDate.now())
}