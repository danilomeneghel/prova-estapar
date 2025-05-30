package com.estapar.repository

import com.estapar.model.Spot
import com.estapar.model.Sector
import com.estapar.model.Garage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class GarageRepositoryTest {

    @Mock
    private lateinit var garageRepository: GarageRepository

    @Test
    fun findByIdShouldReturnOptionalOfVehicleEntryWhenFound() {
        val licensePlate = "ABC1234"
        val expectedEntry = Garage(licensePlate = licensePlate, entryTime = Instant.now())

        whenever(garageRepository.findById(licensePlate)).thenReturn(Optional.of(expectedEntry))

        val result = garageRepository.findById(licensePlate)

        assertThat(result).isEqualTo(Optional.of(expectedEntry))
        verify(garageRepository).findById(eq(licensePlate))
    }

    @Test
    fun saveShouldPersistANewVehicleEntry() {
        val newEntry = Garage(licensePlate = "NEW4567", entryTime = Instant.now())

        whenever(garageRepository.save(argThat { entry -> entry.licensePlate == newEntry.licensePlate && entry.entryTime == newEntry.entryTime }))
            .doAnswer { invocation ->
                invocation.getArgument<Garage>(0)
            }

        val result = garageRepository.save(newEntry)

        assertThat(result.licensePlate).isEqualTo(newEntry.licensePlate)
        assertThat(result.entryTime).isEqualTo(newEntry.entryTime)
        assertThat(result).isEqualTo(newEntry)
    }

    @Test
    fun deleteByIdShouldRemoveVehicleEntryByLicensePlate() {
        val licensePlateToDelete = "DEL7890"

        garageRepository.deleteById(licensePlateToDelete)

        verify(garageRepository).deleteById(eq(licensePlateToDelete))
    }

    @Test
    fun existsByIdShouldReturnTrueIfEntryExists() {
        val licensePlate = "EXIST111"
        whenever(garageRepository.existsById(licensePlate)).thenReturn(true)

        val result = garageRepository.existsById(licensePlate)

        assertThat(result).isTrue()
        verify(garageRepository).existsById(eq(licensePlate))
    }

    @Test
    fun existsByIdShouldReturnFalseIfEntryDoesNotExist() {
        val licensePlate = "NONEXIST222"
        whenever(garageRepository.existsById(licensePlate)).thenReturn(false)

        val result = garageRepository.existsById(licensePlate)

        assertThat(result).isFalse()
        verify(garageRepository).existsById(eq(licensePlate))
    }

    @Test
    fun findAllShouldReturnAllVehicleEntries() {
        val mockSector = Sector(id = 100L, name = "Parking", basePrice = 5.0, maxCapacity = 10, openHour = "06", closeHour = "22", durationLimitMinutes = 180)
        val mockSpot1 = Spot(id = 1L, sector = mockSector, lat = 10.0, lng = 20.0, ocupied = true)
        val mockSpot2 = Spot(id = 2L, sector = mockSector, lat = 11.0, lng = 21.0, ocupied = false)

        val entries = listOf(
            Garage(licensePlate = "VEH001", entryTime = Instant.now().minusSeconds(3600), spot = mockSpot1, parkedTime = Instant.now().minusSeconds(3000)),
            Garage(licensePlate = "VEH002", entryTime = Instant.now().minusSeconds(1800), spot = mockSpot2, parkedTime = Instant.now().minusSeconds(1200))
        )
        whenever(garageRepository.findAll()).thenReturn(entries)

        val result = garageRepository.findAll()

        assertThat(result).isEqualTo(entries)
        verify(garageRepository).findAll()
    }
}