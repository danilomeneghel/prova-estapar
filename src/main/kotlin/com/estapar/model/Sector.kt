package com.estapar.model

import jakarta.persistence.*

@Entity
data class Sector(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    val name: String,
    var basePrice: Double,
    var maxCapacity: Int,
    var openHour: String,
    var closeHour: String,
    var durationLimitMinutes: Int,
    var currentOcupied: Int = 0
) {
    constructor() : this(null, "", 0.0, 0, "", "", 0, 0)
}