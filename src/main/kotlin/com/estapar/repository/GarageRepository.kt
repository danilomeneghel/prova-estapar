package com.estapar.repository

import com.estapar.model.Spot
import com.estapar.model.Garage
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.Optional

@Repository
interface GarageRepository : JpaRepository<Garage, String> {
    fun findBySpotAndStatus(spot: Spot, status: String): Optional<Garage>
}