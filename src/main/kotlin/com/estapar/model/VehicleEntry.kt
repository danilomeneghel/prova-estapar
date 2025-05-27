package com.estapar.model

import javax.persistence.*
import java.time.Instant

@Entity
class VehicleEntry(
    @Id
    val licensePlate: String,
    val entryTime: Instant,
    var parkedTime: Instant? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    var spot: Spot? = null
) {
    constructor() : this("", Instant.EPOCH, null, null)
}