package com.estapar.repository

import com.estapar.model.Spot
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface SpotRepository : JpaRepository<Spot, Long> {
    fun findByLatAndLng(lat: Double, lng: Double): Spot?
}