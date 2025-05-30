package com.estapar.repository

import com.estapar.model.Sector
import com.estapar.model.Spot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class SpotRepositoryTest {

    @Mock
    private lateinit var spotRepository: SpotRepository

    @Test
    fun findByLatAndLngShouldReturnSpotWhenFound() {
        val lat = -23.5
        val lng = -46.6
        val mockSector = Sector(id = 1L, name = "A", basePrice = 10.0, maxCapacity = 100, openHour = "08", closeHour = "20", durationLimitMinutes = 300)
        val expectedSpot = Spot(id = 1, sector = mockSector, lat = lat, lng = lng, ocupied = false)

        whenever(spotRepository.findByLatAndLng(lat, lng)).thenReturn(expectedSpot)

        val result = spotRepository.findByLatAndLng(lat, lng)

        assertThat(result).isEqualTo(expectedSpot)
        verify(spotRepository).findByLatAndLng(eq(lat), eq(lng))
    }

    @Test
    fun findByLatAndLngShouldReturnNullWhenNotFound() {
        val lat = -99.0
        val lng = -99.0

        whenever(spotRepository.findByLatAndLng(lat, lng)).thenReturn(null)

        val result = spotRepository.findByLatAndLng(lat, lng)

        assertThat(result).isNull()
        verify(spotRepository).findByLatAndLng(eq(lat), eq(lng))
    }

    @Test
    fun findByIdShouldReturnOptionalOfSpotWhenFound() {
        val id = 1L
        val mockSector = Sector(id = 1L, name = "A", basePrice = 10.0, maxCapacity = 100, openHour = "08", closeHour = "20", durationLimitMinutes = 300)
        val expectedSpot = Spot(id = id, sector = mockSector, lat = 1.0, lng = 2.0, ocupied = true)

        whenever(spotRepository.findById(id)).thenReturn(Optional.of(expectedSpot))

        val result = spotRepository.findById(id)

        assertThat(result).isEqualTo(Optional.of(expectedSpot))
        verify(spotRepository).findById(eq(id))
    }

    @Test
    fun saveShouldPersistANewSpot() {
        val mockSector = Sector(id = 1L, name = "B", basePrice = 20.0, maxCapacity = 50, openHour = "09", closeHour = "18", durationLimitMinutes = 240)
        val newSpot = Spot(sector = mockSector, lat = 3.0, lng = 4.0, ocupied = false)

        whenever(spotRepository.save(argThat { spot -> spot.lat == newSpot.lat && spot.lng == newSpot.lng && spot.ocupied == newSpot.ocupied && spot.sector == newSpot.sector }))
            .doAnswer { invocation ->
                val spotArg = invocation.getArgument<Spot>(0)
                spotArg.id = 1L
                spotArg
            }

        val result = spotRepository.save(newSpot)

        assertThat(result.id).isNotNull()
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.lat).isEqualTo(newSpot.lat)
        assertThat(result.lng).isEqualTo(newSpot.lng)
        verify(spotRepository).save(argThat { spot -> spot.lat == newSpot.lat && spot.lng == newSpot.lng && spot.ocupied == newSpot.ocupied && spot.sector == newSpot.sector })
    }

    @Test
    fun deleteByIdShouldRemoveSpotById() {
        val idToDelete = 1L

        spotRepository.deleteById(idToDelete)

        verify(spotRepository).deleteById(eq(idToDelete))
    }

    @Test
    fun findAllShouldReturnAllSpots() {
        val mockSector1 = Sector(id = 10L, name = "A", basePrice = 10.0, maxCapacity = 100, openHour = "08", closeHour = "20", durationLimitMinutes = 300)
        val mockSector2 = Sector(id = 20L, name = "B", basePrice = 20.0, maxCapacity = 50, openHour = "09", closeHour = "18", durationLimitMinutes = 240)
        val spots = listOf(
            Spot(id = 1L, sector = mockSector1, lat = 1.0, lng = 1.0, ocupied = false),
            Spot(id = 2L, sector = mockSector2, lat = 2.0, lng = 2.0, ocupied = true)
        )
        whenever(spotRepository.findAll()).thenReturn(spots)

        val result = spotRepository.findAll()

        assertThat(result).isEqualTo(spots)
        verify(spotRepository).findAll()
    }
}