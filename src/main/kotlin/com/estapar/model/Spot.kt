package com.estapar.model

import jakarta.persistence.*

@Entity
data class Spot(
    @Id
    var id: Long,
    var lat: Double,
    var lng: Double,
    var ocupied: Boolean = false,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sector_id")
    var sector: Sector
) {
    constructor() : this(0L, 0.0, 0.0, false, Sector())
}