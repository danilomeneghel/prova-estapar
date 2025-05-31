package com.estapar.controller

import com.estapar.dto.*
import com.estapar.service.ParkingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class ApiParkingControllerTest {

    @Mock
    private lateinit var parkingService: ParkingService

    @InjectMocks
    private lateinit var apiParkingController: ApiParkingController

    @Test
    fun plateStatusShouldReturnPlateStatusDTO() {
        val licensePlate = "ZUL0001"
        val requestBody = mapOf("license_plate" to licensePlate)
        val expectedResponse = PlateStatusDTO(
            licensePlate = licensePlate,
            priceUntilNow = 0.00,
            entryTime = Instant.parse("2025-01-01T12:00:00Z"),
            timeParked = Instant.parse("2025-01-01T12:00:00Z"),
            lat = -23.561684,
            lng = -46.655981
        )

        whenever(parkingService.postPlateStatus(licensePlate)).thenReturn(expectedResponse)

        val result = apiParkingController.plateStatus(requestBody)

        assertEquals(expectedResponse, result)
        verify(parkingService).postPlateStatus(licensePlate)
    }

    @Test
    fun spotStatusShouldReturnSpotStatusDTO() {
        val lat = -23.561684
        val lng = -46.655981
        val requestBody = mapOf("lat" to lat, "lng" to lng)
        val expectedResponse = SpotStatusDTO(
            ocupied = true,
            licensePlate = "ABC1234",
            priceUntilNow = 15.50,
            entryTime = Instant.parse("2025-01-01T10:00:00Z"),
            timeParked = Instant.parse("2025-01-01T10:30:00Z")
        )

        whenever(parkingService.postSpotStatus(lat, lng)).thenReturn(expectedResponse)

        val result = apiParkingController.spotStatus(requestBody)

        assertEquals(expectedResponse, result)
        verify(parkingService).postSpotStatus(lat, lng)
    }

    @Test
    fun revenueShouldReturnRevenueDTO() {
        val date = LocalDate.of(2025, 5, 29)
        val sector = "Setor A"
        val requestBody = RevenueRequestDTO(date, sector)
        val expectedResponse = RevenueDTO(
            amount = 123.45,
            currency = "BRL",
            timestamp = "2025-05-29T00:00:00Z"
        )

        whenever(parkingService.getRevenue(date, sector)).thenReturn(expectedResponse)

        val result = apiParkingController.revenue(requestBody)

        assertEquals(expectedResponse, result)
        verify(parkingService).getRevenue(date, sector)
    }

    @Test
    fun garageStatusShouldReturnGarageInfoDTO() {
        val expectedResponse = GarageInfoDTO(
            garage = listOf(
                SectorInfo(
                    sector = "Setor X",
                    basePrice = 10.0,
                    maxCapacity = 100,
                    openHour = "08:00",
                    closeHour = "22:00",
                    durationLimitMinutes = 240
                )
            ),
            spots = listOf(
                SpotInfo(
                    id = 1L,
                    sector = "Setor X",
                    lat = -23.5,
                    lng = -46.6
                )
            )
        )

        whenever(parkingService.getGarage()).thenReturn(expectedResponse)

        val result = apiParkingController.garageStatus()

        assertEquals(expectedResponse, result)
        verify(parkingService).getGarage()
    }
}