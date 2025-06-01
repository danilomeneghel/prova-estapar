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
import jakarta.inject.Singleton
import java.time.Instant
import org.slf4j.LoggerFactory

@Singleton
open class GarageDataService(
    private val garageRepository: GarageRepository,
    private val spotRepository: SpotRepository,
    private val sectorRepository: SectorRepository,
    @Client("/") private val httpClient: HttpClient
) {

    @Value("\${garage.simulator.url}")
    private val simulatorUrl: String = ""

    private val LOG = LoggerFactory.getLogger(GarageDataService::class.java)

    @EventListener
    fun onStartup(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
        LOG.info("Server started, fetching garage data from simulator...")
        fetchAndSaveGarageData()
    }

    private fun fetchAndSaveGarageData() {
        try {
            val urlString = "$simulatorUrl/garage"
            val request = HttpRequest.GET<Any>(urlString)

            val garageInfoDTO: GarageInfoDTO = httpClient.toBlocking().retrieve(request, GarageInfoDTO::class.java)

            garageInfoDTO.garage.forEach { sectorInfo ->
                val existingSector: Sector? = sectorRepository.findByName(sectorInfo.sector)
                var sector: Sector? = existingSector

                if (sector == null) {
                    sector = Sector(
                        name = sectorInfo.sector,
                        basePrice = sectorInfo.basePrice,
                        maxCapacity = sectorInfo.maxCapacity,
                        openHour = sectorInfo.openHour,
                        closeHour = sectorInfo.closeHour,
                        durationLimitMinutes = sectorInfo.durationLimitMinutes
                    )
                    sectorRepository.save(sector)
                    LOG.info("Saved new sector: ${sector.name}")
                } else {
                    LOG.info("Sector ${sector.name} already exists. Skipping save.")
                }
            }

            garageInfoDTO.spots.forEach { spotInfo ->
                val existingSpot = spotRepository.findById(spotInfo.id!!)
                var spot: Spot? = existingSpot.orElse(null)

                if (spot == null) {
                    val sector: Sector? = sectorRepository.findByName(spotInfo.sector)
                    if (sector == null) {
                        throw IllegalStateException("Sector ${spotInfo.sector} not found for spot ID: ${spotInfo.id}")
                    }

                    spot = Spot(
                        id = spotInfo.id,
                        lat = spotInfo.lat,
                        lng = spotInfo.lng,
                        sector = sector
                    )
                    spotRepository.save(spot)
                    LOG.info("Saved new spot: ${spot.id}")
                } else {
                    LOG.info("Spot ${spot.id} already exists. Skipping save.")
                }

                val existingGarageEntry = garageRepository.findBySpotAndStatus(spot, "ENTRY")

                if (existingGarageEntry.isEmpty) {
                    val garage = Garage(
                        licensePlate = "SIMULATED-${spot.id}",
                        entryTime = Instant.now(),
                        spot = spot,
                        status = "ENTRY"
                    )
                    garageRepository.save(garage)
                    LOG.info("Saved new garage entry for spot ID: ${spot.id}")
                } else {
                    LOG.info("Garage entry for spot ID: ${spot.id} with status 'ENTRY' already exists. Skipping save.")
                }
            }
            LOG.info("Successfully fetched and processed garage data.")
        } catch (e: Exception) {
            LOG.error("Error fetching or saving garage data: ${e.message}", e)
        }
    }
}