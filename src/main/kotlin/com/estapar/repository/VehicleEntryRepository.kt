package com.estapar.repository

import com.estapar.model.VehicleEntry
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface VehicleEntryRepository : JpaRepository<VehicleEntry, String>