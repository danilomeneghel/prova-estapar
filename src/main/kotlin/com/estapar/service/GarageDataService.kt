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
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.time.Instant
import org.slf4j.LoggerFactory
import java.net.URL
import java.lang.Thread

@Singleton
open class GarageDataService(
    private val garageRepository: GarageRepository,
    private val spotRepository: SpotRepository,
    private val sectorRepository: SectorRepository,
    @Value("\${garage.simulator.url}") private val simulatorUrl: String
) {
    private val httpClient: HttpClient = HttpClient.create(URL(simulatorUrl))
    private val LOG = LoggerFactory.getLogger(GarageDataService::class.java)

    init {
        LOG.info("HttpClient initialized with URL: $simulatorUrl")
    }

    @EventListener
    fun onStartup(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
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

    @Transactional
    open fun fetchAndSaveGarageData() {
        try {
            val request = HttpRequest.GET<Any>("/garage")
            val garageInfoDTO: GarageInfoDTO = httpClient.toBlocking().retrieve(request, GarageInfoDTO::class.java)

            garageInfoDTO.garage.forEach { sectorInfo ->
                val existingSector: Sector? = sectorRepository.findByName(sectorInfo.sector)
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
                    LOG.info("Saved new sector: ${newSector.name}")
                } else {
                    LOG.info("Sector ${existingSector.name} already exists. Skipping save.")
                }
            }

            garageInfoDTO.spots.forEach { spotInfo ->
                val spotId = spotInfo.id ?: throw IllegalStateException("Spot ID cannot be null from simulator data.")

                val existingSpotOptional = spotRepository.findById(spotId)
                var spot: Spot
                var isNewSpot = false

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
                } else {
                    spot = existingSpotOptional.get()
                    if (spot.lat != spotInfo.lat || spot.lng != spotInfo.lng || spot.ocupied != spotInfo.occupied || spot.sector.name != spotInfo.sector) {
                        spot.lat = spotInfo.lat
                        spot.lng = spotInfo.lng
                        spot.ocupied = spotInfo.occupied
                        val sector: Sector = sectorRepository.findByName(spotInfo.sector)
                            ?: throw IllegalStateException("Sector ${spotInfo.sector} not found for spot ID: ${spotId} during spot update.")
                        spot.sector = sector
                    } else {
                        LOG.info("Spot ${spot.id} already exists and is up-to-date. Skipping update.")
                    }
                }

                val savedSpot = spotRepository.save(spot)
                if (isNewSpot) {
                    LOG.info("Saved new spot: ${savedSpot.id}")
                } else {
                    LOG.info("Updated existing spot: ${savedSpot.id}")
                }

                if (savedSpot.ocupied) {
                    val existingGarageEntry = garageRepository.findBySpotAndStatus(savedSpot, "ENTRY")
                    if (existingGarageEntry.isEmpty) {
                        val garage = Garage(
                            licensePlate = "SIMULATED-${savedSpot.id}",
                            entryTime = Instant.now(),
                            spot = savedSpot,
                            status = "ENTRY"
                        )
                        garageRepository.save(garage)
                        LOG.info("Saved new garage entry for spot ID: ${savedSpot.id}, License Plate: ${garage.licensePlate}")
                    } else {
                        LOG.info("Garage entry for spot ID: ${savedSpot.id} with status 'ENTRY' already exists. Skipping save.")
                    }
                } else {
                    val existingGarageEntry = garageRepository.findBySpotAndStatus(savedSpot, "ENTRY")
                    if (existingGarageEntry.isPresent) {
                        val garageToExit = existingGarageEntry.get()
                        garageToExit.exitTime = Instant.now()
                        garageToExit.status = "EXIT"
                        garageToExit.spot = null
                        garageRepository.update(garageToExit)
                        LOG.info("Marked existing garage entry for spot ID: ${savedSpot.id} as EXIT (Simulator now shows unoccupied).")
                    } else {
                        LOG.info("Spot ${savedSpot.id} is unoccupied, and no active 'ENTRY' record found. Skipping exit processing.")
                    }
                }
            }
            LOG.info("Successfully fetched and processed garage data.")

        } catch (e: Exception) {
            LOG.error("Error fetching or saving garage data: ${e.message}", e)
            throw e
        } finally {
            httpClient.close()
        }
    }
}