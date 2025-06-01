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

@MicronautTest(environments = ["test"])
class GarageRepositoryTest {

    @Inject
    lateinit var garageRepository: GarageRepository

    @Inject
    lateinit var spotRepository: SpotRepository
    @Inject
    lateinit var sectorRepository: SectorRepository

    private lateinit var testSector: Sector
    private lateinit var testSpot: Spot

    @BeforeEach
    fun setup() {
        garageRepository.deleteAll()
        spotRepository.deleteAll()
        sectorRepository.deleteAll()

        testSector = Sector(name = "Test Sector", basePrice = 5.0, maxCapacity = 10, openHour = "06:00", closeHour = "22:00", durationLimitMinutes = 180)
        testSector = sectorRepository.save(testSector)

        testSpot = Spot(id = 1L, lat = 10.0, lng = 20.0, ocupied = false, sector = testSector)
        testSpot = spotRepository.save(testSpot)
    }

    @Test
    fun findByIdShouldReturnOptionalOfVehicleEntryWhenFound() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()
        val expectedEntry = Garage(licensePlate = licensePlate, entryTime = entryTime)
        garageRepository.save(expectedEntry)

        val result = garageRepository.findById(licensePlate)

        assertTrue(result.isPresent)
        assertEquals(licensePlate, result.get().licensePlate)
        assertEquals(entryTime.epochSecond, result.get().entryTime.epochSecond)
    }

    @Test
    fun saveShouldPersistANewVehicleEntry() {
        val newEntry = Garage(licensePlate = "NEW4567", entryTime = Instant.now())

        val result = garageRepository.save(newEntry)

        assertNotNull(result.licensePlate)
        assertEquals(newEntry.licensePlate, result.licensePlate)
        assertEquals(newEntry.entryTime.epochSecond, result.entryTime.epochSecond)
        assertTrue(garageRepository.existsById(newEntry.licensePlate))
    }

    @Test
    fun deleteByIdShouldRemoveVehicleEntryByLicensePlate() {
        val licensePlateToDelete = "DEL7890"
        val entryToDelete = Garage(licensePlate = licensePlateToDelete, entryTime = Instant.now())
        garageRepository.save(entryToDelete)

        assertTrue(garageRepository.existsById(licensePlateToDelete))

        garageRepository.deleteById(licensePlateToDelete)

        assertFalse(garageRepository.existsById(licensePlateToDelete))
    }

    @Test
    fun existsByIdShouldReturnTrueIfEntryExists() {
        val licensePlate = "EXIST111"
        val existingEntry = Garage(licensePlate = licensePlate, entryTime = Instant.now())
        garageRepository.save(existingEntry)

        val result = garageRepository.existsById(licensePlate)

        assertTrue(result)
    }

    @Test
    fun existsByIdShouldReturnFalseIfEntryDoesNotExist() {
        val licensePlate = "NONEXIST222"
        val result = garageRepository.existsById(licensePlate)

        assertFalse(result)
    }

    @Test
    fun findAllShouldReturnAllVehicleEntries() {
        val entry1 = Garage(licensePlate = "VEH001", entryTime = Instant.now().minusSeconds(3600), spot = testSpot, parkedTime = Instant.now().minusSeconds(3000))
        val entry2 = Garage(licensePlate = "VEH002", entryTime = Instant.now().minusSeconds(1800), spot = testSpot, parkedTime = Instant.now().minusSeconds(1200))

        garageRepository.saveAll(listOf(entry1, entry2))

        val result = garageRepository.findAll().toList()

        assertEquals(2, result.size)
        assertTrue(result.any { it.licensePlate == "VEH001" })
        assertTrue(result.any { it.licensePlate == "VEH002" })
    }
}