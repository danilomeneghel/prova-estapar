package com.estapar.service

import com.estapar.model.*
import com.estapar.repository.*
import jakarta.inject.Singleton
import java.time.Instant
import java.time.LocalDate

@Singleton
class GarageService(
    private val sectorRepo: SectorRepository,
    private val spotRepo: SpotRepository,
    private val entryRepo: VehicleEntryRepository,
    private val revenueRepo: RevenueRepository
) {
    fun registerEntry(plate: String, entryTime: Instant) {
        if (entryRepo.existsById(plate)) return
        entryRepo.save(VehicleEntry(plate, entryTime))
    }

    fun assignSpot(plate: String, lat: Double, lng: Double) {
        val entry = entryRepo.findById(plate).orElse(null) ?: return
        val spot = spotRepo.findByLatAndLng(lat, lng) ?: return
        if (spot.ocupied) return
        spot.ocupied = true
        entry.parkedTime = Instant.now()
        entry.spot = spot
        val sector = spot.sector
        if (sector.currentOcupied < sector.maxCapacity) sector.currentOcupied++
        spotRepo.update(spot)
        sectorRepo.update(sector)
        entryRepo.update(entry)
    }

    fun handleExit(plate: String, exitTime: Instant) {
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
        val date = LocalDate.ofInstant(exitTime, java.time.ZoneId.systemDefault())
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

    fun postPlateStatus(licensePlate: String): Map<String, Any?> {
        val entry = entryRepo.findById(licensePlate).orElse(null) ?: return emptyMap()
        val now = Instant.now()
        val duration = ((now.toEpochMilli() - (entry.parkedTime ?: entry.entryTime).toEpochMilli()) / 60000).toInt()
        val basePrice = entry.spot?.sector?.basePrice ?: 0.0
        val price = basePrice * (duration / 60.0)

        return mapOf(
            "license_plate" to entry.licensePlate,
            "price_until_now" to String.format("%.2f", price).toDouble(),
            "entry_time" to entry.entryTime,
            "time_parked" to entry.parkedTime,
            "lat" to entry.spot?.lat,
            "lng" to entry.spot?.lng
        )
    }

    fun postSpotStatus(lat: Double, lng: Double): Map<String, Any?> {
        val spot = spotRepo.findByLatAndLng(lat, lng) ?: return emptyMap()
        val entry = entryRepo.findAll().find { it.spot?.id == spot.id }
        val now = Instant.now()
        val duration = if (entry != null) ((now.toEpochMilli() - (entry.parkedTime ?: entry.entryTime).toEpochMilli()) / 60000).toInt() else 0
        val basePrice = spot.sector.basePrice
        val price = basePrice * (duration / 60.0)

        return mapOf(
            "ocupied" to spot.ocupied,
            "license_plate" to entry?.licensePlate,
            "price_until_now" to String.format("%.2f", price).toDouble(),
            "entry_time" to entry?.entryTime,
            "time_parked" to entry?.parkedTime
        )
    }

    fun getRevenue(date: LocalDate, sector: String): Map<String, Any> {
        val revenue = revenueRepo.findByDateAndSectorName(date, sector)

        return mapOf(
            "amount" to (revenue?.amount ?: 0.0),
            "currency" to "BRL",
            "timestamp" to date.atStartOfDay().toString()
        )
    }

    fun getGarage(): Map<String, Any> {
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

        return mapOf(
            "garage" to sectors,
            "spots" to spots
        )
    }
}