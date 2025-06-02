package com.estapar.repository

import com.estapar.model.Sector
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MicronautTest
class SectorRepositoryTest {

    @Inject
    lateinit var sectorRepository: SectorRepository

    @BeforeEach
    fun setup() {
        sectorRepository.deleteAll()
    }

    @Test
    fun `should save and find sector entry`() {
        val sector = Sector(
            name = "Setor A",
            basePrice = 50.0,
            maxCapacity = 20,
            openHour = "06:00",
            closeHour = "22:00",
            durationLimitMinutes = 120
        )
        val savedSector = sectorRepository.save(sector)

        assertNotNull(savedSector.id)
        assertEquals("Setor A", savedSector.name)
        assertEquals(50.0, savedSector.basePrice, 0.001)
        assertEquals(20, savedSector.maxCapacity)
        assertEquals("06:00", savedSector.openHour)
        assertEquals("22:00", savedSector.closeHour)
        assertEquals(120, savedSector.durationLimitMinutes)
        assertEquals(0, savedSector.currentOcupied)

        val foundSector = sectorRepository.findById(savedSector.id!!).orElse(null)
        assertNotNull(foundSector)
        assertEquals(savedSector.id, foundSector.id)
        assertEquals("Setor A", foundSector.name)
    }

    @Test
    fun `should update sector entry`() {
        val sector = Sector(
            name = "Setor B",
            basePrice = 45.0,
            maxCapacity = 15,
            openHour = "07:00",
            closeHour = "23:00",
            durationLimitMinutes = 90
        )
        val savedSector = sectorRepository.save(sector)

        savedSector.basePrice = 60.0
        savedSector.currentOcupied = 5
        val updatedSector = sectorRepository.update(savedSector)

        assertNotNull(updatedSector)
        assertEquals(savedSector.id, updatedSector.id)
        assertEquals(60.0, updatedSector.basePrice, 0.001)
        assertEquals(5, updatedSector.currentOcupied)

        val foundSector = sectorRepository.findById(savedSector.id!!).orElse(null)
        assertNotNull(foundSector)
        assertEquals(60.0, foundSector.basePrice, 0.001)
        assertEquals(5, foundSector.currentOcupied)
    }

    @Test
    fun `should find by name`() {
        val sector1 = Sector(
            name = "Setor C",
            basePrice = 70.0,
            maxCapacity = 30,
            openHour = "05:00",
            closeHour = "00:00",
            durationLimitMinutes = 180
        )
        val sector2 = Sector(
            name = "Setor D",
            basePrice = 80.0,
            maxCapacity = 25,
            openHour = "08:00",
            closeHour = "20:00",
            durationLimitMinutes = 60
        )
        sectorRepository.saveAll(listOf(sector1, sector2))

        val foundSectorC = sectorRepository.findByName("Setor C")
        assertNotNull(foundSectorC)
        assertEquals(sector1.id, foundSectorC!!.id)
        assertEquals("Setor C", foundSectorC.name)

        val foundSectorD = sectorRepository.findByName("Setor D")
        assertNotNull(foundSectorD)
        assertEquals(sector2.id, foundSectorD!!.id)
        assertEquals("Setor D", foundSectorD.name)

        val notFoundSector = sectorRepository.findByName("Setor E")
        assertNull(notFoundSector)
    }

    @Test
    fun `should delete sector entry`() {
        val sector = Sector(
            name = "Setor F",
            basePrice = 30.0,
            maxCapacity = 10,
            openHour = "09:00",
            closeHour = "17:00",
            durationLimitMinutes = 45
        )
        val savedSector = sectorRepository.save(sector)

        var foundSector = sectorRepository.findById(savedSector.id!!).orElse(null)
        assertNotNull(foundSector)

        sectorRepository.delete(savedSector)

        foundSector = sectorRepository.findById(savedSector.id!!).orElse(null)
        assertNull(foundSector)
    }

    @Test
    fun `should return null when finding non-existent sector by name`() {
        val nonExistentSector = sectorRepository.findByName("NonExistentSector")
        assertNull(nonExistentSector)
    }
}