package com.estapar.service

import com.estapar.dto.*
import com.estapar.model.*
import com.estapar.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

@ExtendWith(MockitoExtension::class)
class GarageServiceTest {

    @Mock
    private lateinit var sectorRepo: SectorRepository

    @Mock
    private lateinit var spotRepo: SpotRepository

    @Mock
    private lateinit var entryRepo: VehicleEntryRepository

    @Mock
    private lateinit var revenueRepo: RevenueRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var garageService: GarageService

    @Test
    fun `registerEntry should do nothing if entry already exists`() {
        val plate = "TEST001"
        val entryTime = Instant.now()

        whenever(entryRepo.existsById(plate)).thenReturn(true)

        garageService.registerEntry(plate, entryTime)

        verify(entryRepo, never()).save(any())
    }

    @Test
    fun `assignSpot should assign spot, update entry and sector if conditions met`() {
        val plate = "TEST002"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now().minusSeconds(1000)
        val mockSector = Sector(name = "A", basePrice = 10.0, maxCapacity = 10, openHour = "08", closeHour = "20", durationLimitMinutes = 300, currentOcupied = 5)
        val mockSpot = Spot(id = 1, sector = mockSector, lat = lat, lng = lng, ocupied = false)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = null, parkedTime = null)

        whenever(entryRepo.findById(plate)).thenReturn(Optional.of(mockEntry))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(mockSpot)

        garageService.assignSpot(plate, lat, lng)

        assertThat(mockSpot.ocupied).isTrue()
        assertThat(mockEntry.spot).isEqualTo(mockSpot)
        assertThat(mockEntry.parkedTime).isNotNull()
        assertThat(mockSector.currentOcupied).isEqualTo(6)

        verify(spotRepo).update(eq(mockSpot))
        verify(sectorRepo).update(eq(mockSector))
        verify(entryRepo).update(eq(mockEntry))
    }

    @Test
    fun `assignSpot should do nothing if entry not found`() {
        val plate = "TEST002"
        val lat = 10.0
        val lng = 20.0

        whenever(entryRepo.findById(plate)).thenReturn(Optional.empty())

        garageService.assignSpot(plate, lat, lng)

        verify(spotRepo, never()).findByLatAndLng(any(), any())
        verify(spotRepo, never()).update(any())
        verify(sectorRepo, never()).update(any())
        verify(entryRepo, never()).update(any())
    }

    @Test
    fun `assignSpot should do nothing if spot not found`() {
        val plate = "TEST002"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now().minusSeconds(1000)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = null, parkedTime = null)

        whenever(entryRepo.findById(plate)).thenReturn(Optional.of(mockEntry))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(null)

        garageService.assignSpot(plate, lat, lng)

        verify(spotRepo, never()).update(any())
        verify(sectorRepo, never()).update(any())
        verify(entryRepo, never()).update(any())
    }

    @Test
    fun `assignSpot should do nothing if spot is already occupied`() {
        val plate = "TEST002"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now().minusSeconds(1000)
        val mockSector = Sector(name = "A", basePrice = 10.0, maxCapacity = 10, openHour = "08", closeHour = "20", durationLimitMinutes = 300, currentOcupied = 5)
        val mockSpot = Spot(id = 1, sector = mockSector, lat = lat, lng = lng, ocupied = true)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = null, parkedTime = null)

        whenever(entryRepo.findById(plate)).thenReturn(Optional.of(mockEntry))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(mockSpot)

        garageService.assignSpot(plate, lat, lng)

        verify(spotRepo, never()).update(any())
        verify(sectorRepo, never()).update(any())
        verify(entryRepo, never()).update(any())
    }

    @Test
    fun `handleExit should calculate revenue, update spot and sector, and delete entry`() {
        val plate = "TEST003"
        val exitTime = Instant.now()
        val entryTime = Instant.now().minusSeconds(3600)
        val mockSector = Sector(name = "B", basePrice = 20.0, maxCapacity = 50, openHour = "08", closeHour = "20", durationLimitMinutes = 300, currentOcupied = 20)
        val mockSpot = Spot(id = 2, sector = mockSector, lat = 30.0, lng = 40.0, ocupied = true)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = mockSpot, parkedTime = entryTime)
        val existingRevenue = Revenue(id = 1L, date = LocalDate.ofInstant(exitTime, ZoneId.systemDefault()), sectorName = "B", amount = 100.0)

        whenever(entryRepo.findById(plate)).thenReturn(Optional.of(mockEntry))
        whenever(revenueRepo.findByDateAndSectorName(any(), eq("B"))).thenReturn(existingRevenue)

        garageService.handleExit(plate, exitTime)

        val expectedPrice = 20.0
        assertThat(existingRevenue.amount).isEqualTo(100.0 + expectedPrice)
        assertThat(mockSpot.ocupied).isFalse()
        assertThat(mockSector.currentOcupied).isEqualTo(19)

        verify(revenueRepo).update(eq(existingRevenue))
        verify(spotRepo).update(eq(mockSpot))
        verify(sectorRepo).update(eq(mockSector))
        verify(entryRepo).deleteById(eq(plate))
    }

    @Test
    fun `handleExit should do nothing if entry not found`() {
        val plate = "TEST003"
        val exitTime = Instant.now()

        whenever(entryRepo.findById(plate)).thenReturn(Optional.empty())

        garageService.handleExit(plate, exitTime)

        verify(revenueRepo, never()).save(any())
        verify(revenueRepo, never()).update(any())
        verify(spotRepo, never()).update(any())
        verify(sectorRepo, never()).update(any())
        verify(entryRepo, never()).deleteById(any())
    }

    @Test
    fun `handleExit should do nothing if spot in entry is null`() {
        val plate = "TEST003"
        val exitTime = Instant.now()
        val entryTime = Instant.now().minusSeconds(3600)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = null, parkedTime = entryTime)

        whenever(entryRepo.findById(plate)).thenReturn(Optional.of(mockEntry))

        garageService.handleExit(plate, exitTime)

        verify(revenueRepo, never()).save(any())
        verify(revenueRepo, never()).update(any())
        verify(spotRepo, never()).update(any())
        verify(sectorRepo, never()).update(any())
        verify(entryRepo, never()).deleteById(any())
    }

    @Test
    fun `postPlateStatus should return PlateStatusDTO with correct data when entry exists`() {
        val plate = "TEST005"
        val entryTime = Instant.now().minusSeconds(120 * 60)
        val parkedTime = Instant.now().minusSeconds(60 * 60)
        val mockSector = Sector(name = "D", basePrice = 5.0, maxCapacity = 10, openHour = "08", closeHour = "20", durationLimitMinutes = 300, currentOcupied = 5)
        val mockSpot = Spot(id = 4, sector = mockSector, lat = -23.0, lng = -46.0, ocupied = true)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = mockSpot, parkedTime = parkedTime)

        val expectedDto = PlateStatusDTO(
            licensePlate = plate,
            priceUntilNow = 5.0,
            entryTime = entryTime,
            timeParked = parkedTime,
            lat = mockSpot.lat,
            lng = mockSpot.lng
        )

        whenever(entryRepo.findById(plate)).thenReturn(Optional.of(mockEntry))
        whenever(objectMapper.convertValue(any<Map<String, Any?>>(), eq(PlateStatusDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.postPlateStatus(plate)

        assertThat(result).isEqualTo(expectedDto)
        verify(entryRepo).findById(eq(plate))
        verify(objectMapper).convertValue(any<Map<String, Any?>>(), eq(PlateStatusDTO::class.java))
    }

    @Test
    fun `postPlateStatus should return PlateStatusDTO with default data when entry does not exist`() {
        val plate = "NONEXISTENT"
        val expectedDto = PlateStatusDTO(
            licensePlate = null,
            priceUntilNow = 0.0,
            entryTime = null,
            timeParked = null,
            lat = null,
            lng = null
        )

        whenever(entryRepo.findById(plate)).thenReturn(Optional.empty())
        whenever(objectMapper.convertValue(any<Map<String, Any?>>(), eq(PlateStatusDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.postPlateStatus(plate)

        assertThat(result).isEqualTo(expectedDto)
        verify(entryRepo).findById(eq(plate))
        verify(objectMapper).convertValue(any<Map<String, Any?>>(), eq(PlateStatusDTO::class.java))
    }

    @Test
    fun `postSpotStatus should return SpotStatusDTO with correct data when spot and entry exist`() {
        val lat = -23.0
        val lng = -46.0
        val plate = "TEST006"
        val entryTime = Instant.now().minusSeconds(120 * 60)
        val parkedTime = Instant.now().minusSeconds(60 * 60)
        val mockSector = Sector(name = "E", basePrice = 10.0, maxCapacity = 20, openHour = "08", closeHour = "20", durationLimitMinutes = 300, currentOcupied = 15)
        val mockSpot = Spot(id = 5, sector = mockSector, lat = lat, lng = lng, ocupied = true)
        val mockEntry = VehicleEntry(licensePlate = plate, entryTime = entryTime, spot = mockSpot, parkedTime = parkedTime)

        val entries = listOf(mockEntry)

        val expectedDto = SpotStatusDTO(
            ocupied = true,
            licensePlate = plate,
            priceUntilNow = 10.0,
            entryTime = entryTime,
            timeParked = parkedTime
        )

        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(mockSpot)
        whenever(entryRepo.findAll()).thenReturn(entries)
        whenever(objectMapper.convertValue(any<Map<String, Any?>>(), eq(SpotStatusDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.postSpotStatus(lat, lng)

        assertThat(result).isEqualTo(expectedDto)
        verify(spotRepo).findByLatAndLng(eq(lat), eq(lng))
        verify(entryRepo).findAll()
        verify(objectMapper).convertValue(any<Map<String, Any?>>(), eq(SpotStatusDTO::class.java))
    }

    @Test
    fun `postSpotStatus should return SpotStatusDTO with default data when spot does not exist`() {
        val lat = -99.0
        val lng = -99.0

        val expectedDto = SpotStatusDTO(
            ocupied = false,
            licensePlate = null,
            priceUntilNow = 0.0,
            entryTime = null,
            timeParked = null
        )

        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(null)
        whenever(objectMapper.convertValue(any<Map<String, Any?>>(), eq(SpotStatusDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.postSpotStatus(lat, lng)

        assertThat(result).isEqualTo(expectedDto)
        verify(spotRepo).findByLatAndLng(eq(lat), eq(lng))
        verify(entryRepo, never()).findAll()
        verify(objectMapper).convertValue(any<Map<String, Any?>>(), eq(SpotStatusDTO::class.java))
    }

    @Test
    fun `getRevenue should return RevenueDTO with existing revenue data`() {
        val date = LocalDate.of(2024, 5, 29)
        val sector = "F"
        val existingRevenue = Revenue(id = 1L, date = date, sectorName = sector, amount = 999.99)
        val expectedDto = RevenueDTO(
            amount = 999.99,
            currency = "BRL",
            timestamp = date.atStartOfDay(ZoneId.systemDefault()).toString()
        )

        whenever(revenueRepo.findByDateAndSectorName(eq(date), eq(sector))).thenReturn(existingRevenue)
        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(RevenueDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.getRevenue(date, sector)

        assertThat(result).isEqualTo(expectedDto)
        verify(revenueRepo).findByDateAndSectorName(eq(date), eq(sector))
        verify(objectMapper).convertValue(any<Map<String, Any>>(), eq(RevenueDTO::class.java))
    }

    @Test
    fun `getRevenue should return RevenueDTO with zero amount if no revenue found`() {
        val date = LocalDate.of(2024, 5, 29)
        val sector = "G"
        val expectedDto = RevenueDTO(
            amount = 0.0,
            currency = "BRL",
            timestamp = date.atStartOfDay(ZoneId.systemDefault()).toString()
        )

        whenever(revenueRepo.findByDateAndSectorName(eq(date), eq(sector))).thenReturn(null)
        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(RevenueDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.getRevenue(date, sector)

        assertThat(result).isEqualTo(expectedDto)
        verify(revenueRepo).findByDateAndSectorName(eq(date), eq(sector))
        verify(objectMapper).convertValue(any<Map<String, Any>>(), eq(RevenueDTO::class.java))
    }

    @Test
    fun `getGarage should return GarageInfoDTO with all sectors and spots`() {
        val mockSector1 = Sector(name = "H", basePrice = 15.0, maxCapacity = 200, openHour = "08", closeHour = "20", durationLimitMinutes = 300, currentOcupied = 0)
        val mockSector2 = Sector(name = "I", basePrice = 25.0, maxCapacity = 50, openHour = "07", closeHour = "23", durationLimitMinutes = 600, currentOcupied = 0)
        val mockSpot1 = Spot(id = 6, sector = mockSector1, lat = -20.0, lng = -40.0)
        val mockSpot2 = Spot(id = 7, sector = mockSector1, lat = -20.1, lng = -40.1)
        val mockSpot3 = Spot(id = 8, sector = mockSector2, lat = -25.0, lng = -45.0)

        val allSectors = listOf(mockSector1, mockSector2)
        val allSpots = listOf(mockSpot1, mockSpot2, mockSpot3)

        val expectedDto = GarageInfoDTO(
            garage = listOf(
                SectorInfo(sector = "H", basePrice = 15.0, max_capacity = 200, open_hour = "08", close_hour = "20", duration_limit_minutes = 300),
                SectorInfo(sector = "I", basePrice = 25.0, max_capacity = 50, open_hour = "07", close_hour = "23", duration_limit_minutes = 600)
            ),
            spots = listOf(
                SpotInfo(id = 6, sector = "H", lat = -20.0, lng = -40.0),
                SpotInfo(id = 7, sector = "H", lat = -20.1, lng = -40.1),
                SpotInfo(id = 8, sector = "I", lat = -25.0, lng = -45.0)
            )
        )

        whenever(sectorRepo.findAll()).thenReturn(allSectors)
        whenever(spotRepo.findAll()).thenReturn(allSpots)
        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(GarageInfoDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.getGarage()

        assertThat(result).isEqualTo(expectedDto)
        verify(sectorRepo).findAll()
        verify(spotRepo).findAll()
        verify(objectMapper).convertValue(any<Map<String, Any>>(), eq(GarageInfoDTO::class.java))
    }

    @Test
    fun `getGarage should return empty GarageInfoDTO if no sectors or spots`() {
        val expectedDto = GarageInfoDTO(garage = emptyList(), spots = emptyList())

        whenever(sectorRepo.findAll()).thenReturn(emptyList())
        whenever(spotRepo.findAll()).thenReturn(emptyList())
        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(GarageInfoDTO::class.java))).thenReturn(expectedDto)

        val result = garageService.getGarage()

        assertThat(result).isEqualTo(expectedDto)
        verify(sectorRepo).findAll()
        verify(spotRepo).findAll()
        verify(objectMapper).convertValue(any<Map<String, Any>>(), eq(GarageInfoDTO::class.java))
    }
}