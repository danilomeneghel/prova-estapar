package com.estapar.service

import com.estapar.dto.GarageInfoDTO
import com.estapar.model.Garage
import com.estapar.model.Sector
import com.estapar.model.Spot
import com.estapar.repository.GarageRepository
import com.estapar.repository.SectorRepository
import com.estapar.repository.SpotRepository
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.time.Instant
import org.slf4j.LoggerFactory
import io.micronaut.scheduling.annotation.Scheduled

@Singleton
class GarageDataService(
    private val garageRepository: GarageRepository,
    private val spotRepository: SpotRepository,
    private val sectorRepository: SectorRepository,
    @Client("\${garage.simulator.url}") private val httpClient: HttpClient,
    private val parkingService: ParkingService
) {
    private val LOG = LoggerFactory.getLogger(GarageDataService::class.java)

    @EventListener
    fun onStartup(event: ServerStartupEvent) {
        LOG.info("Server started, fetching garage data from simulator...")
        try {
            Thread.sleep(5000)
            LOG.info("Waiting 5 seconds for simulator readiness...")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            LOG.error("Thread sleep interrupted", e)
        }
        fetchAndSaveGarageData()
    }

    @Scheduled(fixedDelay = "2s")
    @Transactional
    fun fetchAndSaveGarageData() {
        try {
            val request = HttpRequest.GET<Any>("/garage")
            val garageInfoDTO: GarageInfoDTO = httpClient.toBlocking().retrieve(request, GarageInfoDTO::class.java)

            garageInfoDTO.garage.forEach { sectorInfo ->
                val existingSector: Sector? = sectorRepository.findByName(sectorInfo.sector)
                var sectorNeedsUpdate = false

                if (existingSector == null) {
                    val newSector = Sector(
                        name = sectorInfo.sector,
                        basePrice = sectorInfo.basePrice,
                        maxCapacity = sectorInfo.maxCapacity,
                        openHour = sectorInfo.openHour,
                        closeHour = sectorInfo.closeHour,
                        durationLimitMinutes = sectorInfo.durationLimitMinutes
                    )
                    sectorRepository.save(newSector)
                    LOG.info("Saved new sector: ${newSector.name} with basePrice: ${newSector.basePrice}")
                } else {
                    if (existingSector.basePrice != sectorInfo.basePrice ||
                        existingSector.maxCapacity != sectorInfo.maxCapacity ||
                        existingSector.openHour != sectorInfo.openHour ||
                        existingSector.closeHour != sectorInfo.closeHour ||
                        existingSector.durationLimitMinutes != sectorInfo.durationLimitMinutes
                    ) {
                        existingSector.basePrice = sectorInfo.basePrice
                        existingSector.maxCapacity = sectorInfo.maxCapacity
                        existingSector.openHour = sectorInfo.openHour
                        existingSector.closeHour = sectorInfo.closeHour
                        existingSector.durationLimitMinutes = sectorInfo.durationLimitMinutes
                        sectorNeedsUpdate = true
                    }
                    if (sectorNeedsUpdate) {
                        sectorRepository.update(existingSector)
                        LOG.info("Updated existing sector: ${existingSector.name} with basePrice: ${existingSector.basePrice}")
                    } else {
                        LOG.debug("Sector ${existingSector.name} already up-to-date. Skipping update.")
                    }
                }
            }

            garageInfoDTO.spots.forEach { spotInfo ->
                val spotId = spotInfo.id ?: throw IllegalStateException("Spot ID cannot be null from simulator data.")

                val existingSpotOptional = spotRepository.findById(spotId)
                var spot: Spot
                var isNewSpot = false
                var needsSpotUpdate = false

                if (existingSpotOptional.isEmpty) {
                    val sector: Sector = sectorRepository.findByName(spotInfo.sector)
                        ?: throw IllegalStateException("Sector ${spotInfo.sector} not found for spot ID: ${spotId} while creating new spot.")

                    spot = Spot(
                        id = spotId,
                        lat = spotInfo.lat,
                        lng = spotInfo.lng,
                        sector = sector,
                        ocupied = spotInfo.occupied
                    )
                    isNewSpot = true
                    needsSpotUpdate = true
                } else {
                    spot = existingSpotOptional.get()
                    val sector: Sector = sectorRepository.findByName(spotInfo.sector)
                        ?: throw IllegalStateException("Sector ${spotInfo.sector} not found for spot ID: ${spotId} during spot update.")

                    if (spot.ocupied != spotInfo.occupied ||
                        spot.lat != spotInfo.lat ||
                        spot.lng != spotInfo.lng ||
                        spot.sector.name != sector.name
                    ) {
                        spot.ocupied = spotInfo.occupied
                        spot.lat = spotInfo.lat
                        spot.lng = spotInfo.lng
                        spot.sector = sector
                        needsSpotUpdate = true
                    }
                }

                if (needsSpotUpdate) {
                    val savedSpot = spotRepository.save(spot)
                    if (isNewSpot) {
                        LOG.info("Saved new spot: ${savedSpot.id} with occupied status: ${savedSpot.ocupied}")
                    } else {
                        LOG.info("Updated existing spot: ${savedSpot.id} with occupied status: ${savedSpot.ocupied}")
                    }

                    if (savedSpot.ocupied) {
                        val licensePlate = "SIMULATED-SPOT-${savedSpot.id}"
                        val existingGarageEntry = garageRepository.findById(licensePlate)

                        if (existingGarageEntry.isEmpty) {
                            val garage = Garage(
                                licensePlate = licensePlate,
                                entryTime = Instant.now(),
                                parkedTime = Instant.now(),
                                spot = savedSpot,
                                status = "PARKED"
                            )
                            garageRepository.save(garage)
                            val sector = savedSpot.sector
                            sector.currentOcupied++
                            sectorRepository.save(sector)
                            LOG.info("Saved new garage entry for spot ID: ${savedSpot.id}, License Plate: ${garage.licensePlate}. Sector occupied count incremented.")
                        } else {
                            LOG.debug("Garage entry for spot ID: ${savedSpot.id} (License Plate: $licensePlate) with status 'PARKED' already exists. Skipping new entry.")
                        }
                    } else {
                        val licensePlate = "SIMULATED-SPOT-${savedSpot.id}"
                        val existingGarageEntry = garageRepository.findById(licensePlate)
                        if (existingGarageEntry.isPresent && existingGarageEntry.get().status == "PARKED") {
                            val garageToExit = existingGarageEntry.get()
                            parkingService.handleExit(garageToExit.licensePlate, Instant.now())
                            LOG.info("Handled exit for garage entry for spot ID: ${savedSpot.id} (Simulator now shows unoccupied).")
                        } else {
                            LOG.debug("Spot ${savedSpot.id} is unoccupied, and no active 'PARKED' record found for this spot. Skipping exit processing.")
                        }
                    }
                } else {
                    LOG.debug("Spot ${spot.id} already exists and is up-to-date. Skipping update.")
                }
            }
            LOG.info("Successfully fetched and processed garage data.")

        } catch (e: Exception) {
            LOG.error("Error fetching or saving garage data: ${e.message}", e)
        }
    }
}