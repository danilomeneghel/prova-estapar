package com.estapar.repository

import com.estapar.model.Spot
import com.estapar.model.Sector
import com.estapar.model.VehicleEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class VehicleEntryRepositoryTest {

    @Mock
    private lateinit var vehicleEntryRepository: VehicleEntryRepository

    @Test
    fun `findById should return optional of vehicle entry when found`() {
        val licensePlate = "ABC1234"
        val expectedEntry = VehicleEntry(licensePlate = licensePlate, entryTime = Instant.now())

        whenever(vehicleEntryRepository.findById(licensePlate)).thenReturn(Optional.of(expectedEntry))

        val result = vehicleEntryRepository.findById(licensePlate)

        assertThat(result).isEqualTo(Optional.of(expectedEntry))
        verify(vehicleEntryRepository).findById(eq(licensePlate))
    }

    @Test
    fun `save should persist a new vehicle entry`() {
        val newEntry = VehicleEntry(licensePlate = "NEW4567", entryTime = Instant.now())
        
        whenever(vehicleEntryRepository.save(argThat { entry -> entry.licensePlate == newEntry.licensePlate && entry.entryTime == newEntry.entryTime }))
            .doAnswer { invocation ->
                invocation.getArgument<VehicleEntry>(0)
            }

        val result = vehicleEntryRepository.save(newEntry)

        assertThat(result.licensePlate).isEqualTo(newEntry.licensePlate)
        assertThat(result.entryTime).isEqualTo(newEntry.entryTime)
        assertThat(result).isEqualTo(newEntry)
    }

    @Test
    fun `deleteById should remove vehicle entry by license plate`() {
        val licensePlateToDelete = "DEL7890"

        vehicleEntryRepository.deleteById(licensePlateToDelete)

        verify(vehicleEntryRepository).deleteById(eq(licensePlateToDelete))
    }

    @Test
    fun `existsById should return true if entry exists`() {
        val licensePlate = "EXIST111"
        whenever(vehicleEntryRepository.existsById(licensePlate)).thenReturn(true)

        val result = vehicleEntryRepository.existsById(licensePlate)

        assertThat(result).isTrue()
        verify(vehicleEntryRepository).existsById(eq(licensePlate))
    }

    @Test
    fun `existsById should return false if entry does not exist`() {
        val licensePlate = "NONEXIST222"
        whenever(vehicleEntryRepository.existsById(licensePlate)).thenReturn(false)

        val result = vehicleEntryRepository.existsById(licensePlate)

        assertThat(result).isFalse()
        verify(vehicleEntryRepository).existsById(eq(licensePlate))
    }

    @Test
    fun `findAll should return all vehicle entries`() {
        val mockSector = Sector(id = 100L, name = "Parking", basePrice = 5.0, maxCapacity = 10, openHour = "06", closeHour = "22", durationLimitMinutes = 180)
        val mockSpot1 = Spot(id = 1L, sector = mockSector, lat = 10.0, lng = 20.0, ocupied = true)
        val mockSpot2 = Spot(id = 2L, sector = mockSector, lat = 11.0, lng = 21.0, ocupied = false)

        val entries = listOf(
            VehicleEntry(licensePlate = "VEH001", entryTime = Instant.now().minusSeconds(3600), spot = mockSpot1, parkedTime = Instant.now().minusSeconds(3000)),
            VehicleEntry(licensePlate = "VEH002", entryTime = Instant.now().minusSeconds(1800), spot = mockSpot2, parkedTime = Instant.now().minusSeconds(1200))
        )
        whenever(vehicleEntryRepository.findAll()).thenReturn(entries)

        val result = vehicleEntryRepository.findAll()

        assertThat(result).isEqualTo(entries)
        verify(vehicleEntryRepository).findAll()
    }
}