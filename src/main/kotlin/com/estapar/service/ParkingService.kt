package com.estapar.service

import com.estapar.dto.*
import com.estapar.model.Garage
import com.estapar.model.Revenue
import com.estapar.repository.GarageRepository
import com.estapar.repository.RevenueRepository
import com.estapar.repository.SectorRepository
import com.estapar.repository.SpotRepository
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Singleton
open class ParkingService(
    private val sectorRepo: SectorRepository,
    private val spotRepo: SpotRepository,
    private val garageRepo: GarageRepository,
    private val revenueRepo: RevenueRepository,
) {
    private val LOG = LoggerFactory.getLogger(ParkingService::class.java)

    @Transactional
    open fun registerEntry(plate: String, entryTime: Instant) {
        if (garageRepo.existsById(plate)) {
            LOG.warn("ParkingService: Vehicle entry for plate {} already exists. Skipping registration.", plate)
            return
        }
        val newEntry = Garage(plate, entryTime)
        garageRepo.save(newEntry)
        LOG.info("ParkingService: Registered new vehicle entry for plate {}. EntryTime: {}", plate, entryTime)
    }

    @Transactional
    open fun assignSpot(plate: String, lat: Double, lng: Double) {
        LOG.info("ParkingService: Attempting to assign spot for plate: {}, lat: {}, lng: {}", plate, lat, lng)

        val entry = garageRepo.findById(plate).orElse(null)
        if (entry == null) {
            LOG.warn("ParkingService: Vehicle entry not found for plate: {}. Cannot assign spot.", plate)
            return
        }
        LOG.debug("ParkingService: Found Garage for plate: {}", plate)

        val spot = spotRepo.findByLatAndLng(lat, lng)

        if (spot == null) {
            LOG.warn("ParkingService: Spot not found for lat: {}, lng: {}. Cannot assign spot. Check if initial garage data (spots) is loaded.", lat, lng)
            return
        }
        LOG.debug("ParkingService: Found Spot ID: {} (currently occupied: {})", spot.id, spot.ocupied)

        val sector = spot.sector
        if (sector.currentOcupied >= sector.maxCapacity) {
            LOG.warn("ParkingService: Sector {} is at max capacity ({}). Cannot assign spot to plate {}.", sector.name, sector.maxCapacity, plate)
            return
        }

        if (spot.ocupied) {
            LOG.warn("ParkingService: Spot {} is already occupied. Cannot assign spot to plate {}.", spot.id, plate)
            return
        }

        spot.ocupied = true
        val savedSpot = spotRepo.save(spot)
        LOG.info("ParkingService: Spot {} marked as occupied. Lat: {}, Lng: {}.", savedSpot.id, savedSpot.lat, savedSpot.lng)

        entry.parkedTime = Instant.now()
        entry.spot = savedSpot
        entry.status = "PARKED"
        val savedEntry = garageRepo.save(entry)
        LOG.info("ParkingService: Vehicle entry for plate {} updated. ParkedTime: {}, Spot ID: {}. Status: {}", plate, savedEntry.parkedTime, savedEntry.spot?.id, savedEntry.status)

        sector.currentOcupied++
        val savedSector = sectorRepo.save(sector)
        LOG.info("ParkingService: Sector {} current occupied count incremented to {}.", savedSector.name, savedSector.currentOcupied)
    }

    @Transactional
    open fun handleExit(plate: String, exitTime: Instant) {
        LOG.info("ParkingService: Attempting to handle exit for plate: {}. ExitTime: {}", plate, exitTime)

        if (plate.isBlank()) {
            LOG.info("ParkingService: Received empty plate. Skipping exit processing.")
            return
        }

        val entry = garageRepo.findById(plate).orElse(null)
        if (entry == null) {
            LOG.warn("ParkingService: Vehicle entry not found for plate: {}. Cannot process exit.", plate)
            return
        }
        LOG.debug("ParkingService: Found Garage for plate: {}", plate)

        val spot = entry.spot

        if (spot == null) {
            LOG.warn("ParkingService: Spot not associated with vehicle entry for plate: {}. Cannot calculate price or release spot. Still updating entry status.", plate)
        } else {
            val sector = spot.sector
            val basePrice = sector.basePrice

            val parkedDurationMinutes = if (entry.parkedTime != null) {
                (exitTime.toEpochMilli() - entry.parkedTime!!.toEpochMilli()) / 60000.0
            } else {
                (exitTime.toEpochMilli() - entry.entryTime.toEpochMilli()) / 60000.0
            }
            LOG.debug("ParkingService: Parked duration for plate {} is {} minutes.", plate, parkedDurationMinutes)

            val pricePerHour = basePrice
            val totalCalculatedPrice = (parkedDurationMinutes / 60.0) * pricePerHour

            val lotPercent = sector.currentOcupied.toDouble() / sector.maxCapacity
            val totalRevenueAmount = when {
                lotPercent < 0.25 -> totalCalculatedPrice * 0.9
                lotPercent < 0.5 -> totalCalculatedPrice
                lotPercent < 0.75 -> totalCalculatedPrice * 1.1
                else -> totalCalculatedPrice * 1.25
            }
            LOG.info("ParkingService: Calculated final revenue amount for plate {}: {:.2f}", plate, totalRevenueAmount)

            val date = LocalDate.ofInstant(exitTime, ZoneId.systemDefault())
            var revenue = revenueRepo.findByDateAndSectorName(date, sector.name)

            if (revenue == null) {
                revenue = Revenue(date = date, sectorName = sector.name, amount = totalRevenueAmount)
                revenueRepo.save(revenue)
                LOG.info("ParkingService: Created new revenue entry for date {} and sector {}. Amount: {:.2f}", date, sector.name, totalRevenueAmount)
            } else {
                revenue.amount += totalRevenueAmount
                revenueRepo.save(revenue)
                LOG.info("ParkingService: Updated existing revenue for date {} and sector {}. New amount: {:.2f}", date, sector.name, revenue.amount)
            }

            spot.ocupied = false
            spotRepo.save(spot)
            LOG.info("ParkingService: Spot {} marked as unoccupied.", spot.id)

            sector.currentOcupied--
            sectorRepo.save(sector)
            LOG.info("ParkingService: Sector {} current occupied count decremented to {}.", sector.name, sector.currentOcupied)
        }

        entry.exitTime = exitTime
        entry.status = "EXIT"
        entry.spot = null
        garageRepo.save(entry)
        LOG.info("ParkingService: Vehicle entry for plate {} updated to exited status.", plate)
    }

    fun postPlateStatus(licensePlate: String): PlateStatusDTO {
        val entry = garageRepo.findById(licensePlate).orElse(null)
        val now = Instant.now()
        val duration = if (entry != null) ((now.toEpochMilli() - (entry.parkedTime ?: entry.entryTime).toEpochMilli()) / 60000.0) else 0.0
        val basePrice = entry?.spot?.sector?.basePrice ?: 0.0
        val price = basePrice * (duration / 60.0)

        return PlateStatusDTO(
            licensePlate = entry?.licensePlate,
            priceUntilNow = String.format("%.2f", price).toDouble(),
            entryTime = entry?.entryTime,
            timeParked = entry?.parkedTime,
            lat = entry?.spot?.lat,
            lng = entry?.spot?.lng
        )
    }

    fun postSpotStatus(lat: Double, lng: Double): SpotStatusDTO {
        val spot = spotRepo.findByLatAndLng(lat, lng)
        val entry = spot?.let { s -> garageRepo.findBySpotAndStatus(s, "PARKED").orElse(null) }
        val now = Instant.now()

        val duration = if (entry != null) ((now.toEpochMilli() - (entry.parkedTime ?: entry.entryTime).toEpochMilli()) / 60000.0) else 0.0
        val basePrice = spot?.sector?.basePrice ?: 0.0
        val price = basePrice * (duration / 60.0)

        return SpotStatusDTO(
            ocupied = spot?.ocupied ?: false,
            licensePlate = entry?.licensePlate,
            priceUntilNow = String.format("%.2f", price).toDouble(),
            entryTime = entry?.entryTime,
            timeParked = entry?.parkedTime
        )
    }

    fun getRevenue(date: LocalDate, sector: String): RevenueDTO {
        val revenue = revenueRepo.findByDateAndSectorName(date, sector)

        return RevenueDTO(
            amount = revenue?.amount ?: 0.0,
            currency = "BRL",
            timestamp = date.atStartOfDay(ZoneId.systemDefault()).toString()
        )
    }

    fun getGarage(): GarageInfoDTO {
        val sectors = sectorRepo.findAll().map {
            SectorInfo(
                sector = it.name,
                basePrice = it.basePrice,
                maxCapacity = it.maxCapacity,
                openHour = it.openHour,
                closeHour = it.closeHour,
                durationLimitMinutes = it.durationLimitMinutes
            )
        }

        val spots = spotRepo.findAll().map {
            SpotInfo(
                id = it.id,
                sector = it.sector.name,
                lat = it.lat,
                lng = it.lng,
                occupied = it.ocupied
            )
        }

        return GarageInfoDTO(garage = sectors, spots = spots)
    }

}