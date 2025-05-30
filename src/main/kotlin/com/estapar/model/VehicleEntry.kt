package com.estapar.model

import jakarta.persistence.*
import java.time.Instant

@Entity
data class VehicleEntry(
    @Id
    var licensePlate: String,
    val entryTime: Instant,
    var parkedTime: Instant? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spot_id")
    var spot: Spot? = null,
    var exitTime: Instant? = null,
    var status: String = "ENTRY"
) {
    constructor() : this("", Instant.EPOCH, null, null, null, "ENTRY")
}