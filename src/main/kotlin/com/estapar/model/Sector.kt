package com.estapar.model

import jakarta.persistence.*

@Entity
class Sector(
    @Id
    @GeneratedValue
    val id: Long? = null,
    val name: String,
    val basePrice: Double,
    val maxCapacity: Int,
    val openHour: String,
    val closeHour: String,
    val durationLimitMinutes: Int,
    var currentOcupied: Int = 0
) {
    constructor() : this(null, "", 0.0, 0, "", "", 0, 0)
}