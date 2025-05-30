package com.estapar.repository

import com.estapar.model.Sector
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@MicronautTest(environments = ["test"])
class SectorRepositoryTest {

    @Inject
    lateinit var sectorRepository: SectorRepository

    @BeforeEach
    fun setup() {
        sectorRepository.deleteAll()
    }

    @Test
    fun saveShouldPersistNewSector() {
        val newSector = Sector(
            name = "Setor Alpha-${UUID.randomUUID()}",
            basePrice = 10.50,
            maxCapacity = 50,
            openHour = "08:00",
            closeHour = "20:00",
            durationLimitMinutes = 240,
            currentOcupied = 5
        )

        val savedSector = sectorRepository.save(newSector)

        assertNotNull(savedSector.id)
        assertEquals(newSector.name, savedSector.name)
        assertEquals(newSector.basePrice, savedSector.basePrice, 0.001)
        assertEquals(newSector.maxCapacity, savedSector.maxCapacity)
        assertEquals(newSector.openHour, savedSector.openHour)
        assertEquals(newSector.closeHour, savedSector.closeHour)
        assertEquals(newSector.durationLimitMinutes, savedSector.durationLimitMinutes)
        assertEquals(newSector.currentOcupied, savedSector.currentOcupied)
    }

    @Test
    fun findByIdShouldReturnOptionalOfSectorWhenFound() {
        val existingSector = Sector(
            name = "Setor Beta-${UUID.randomUUID()}",
            basePrice = 12.00,
            maxCapacity = 30,
            openHour = "07:00",
            closeHour = "23:00",
            durationLimitMinutes = 300,
            currentOcupied = 10
        )
        val savedSector = sectorRepository.save(existingSector)

        val foundSectorOptional = sectorRepository.findById(savedSector.id!!)

        assertTrue(foundSectorOptional.isPresent)
        val foundSector = foundSectorOptional.get()
        assertEquals(savedSector.id, foundSector.id)
        assertEquals(existingSector.name, foundSector.name)
    }

    @Test
    fun findByIdShouldReturnEmptyOptionalWhenNotFound() {
        val foundSectorOptional = sectorRepository.findById(999L)

        assertFalse(foundSectorOptional.isPresent)
    }

    @Test
    fun findByNameShouldReturnSectorWhenFound() {
        val sectorName = "Setor Gamma-${UUID.randomUUID()}"
        val existingSector = Sector(
            name = sectorName,
            basePrice = 8.00,
            maxCapacity = 20,
            openHour = "09:00",
            closeHour = "18:00",
            durationLimitMinutes = 180,
            currentOcupied = 3
        )
        sectorRepository.save(existingSector)

        val foundSector = sectorRepository.findByName(sectorName)

        assertNotNull(foundSector)
        assertEquals(sectorName, foundSector!!.name)
        assertEquals(existingSector.basePrice, foundSector.basePrice, 0.001)
    }

    @Test
    fun findByNameShouldReturnNullWhenNotFound() {
        val foundSector = sectorRepository.findByName("Setor Inexistente-${UUID.randomUUID()}")

        assertNull(foundSector)
    }

    @Test
    fun updateShouldModifyExistingSector() {
        val initialSector = Sector(
            name = "Setor Delta-${UUID.randomUUID()}",
            basePrice = 15.00,
            maxCapacity = 60,
            openHour = "06:00",
            closeHour = "00:00",
            durationLimitMinutes = 360,
            currentOcupied = 20
        )
        val savedSector = sectorRepository.save(initialSector)

        val updatedCurrentOccupied = 25
        val sectorToUpdate = savedSector.copy(currentOcupied = updatedCurrentOccupied)
        val updatedSector = sectorRepository.update(sectorToUpdate)

        assertNotNull(updatedSector)
        assertEquals(savedSector.id, updatedSector.id)
        assertEquals(updatedCurrentOccupied, updatedSector.currentOcupied)
    }

    @Test
    fun deleteShouldRemoveSector() {
        val sectorToDelete = Sector(
            name = "Setor Epsilon-${UUID.randomUUID()}",
            basePrice = 7.00,
            maxCapacity = 25,
            openHour = "10:00",
            closeHour = "19:00",
            durationLimitMinutes = 120,
            currentOcupied = 8
        )
        val savedSector = sectorRepository.save(sectorToDelete)

        assertNotNull(sectorRepository.findById(savedSector.id!!))

        sectorRepository.delete(savedSector)

        assertFalse(sectorRepository.findById(savedSector.id!!).isPresent)
    }

    @Test
    fun findAllShouldReturnAllSectors() {
        val sector1 = Sector(name = "S1-${UUID.randomUUID()}", basePrice = 5.0, maxCapacity = 10, openHour = "08:00", closeHour = "18:00", durationLimitMinutes = 120, currentOcupied = 2)
        val sector2 = Sector(name = "S2-${UUID.randomUUID()}", basePrice = 7.5, maxCapacity = 15, openHour = "09:00", closeHour = "19:00", durationLimitMinutes = 180, currentOcupied = 5)
        sectorRepository.saveAll(listOf(sector1, sector2))

        val allSectors = sectorRepository.findAll().toList()

        assertEquals(2, allSectors.size)
        assertTrue(allSectors.any { it.name.startsWith("S1-") })
        assertTrue(allSectors.any { it.name.startsWith("S2-") })
    }
}