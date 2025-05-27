package com.estapar.repository

import com.estapar.model.Revenue
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.time.LocalDate

@Repository
interface RevenueRepository : JpaRepository<Revenue, Long> {
    fun findByDateAndSectorName(date: LocalDate, sectorName: String): Revenue?
}