package com.estapar.service

import com.estapar.model.*
import com.estapar.repository.*
import jakarta.inject.Singleton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.fasterxml.jackson.databind.ObjectMapper
import com.estapar.dto.PlateStatusDTO
import com.estapar.dto.SpotStatusDTO
import com.estapar.dto.RevenueDTO
import com.estapar.dto.GarageInfoDTO
import com.estapar.dto.SectorInfo
import com.estapar.dto.SpotInfo
import io.micronaut.transaction.annotation.Transactional

@Singleton
open class GarageService(
    private val sectorRepo: SectorRepository,
    private val spotRepo: SpotRepository,
    private val entryRepo: VehicleEntryRepository,
    private val revenueRepo: RevenueRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    open fun registerEntry(plate: String, entryTime: Instant) {
        if (entryRepo.existsById(plate)) return
        entryRepo.save(VehicleEntry(plate, entryTime))
    }

    @Transactional
    open fun assignSpot(plate: String, lat: Double, lng: Double) {
        val entry = entryRepo.findById(plate).orElse(null)
        if (entry == null) {
            return
        }

        val spot = spotRepo.findByLatAndLng(lat, lng)

        if (spot == null || spot.ocupied) {
            return
        }

        spot.ocupied = true
        spotRepo.save(spot)

        entry.parkedTime = Instant.now()
        entry.spot = spot
        entryRepo.save(entry)

        val sector = spot.sector
        if (sector.currentOcupied < sector.maxCapacity) {
            sector.currentOcupied++
            sectorRepo.save(sector)
        }
    }

    @Transactional
    open fun handleExit(plate: String, exitTime: Instant) {
        val entry = entryRepo.findById(plate).orElse(null) ?: return
        val spot = entry.spot ?: return
        val basePrice = spot.sector.basePrice
        val lotPercent = spot.sector.currentOcupied.toDouble() / spot.sector.maxCapacity
        val price = when {
            lotPercent < 0.25 -> basePrice * 0.9
            lotPercent < 0.5 -> basePrice
            lotPercent < 0.75 -> basePrice * 1.1
            else -> basePrice * 1.25
        }
        val total = price
        val date = LocalDate.ofInstant(exitTime, ZoneId.systemDefault())
        val revenue = revenueRepo.findByDateAndSectorName(date, spot.sector.name)

        if (revenue == null) {
            revenueRepo.save(Revenue(date = date, sectorName = spot.sector.name, amount = total))
        } else {
            revenue.amount += total
            revenueRepo.update(revenue)
        }

        spot.ocupied = false
        spotRepo.update(spot)
        spot.sector.currentOcupied--
        sectorRepo.update(spot.sector)
        entryRepo.deleteById(plate)
    }

    fun postPlateStatus(licensePlate: String): PlateStatusDTO {
        val entry = entryRepo.findById(licensePlate).orElse(null)
        val now = Instant.now()
        val duration = if (entry != null) ((now.toEpochMilli() - (entry.parkedTime ?: entry.entryTime).toEpochMilli()) / 60000).toInt() else 0
        val basePrice = entry?.spot?.sector?.basePrice ?: 0.0
        val price = basePrice * (duration / 60.0)

        val map = mapOf(
            "licensePlate" to entry?.licensePlate,
            "priceUntilNow" to String.format("%.2f", price).toDouble(),
            "entryTime" to entry?.entryTime,
            "timeParked" to entry?.parkedTime,
            "lat" to entry?.spot?.lat,
            "lng" to entry?.spot?.lng
        )
        return objectMapper.convertValue(map, PlateStatusDTO::class.java)
    }

    fun postSpotStatus(lat: Double, lng: Double): SpotStatusDTO {
        val spot = spotRepo.findByLatAndLng(lat, lng)
        val entry = spot?.let { s -> entryRepo.findAll().find { it.spot?.id == s.id } }
        val now = Instant.now()
        val duration = if (entry != null) ((now.toEpochMilli() - (entry.parkedTime ?: entry.entryTime).toEpochMilli()) / 60000).toInt() else 0
        val basePrice = spot?.sector?.basePrice ?: 0.0
        val price = basePrice * (duration / 60.0)

        val map = mapOf(
            "ocupied" to (spot?.ocupied ?: false),
            "licensePlate" to entry?.licensePlate,
            "priceUntilNow" to String.format("%.2f", price).toDouble(),
            "entryTime" to entry?.entryTime,
            "timeParked" to entry?.parkedTime
        )
        return objectMapper.convertValue(map, SpotStatusDTO::class.java)
    }

    fun getRevenue(date: LocalDate, sector: String): RevenueDTO {
        val revenue = revenueRepo.findByDateAndSectorName(date, sector)

        val map = mapOf(
            "amount" to (revenue?.amount ?: 0.0),
            "currency" to "BRL",
            "timestamp" to date.atStartOfDay(ZoneId.systemDefault()).toString()
        )
        return objectMapper.convertValue(map, RevenueDTO::class.java)
    }

    fun getGarage(): GarageInfoDTO {
        val sectors = sectorRepo.findAll().map {
            mapOf(
                "sector" to it.name,
                "basePrice" to it.basePrice,
                "max_capacity" to it.maxCapacity,
                "open_hour" to it.openHour.toString(),
                "close_hour" to it.closeHour.toString(),
                "duration_limit_minutes" to it.durationLimitMinutes
            )
        }

        val spots = spotRepo.findAll().map {
            mapOf(
                "id" to it.id,
                "sector" to it.sector.name,
                "lat" to it.lat,
                "lng" to it.lng
            )
        }

        val garageMap = mapOf(
            "garage" to sectors,
            "spots" to spots
        )
        return objectMapper.convertValue(garageMap, GarageInfoDTO::class.java)
    }
}