package com.estapar.repository

import com.estapar.model.Sector
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class SectorRepositoryTest {

    @Mock
    private lateinit var sectorRepository: SectorRepository

    @Test
    fun findByIdShouldReturnOptionalOfSectorWhenFound() {
        val id = 1L
        val expectedSector = Sector(id = id, name = "A", basePrice = 10.0, maxCapacity = 100, openHour = "08", closeHour = "20", durationLimitMinutes = 300)

        whenever(sectorRepository.findById(id)).thenReturn(Optional.of(expectedSector))

        val result = sectorRepository.findById(id)

        assertThat(result).isEqualTo(Optional.of(expectedSector))
        verify(sectorRepository).findById(eq(id))
    }

    @Test
    fun saveShouldPersistANewSector() {
        val newSector = Sector(name = "New", basePrice = 15.0, maxCapacity = 50, openHour = "09", closeHour = "18", durationLimitMinutes = 240)

        whenever(sectorRepository.save(argThat { sector -> sector.name == newSector.name && sector.basePrice == newSector.basePrice }))
            .doAnswer { invocation ->
                val sectorArg = invocation.getArgument<Sector>(0)
                sectorArg.id = 1L
                sectorArg
            }

        val result = sectorRepository.save(newSector)

        assertThat(result.id).isNotNull()
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.name).isEqualTo(newSector.name)
        assertThat(result.basePrice).isEqualTo(newSector.basePrice)
        verify(sectorRepository).save(argThat { sector -> sector.name == newSector.name && sector.basePrice == newSector.basePrice })
    }

    @Test
    fun deleteByIdShouldRemoveSectorById() {
        val idToDelete = 1L

        sectorRepository.deleteById(idToDelete)

        verify(sectorRepository).deleteById(eq(idToDelete))
    }

    @Test
    fun findAllShouldReturnAllSectors() {
        val sectors = listOf(
            Sector(id = 1L, name = "A", basePrice = 10.0, maxCapacity = 100, openHour = "08", closeHour = "20", durationLimitMinutes = 300),
            Sector(id = 2L, name = "B", basePrice = 20.0, maxCapacity = 200, openHour = "07", closeHour = "22", durationLimitMinutes = 400)
        )
        whenever(sectorRepository.findAll()).thenReturn(sectors)

        val result = sectorRepository.findAll()

        assertThat(result).isEqualTo(sectors)
        verify(sectorRepository).findAll()
    }
}