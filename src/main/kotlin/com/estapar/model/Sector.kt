package com.estapar.model

import jakarta.persistence.*

@Entity
data class Sector(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
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