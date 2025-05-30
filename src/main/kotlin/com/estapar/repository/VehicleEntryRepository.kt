package com.estapar.repository

import com.estapar.model.Spot
import com.estapar.model.VehicleEntry
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.Optional

@Repository
interface VehicleEntryRepository : JpaRepository<VehicleEntry, String> {
    fun findBySpotAndStatus(spot: Spot, status: String): Optional<VehicleEntry>
}