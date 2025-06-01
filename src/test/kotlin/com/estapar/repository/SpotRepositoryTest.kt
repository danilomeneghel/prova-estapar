package com.estapar.repository

import com.estapar.model.Sector
import com.estapar.model.Spot
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@MicronautTest(environments = ["test"])
class SpotRepositoryTest {

    @Inject
    lateinit var spotRepository: SpotRepository

    @Inject
    lateinit var sectorRepository: SectorRepository

    private lateinit var testSector: Sector
    private val idCounter = AtomicLong(1L)

    @BeforeEach
    fun setup() {
        spotRepository.deleteAll()
        sectorRepository.deleteAll()

        testSector = Sector(
            name = "Setor Teste Spot-${UUID.randomUUID()}",
            basePrice = 10.0,
            maxCapacity = 10,
            openHour = "08:00",
            closeHour = "18:00",
            durationLimitMinutes = 180
        )
        testSector = sectorRepository.save(testSector)
        idCounter.set(1L)
    }

    @Test
    fun saveShouldPersistNewSpot() {
        val newSpot = Spot(
            id = idCounter.getAndIncrement(),
            lat = 1.0,
            lng = 2.0,
            ocupied = false,
            sector = testSector
        )

        val savedSpot = spotRepository.save(newSpot)

        assertNotNull(savedSpot.id)
        assertEquals(newSpot.id, savedSpot.id)
        assertEquals(newSpot.lat, savedSpot.lat, 0.001)
        assertEquals(newSpot.lng, savedSpot.lng, 0.001)
        assertEquals(newSpot.ocupied, savedSpot.ocupied)
        assertEquals(newSpot.sector.id, savedSpot.sector.id)
    }

    @Test
    fun findByIdShouldReturnOptionalOfSpotWhenFound() {
        val existingSpot = Spot(
            id = idCounter.getAndIncrement(),
            lat = 3.0,
            lng = 4.0,
            ocupied = true,
            sector = testSector
        )
        val savedSpot = spotRepository.save(existingSpot)

        val foundSpotOptional = spotRepository.findById(savedSpot.id)

        assertTrue(foundSpotOptional.isPresent)
        val foundSpot = foundSpotOptional.get()
        assertEquals(savedSpot.id, foundSpot.id)
        assertEquals(existingSpot.lat, foundSpot.lat, 0.001)
        assertEquals(existingSpot.lng, foundSpot.lng, 0.001)
    }

    @Test
    fun findByIdShouldReturnEmptyOptionalWhenNotFound() {
        val foundSpotOptional = spotRepository.findById(999L)

        assertFalse(foundSpotOptional.isPresent)
    }

    @Test
    fun findByLatAndLngShouldReturnSpotWhenFound() {
        val lat = 5.0
        val lng = 6.0
        val existingSpot = Spot(
            id = idCounter.getAndIncrement(),
            lat = lat,
            lng = lng,
            ocupied = false,
            sector = testSector
        )
        spotRepository.save(existingSpot)

        val foundSpot = spotRepository.findByLatAndLng(lat, lng)

        assertNotNull(foundSpot)
        assertEquals(lat, foundSpot!!.lat, 0.001)
        assertEquals(lng, foundSpot.lng, 0.001)
        assertEquals(existingSpot.sector.id, foundSpot.sector.id)
    }

    @Test
    fun findByLatAndLngShouldReturnNullWhenNotFound() {
        val foundSpot = spotRepository.findByLatAndLng(99.0, 99.0)

        assertNull(foundSpot)
    }

    @Test
    fun updateShouldModifyExistingSpot() {
        val initialSpot = Spot(
            id = idCounter.getAndIncrement(),
            lat = 7.0,
            lng = 8.0,
            ocupied = false,
            sector = testSector
        )
        val savedSpot = spotRepository.save(initialSpot)

        val updatedOcupied = true
        val spotToUpdate = savedSpot.copy(ocupied = updatedOcupied)
        val updatedSpot = spotRepository.update(spotToUpdate)

        assertNotNull(updatedSpot)
        assertEquals(savedSpot.id, updatedSpot.id)
        assertEquals(updatedOcupied, updatedSpot.ocupied)
    }

    @Test
    fun deleteShouldRemoveSpot() {
        val spotToDelete = Spot(
            id = idCounter.getAndIncrement(),
            lat = 9.0,
            lng = 10.0,
            ocupied = false,
            sector = testSector
        )
        val savedSpot = spotRepository.save(spotToDelete)

        assertNotNull(spotRepository.findById(savedSpot.id))

        spotRepository.delete(savedSpot)

        assertFalse(spotRepository.findById(savedSpot.id).isPresent)
    }

    @Test
    fun findAllShouldReturnAllSpots() {
        val spot1 = Spot(id = idCounter.getAndIncrement(), lat = 11.0, lng = 12.0, ocupied = false, sector = testSector)
        val spot2 = Spot(id = idCounter.getAndIncrement(), lat = 13.0, lng = 14.0, ocupied = true, sector = testSector)
        spotRepository.saveAll(listOf(spot1, spot2))

        val allSpots = spotRepository.findAll().toList()

        assertEquals(2, allSpots.size)
        assertTrue(allSpots.any { it.lat == 11.0 && it.lng == 12.0 })
        assertTrue(allSpots.any { it.lat == 13.0 && it.lng == 14.0 })
    }
}