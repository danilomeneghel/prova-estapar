package com.estapar.repository

import com.estapar.model.Sector
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface SectorRepository : JpaRepository<Sector, Long>