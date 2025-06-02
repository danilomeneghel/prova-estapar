package com.estapar.repository

import com.estapar.model.Garage
import com.estapar.model.Sector
import com.estapar.model.Spot
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@MicronautTest
class GarageRepositoryTest {

    @Inject
    lateinit var garageRepository: GarageRepository

    @Inject
    lateinit var spotRepository: SpotRepository

    @Inject
    lateinit var sectorRepository: SectorRepository

    @BeforeEach
    fun setup() {
        garageRepository.deleteAll()
        spotRepository.deleteAll()
        sectorRepository.deleteAll()
    }

    @Test
    fun `should save and find garage entry`() {
        val entryTime = Instant.now()
        val garage = Garage(licensePlate = "ABC-1234", entryTime = entryTime)
        val savedGarage = garageRepository.save(garage)

        assertNotNull(savedGarage.licensePlate)
        assertEquals("ABC-1234", savedGarage.licensePlate)
        assertEquals(entryTime, savedGarage.entryTime)

        val foundGarage = garageRepository.findById("ABC-1234").orElse(null)
        assertNotNull(foundGarage)
        assertEquals("ABC-1234", foundGarage.licensePlate)
        assertEquals(entryTime, foundGarage.entryTime)
    }

    @Test
    fun `should update garage entry status and spot`() {
        val sector = Sector(name = "A", basePrice = 10.0, maxCapacity = 10, openHour = "08:00", closeHour = "20:00", durationLimitMinutes = 60)
        val savedSector = sectorRepository.save(sector)

        val spot = Spot(id = 1L, lat = -23.5, lng = -46.6, sector = savedSector)
        val savedSpot = spotRepository.save(spot)

        val entryTime = Instant.now().minusSeconds(3600)
        val garage = Garage(licensePlate = "XYZ-5678", entryTime = entryTime)
        val savedGarage = garageRepository.save(garage)

        savedGarage.status = "PARKED"
        savedGarage.spot = savedSpot
        savedGarage.parkedTime = Instant.now()
        val updatedGarage = garageRepository.update(savedGarage)

        assertNotNull(updatedGarage)
        assertEquals("PARKED", updatedGarage.status)
        assertNotNull(updatedGarage.spot)
        assertEquals(savedSpot.id, updatedGarage.spot!!.id)
        assertNotNull(updatedGarage.parkedTime)

        val foundGarage = garageRepository.findById("XYZ-5678").orElse(null)
        assertNotNull(foundGarage)
        assertEquals("PARKED", foundGarage.status)
        assertNotNull(foundGarage.spot)
        assertEquals(savedSpot.id, foundGarage.spot!!.id)
        assertNotNull(foundGarage.parkedTime)
    }

    @Test
    fun `should find by spot and status`() {
        val sector = Sector(name = "B", basePrice = 12.0, maxCapacity = 5, openHour = "09:00", closeHour = "18:00", durationLimitMinutes = 90)
        val savedSector = sectorRepository.save(sector)

        val spot1 = Spot(id = 10L, lat = -23.51, lng = -46.61, sector = savedSector, ocupied = true)
        val savedSpot1 = spotRepository.save(spot1)

        val spot2 = Spot(id = 11L, lat = -23.52, lng = -46.62, sector = savedSector, ocupied = false)
        val savedSpot2 = spotRepository.save(spot2)

        val garage1 = Garage(licensePlate = "TEST-001", entryTime = Instant.now(), spot = savedSpot1, status = "PARKED")
        garageRepository.save(garage1)

        val garage2 = Garage(licensePlate = "TEST-002", entryTime = Instant.now(), status = "ENTRY")
        garageRepository.save(garage2)

        val foundParkedGarage = garageRepository.findBySpotAndStatus(savedSpot1, "PARKED").orElse(null)
        assertNotNull(foundParkedGarage)
        assertEquals("TEST-001", foundParkedGarage.licensePlate)
        assertEquals(savedSpot1.id, foundParkedGarage.spot!!.id)
        assertEquals("PARKED", foundParkedGarage.status)

        val foundNonParkedGarage = garageRepository.findBySpotAndStatus(savedSpot2, "PARKED").orElse(null)
        assertNull(foundNonParkedGarage)
    }

    @Test
    fun `should delete garage entry`() {
        val garage = Garage(licensePlate = "DEL-9999", entryTime = Instant.now())
        garageRepository.save(garage)

        var foundGarage = garageRepository.findById("DEL-9999").orElse(null)
        assertNotNull(foundGarage)

        garageRepository.delete(foundGarage)

        foundGarage = garageRepository.findById("DEL-9999").orElse(null)
        assertNull(foundGarage)
    }
}