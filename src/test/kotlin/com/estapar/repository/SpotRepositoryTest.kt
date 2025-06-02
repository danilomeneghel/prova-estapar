package com.estapar.repository

import com.estapar.model.Sector
import com.estapar.model.Spot
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MicronautTest
class SpotRepositoryTest {

    @Inject
    lateinit var spotRepository: SpotRepository

    @Inject
    lateinit var sectorRepository: SectorRepository

    private lateinit var defaultSector: Sector

    @BeforeEach
    fun setup() {
        spotRepository.deleteAll()
        sectorRepository.deleteAll()
        defaultSector = Sector(
            name = "Test Sector",
            basePrice = 30.0,
            maxCapacity = 10,
            openHour = "08:00",
            closeHour = "18:00",
            durationLimitMinutes = 60
        )
        sectorRepository.save(defaultSector)
    }

    @Test
    fun `should save and find spot entry`() {
        val spot = Spot(id = 1L, lat = -23.561, lng = -46.656, sector = defaultSector, ocupied = false)
        val savedSpot = spotRepository.save(spot)

        assertNotNull(savedSpot)
        assertEquals(1L, savedSpot.id)
        assertEquals(-23.561, savedSpot.lat, 0.0001)
        assertEquals(-46.656, savedSpot.lng, 0.0001)
        assertFalse(savedSpot.ocupied)
        assertEquals(defaultSector.id, savedSpot.sector.id)

        val foundSpot = spotRepository.findById(1L).orElse(null)
        assertNotNull(foundSpot)
        assertEquals(1L, foundSpot.id)
        assertEquals(defaultSector.id, foundSpot.sector.id)
    }

    @Test
    fun `should update spot entry`() {
        val spot = Spot(id = 2L, lat = -23.562, lng = -46.657, sector = defaultSector, ocupied = false)
        val savedSpot = spotRepository.save(spot)

        savedSpot.ocupied = true
        val updatedSpot = spotRepository.update(savedSpot)

        assertNotNull(updatedSpot)
        assertEquals(2L, updatedSpot.id)
        assertTrue(updatedSpot.ocupied)

        val foundSpot = spotRepository.findById(2L).orElse(null)
        assertNotNull(foundSpot)
        assertTrue(foundSpot.ocupied)
    }

    @Test
    fun `should find by latitude and longitude`() {
        val spot1 = Spot(id = 3L, lat = -23.563, lng = -46.658, sector = defaultSector, ocupied = false)
        val spot2 = Spot(id = 4L, lat = -23.564, lng = -46.659, sector = defaultSector, ocupied = true)
        spotRepository.saveAll(listOf(spot1, spot2))

        val foundSpot1 = spotRepository.findByLatAndLng(-23.563, -46.658)
        assertNotNull(foundSpot1)
        assertEquals(3L, foundSpot1!!.id)
        assertFalse(foundSpot1.ocupied)

        val foundSpot2 = spotRepository.findByLatAndLng(-23.564, -46.659)
        assertNotNull(foundSpot2)
        assertEquals(4L, foundSpot2!!.id)
        assertTrue(foundSpot2.ocupied)

        val notFoundSpot = spotRepository.findByLatAndLng(-99.0, -99.0)
        assertNull(notFoundSpot)
    }

    @Test
    fun `should delete spot entry`() {
        val spot = Spot(id = 5L, lat = -23.565, lng = -46.660, sector = defaultSector, ocupied = false)
        val savedSpot = spotRepository.save(spot)

        var foundSpot = spotRepository.findById(5L).orElse(null)
        assertNotNull(foundSpot)

        spotRepository.delete(savedSpot)

        foundSpot = spotRepository.findById(5L).orElse(null)
        assertNull(foundSpot)
    }

    @Test
    fun `should update spot sector`() {
        val newSector = Sector(
            name = "New Test Sector",
            basePrice = 40.0,
            maxCapacity = 15,
            openHour = "07:00",
            closeHour = "21:00",
            durationLimitMinutes = 90
        )
        sectorRepository.save(newSector)

        val spot = Spot(id = 6L, lat = -23.566, lng = -46.661, sector = defaultSector, ocupied = false)
        val savedSpot = spotRepository.save(spot)

        savedSpot.sector = newSector
        val updatedSpot = spotRepository.update(savedSpot)

        assertNotNull(updatedSpot)
        assertEquals(6L, updatedSpot.id)
        assertEquals(newSector.id, updatedSpot.sector.id)
        assertEquals("New Test Sector", updatedSpot.sector.name)
    }
}