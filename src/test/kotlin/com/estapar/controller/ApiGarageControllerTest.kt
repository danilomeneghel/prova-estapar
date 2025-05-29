package com.estapar.controller

import com.estapar.dto.GarageInfoDTO
import com.estapar.dto.PlateStatusDTO
import com.estapar.dto.RevenueDTO
import com.estapar.dto.SectorInfo
import com.estapar.dto.SpotInfo
import com.estapar.dto.SpotStatusDTO
import com.estapar.service.GarageService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class ApiGarageControllerTest {

    @Mock
    private lateinit var garageService: GarageService

    @InjectMocks
    private lateinit var apiGarageController: ApiGarageController

    @Test
    fun `plateStatus should return PlateStatusDTO from GarageService`() {
        val licensePlate = "ABC1234"
        val requestBody = mapOf("license_plate" to licensePlate)
        val expectedPlateStatusDTO = PlateStatusDTO(
            licensePlate = licensePlate,
            priceUntilNow = 15.50,
            entryTime = Instant.now().minusSeconds(3600),
            timeParked = Instant.now().minusSeconds(1800),
            lat = -23.5505,
            lng = -46.6333
        )

        whenever(garageService.postPlateStatus(eq(licensePlate))).thenReturn(expectedPlateStatusDTO)

        val result = apiGarageController.plateStatus(requestBody)

        assertThat(result).isEqualTo(expectedPlateStatusDTO)
        verify(garageService).postPlateStatus(eq(licensePlate))
    }

    @Test
    fun `spotStatus should return SpotStatusDTO from GarageService`() {
        val lat = -23.5505
        val lng = -46.6333
        val requestBody = mapOf("lat" to lat, "lng" to lng)
        val expectedSpotStatusDTO = SpotStatusDTO(
            ocupied = true,
            licensePlate = "XYZ5678",
            priceUntilNow = 25.00,
            entryTime = Instant.now().minusSeconds(7200),
            timeParked = Instant.now().minusSeconds(3600)
        )

        whenever(garageService.postSpotStatus(eq(lat), eq(lng))).thenReturn(expectedSpotStatusDTO)

        val result = apiGarageController.spotStatus(requestBody)

        assertThat(result).isEqualTo(expectedSpotStatusDTO)
        verify(garageService).postSpotStatus(eq(lat), eq(lng))
    }

    @Test
    fun `revenue should return RevenueDTO from GarageService`() {
        val dateString = "2024-05-28"
        val parsedDate = LocalDate.parse(dateString)
        val sector = "A"
        val expectedRevenueDTO = RevenueDTO(
            amount = 1234.56,
            currency = "BRL",
            timestamp = "2024-05-28T00:00:00Z"
        )

        whenever(garageService.getRevenue(eq(parsedDate), eq(sector))).thenReturn(expectedRevenueDTO)

        val result = apiGarageController.revenue(dateString, sector)

        assertThat(result).isEqualTo(expectedRevenueDTO)
        verify(garageService).getRevenue(eq(parsedDate), eq(sector))
    }

    @Test
    fun `garageStatus should return GarageInfoDTO from GarageService`() {
        val expectedGarageInfoDTO = GarageInfoDTO(
            garage = listOf(
                SectorInfo(
                    sector = "A",
                    basePrice = 10.0,
                    max_capacity = 100,
                    open_hour = "08:00",
                    close_hour = "22:00",
                    duration_limit_minutes = 240
                )
            ),
            spots = listOf(
                SpotInfo(id = 1, sector = "A", lat = -23.5505, lng = -46.6333)
            )
        )

        whenever(garageService.getGarage()).thenReturn(expectedGarageInfoDTO)

        val result = apiGarageController.garageStatus()

        assertThat(result).isEqualTo(expectedGarageInfoDTO)
        verify(garageService).getGarage()
    }
}