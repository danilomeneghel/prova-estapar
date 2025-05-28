package com.estapar.model

import jakarta.persistence.*

@Entity
class Spot(
    @Id
    @GeneratedValue
    val id: Long? = null,
    var lat: Double,
    var lng: Double,
    var ocupied: Boolean = false,
    @ManyToOne(fetch = FetchType.EAGER)
    var sector: Sector
) {
    constructor() : this(null, 0.0, 0.0, false, Sector())
}