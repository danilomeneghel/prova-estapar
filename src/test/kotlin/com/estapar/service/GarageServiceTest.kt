package com.estapar.service

import com.estapar.dto.*
import com.estapar.model.Garage
import com.estapar.model.Revenue
import com.estapar.model.Sector
import com.estapar.model.Spot
import com.estapar.repository.GarageRepository
import com.estapar.repository.RevenueRepository
import com.estapar.repository.SectorRepository
import com.estapar.repository.SpotRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import org.junit.jupiter.api.Assertions.*
import com.fasterxml.jackson.databind.ObjectMapper

@ExtendWith(MockitoExtension::class)
class GarageServiceTest {

    @Mock
    private lateinit var sectorRepo: SectorRepository

    @Mock
    private lateinit var spotRepo: SpotRepository

    @Mock
    private lateinit var garageRepo: GarageRepository

    @Mock
    private lateinit var revenueRepo: RevenueRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var garageService: GarageService

    @Test
    fun registerEntryShouldSaveNewEntryWhenNotExists() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()
        val newEntry = Garage(licensePlate, entryTime)

        whenever(garageRepo.existsById(licensePlate)).thenReturn(false)
        whenever(garageRepo.save(any<Garage>())).thenReturn(newEntry)

        garageService.registerEntry(licensePlate, entryTime)

        verify(garageRepo).existsById(licensePlate)
        verify(garageRepo).save(argThat<Garage> { garage -> garage.licensePlate == licensePlate && garage.entryTime == entryTime })
    }

    @Test
    fun registerEntryShouldNotSaveWhenEntryAlreadyExists() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()

        whenever(garageRepo.existsById(licensePlate)).thenReturn(true)

        garageService.registerEntry(licensePlate, entryTime)

        verify(garageRepo).existsById(licensePlate)
        verify(garageRepo, never()).save(any())
    }

    @Test
    fun assignSpotShouldAssignSpotAndUpdateSectorAndGarage() {
        val licensePlate = "ABC1234"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now().minusSeconds(3600)
        val existingGarage = Garage(licensePlate, entryTime)
        val sector = Sector(
            id = 1L,
            name = "A",
            basePrice = 10.0,
            maxCapacity = 10,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240,
            currentOcupied = 0
        )
        val spot = Spot(id = 100L, lat = lat, lng = lng, ocupied = false, sector = sector)

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(existingGarage))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)
        whenever(spotRepo.save(any<Spot>())).thenReturn(spot.copy(ocupied = true))
        whenever(garageRepo.save(any<Garage>())).thenReturn(existingGarage.copy(spot = spot, status = "PARKED"))
        whenever(sectorRepo.save(any<Sector>())).thenReturn(sector.copy(currentOcupied = 1))

        garageService.assignSpot(licensePlate, lat, lng)

        assertTrue(spot.ocupied)
        assertEquals("PARKED", existingGarage.status)
        assertEquals(spot, existingGarage.spot)
        assertEquals(1, sector.currentOcupied)

        verify(garageRepo).findById(licensePlate)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(spotRepo).save(argThat<Spot> { s -> s.id == spot.id && s.ocupied })
        verify(garageRepo).save(argThat<Garage> { g -> g.licensePlate == licensePlate && g.status == "PARKED" && g.spot == spot })
        verify(sectorRepo).save(argThat<Sector> { s -> s.id == sector.id && s.currentOcupied == 1 })
    }

    @Test
    fun assignSpotShouldNotAssignWhenEntryNotFound() {
        val licensePlate = "ABC1234"
        val lat = 10.0
        val lng = 20.0

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.empty())

        garageService.assignSpot(licensePlate, lat, lng)

        verify(garageRepo).findById(licensePlate)
        verify(spotRepo, never()).findByLatAndLng(any(), any())
        verify(spotRepo, never()).save(any())
        verify(garageRepo, never()).save(any())
        verify(sectorRepo, never()).save(any())
    }

    @Test
    fun assignSpotShouldNotAssignWhenSpotNotFound() {
        val licensePlate = "ABC1234"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now()
        val existingGarage = Garage(licensePlate, entryTime)

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(existingGarage))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(null)

        garageService.assignSpot(licensePlate, lat, lng)

        verify(garageRepo).findById(licensePlate)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(spotRepo, never()).save(any())
        verify(garageRepo, never()).save(any())
        verify(sectorRepo, never()).save(any())
    }

    @Test
    fun assignSpotShouldNotAssignWhenSectorIsAtMaxCapacity() {
        val licensePlate = "ABC1234"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now()
        val existingGarage = Garage(licensePlate, entryTime)
        val sector = Sector(
            id = 1L,
            name = "A",
            basePrice = 10.0,
            maxCapacity = 10,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240,
            currentOcupied = 10
        )
        val spot = Spot(id = 100L, lat = lat, lng = lng, ocupied = false, sector = sector)

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(existingGarage))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)

        garageService.assignSpot(licensePlate, lat, lng)

        verify(garageRepo).findById(licensePlate)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(spotRepo, never()).save(any())
        verify(garageRepo, never()).save(any())
        verify(sectorRepo, never()).save(any())
    }

    @Test
    fun assignSpotShouldNotAssignWhenSpotAlreadyOccupied() {
        val licensePlate = "ABC1234"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now()
        val existingGarage = Garage(licensePlate, entryTime)
        val sector = Sector(
            id = 1L,
            name = "A",
            basePrice = 10.0,
            maxCapacity = 10,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240,
            currentOcupied = 5
        )
        val spot = Spot(id = 100L, lat = lat, lng = lng, ocupied = true, sector = sector)

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(existingGarage))
        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)

        garageService.assignSpot(licensePlate, lat, lng)

        verify(garageRepo).findById(licensePlate)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(spotRepo, never()).save(any())
        verify(garageRepo, never()).save(any())
        verify(sectorRepo, never()).save(any())
    }

    @Test
    fun handleExitShouldProcessExitAndCalculateRevenue() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now().minusSeconds(7200)
        val parkedTime = Instant.now().minusSeconds(3600)
        val exitTime = Instant.now()
        val sector = Sector(
            id = 1L,
            name = "Setor A",
            basePrice = 10.0,
            maxCapacity = 10,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240,
            currentOcupied = 5
        )
        val spot = Spot(id = 100L, lat = 10.0, lng = 20.0, ocupied = true, sector = sector)
        val existingGarage = Garage(licensePlate, entryTime, parkedTime, spot, null, "PARKED")
        val today = LocalDate.ofInstant(exitTime, ZoneId.systemDefault())

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(existingGarage))
        whenever(revenueRepo.findByDateAndSectorName(today, sector.name)).thenReturn(null)
        whenever(revenueRepo.save(any<Revenue>())).thenReturn(Revenue(date = today, sectorName = sector.name, amount = 10.0))
        whenever(spotRepo.save(any<Spot>())).thenReturn(spot.copy(ocupied = false))
        whenever(sectorRepo.save(any<Sector>())).thenReturn(sector.copy(currentOcupied = 4))
        whenever(garageRepo.save(any<Garage>())).thenReturn(existingGarage.copy(exitTime = exitTime, status = "EXIT", spot = null))

        garageService.handleExit(licensePlate, exitTime)

        verify(garageRepo).findById(licensePlate)
        verify(revenueRepo).findByDateAndSectorName(today, sector.name)
        verify(revenueRepo).save(argThat<Revenue> { revenue -> revenue.amount > 0.0 && revenue.sectorName == sector.name && revenue.date == today })
        verify(spotRepo).save(argThat<Spot> { s -> s.id == spot.id && !s.ocupied })
        verify(sectorRepo).save(argThat<Sector> { s -> s.id == sector.id && s.currentOcupied == 4 })
        verify(garageRepo).save(argThat<Garage> { g -> g.licensePlate == licensePlate && g.status == "EXIT" && g.exitTime == exitTime && g.spot == null })
    }

    @Test
    fun handleExitShouldUpdateExistingRevenue() {
        val licensePlate = "DEF5678"
        val entryTime = Instant.now().minusSeconds(7200)
        val parkedTime = Instant.now().minusSeconds(3600)
        val exitTime = Instant.now()
        val sector = Sector(
            id = 2L,
            name = "Setor B",
            basePrice = 15.0,
            maxCapacity = 20,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240,
            currentOcupied = 10
        )
        val spot = Spot(id = 200L, lat = 30.0, lng = 40.0, ocupied = true, sector = sector)
        val existingGarage = Garage(licensePlate, entryTime, parkedTime, spot, null, "PARKED")
        val today = LocalDate.ofInstant(exitTime, ZoneId.systemDefault())
        val existingRevenue = Revenue(date = today, sectorName = sector.name, amount = 50.0)

        val parkedDurationMinutes = (exitTime.toEpochMilli() - parkedTime.toEpochMilli()) / 60000.0
        val pricePerHour = sector.basePrice
        val totalCalculatedPrice = (parkedDurationMinutes / 60.0) * pricePerHour
        val lotPercent = sector.currentOcupied.toDouble() / sector.maxCapacity

        val expectedAdditionalRevenue = when {
            lotPercent < 0.25 -> totalCalculatedPrice * 0.9
            lotPercent < 0.5 -> totalCalculatedPrice
            lotPercent < 0.75 -> totalCalculatedPrice * 1.1
            else -> totalCalculatedPrice * 1.25
        }

        val expectedTotalRevenueAmount = existingRevenue.amount + expectedAdditionalRevenue

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(existingGarage))
        whenever(revenueRepo.findByDateAndSectorName(today, sector.name)).thenReturn(existingRevenue)
        whenever(revenueRepo.save(any<Revenue>())).thenReturn(existingRevenue.copy(amount = expectedTotalRevenueAmount))
        whenever(spotRepo.save(any<Spot>())).thenReturn(spot.copy(ocupied = false))
        whenever(sectorRepo.save(any<Sector>())).thenReturn(sector.copy(currentOcupied = sector.currentOcupied - 1))
        whenever(garageRepo.save(any<Garage>())).thenReturn(existingGarage.copy(exitTime = exitTime, status = "EXIT", spot = null))

        garageService.handleExit(licensePlate, exitTime)

        verify(revenueRepo).findByDateAndSectorName(today, sector.name)
        verify(revenueRepo).save(argThat<Revenue> { revenue ->
            revenue.amount == expectedTotalRevenueAmount && revenue.sectorName == sector.name && revenue.date == today
        })
        verify(spotRepo).save(argThat<Spot> { s -> s.id == spot.id && !s.ocupied })
        verify(sectorRepo).save(argThat<Sector> { s -> s.id == sector.id && s.currentOcupied == 9 })
        verify(garageRepo).save(argThat<Garage> { g -> g.licensePlate == licensePlate && g.status == "EXIT" && g.exitTime == exitTime && g.spot == null })
    }

    @Test
    fun handleExitShouldNotProcessWhenEntryNotFound() {
        val licensePlate = "XYZ9876"
        val exitTime = Instant.now()

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.empty())

        garageService.handleExit(licensePlate, exitTime)

        verify(garageRepo).findById(licensePlate)
        verify(revenueRepo, never()).findByDateAndSectorName(any(), any())
        verify(revenueRepo, never()).save(any())
        verify(spotRepo, never()).save(any())
        verify(sectorRepo, never()).save(any())
        verify(garageRepo, never()).save(any())
    }

    @Test
    fun postPlateStatusShouldReturnDTOWithCorrectInfoWhenFoundAndParked() {
        val licensePlate = "PARKED123"
        val entryTime = Instant.now().minusSeconds(7200)
        val parkedTime = Instant.now().minusSeconds(3600)
        val sector = Sector(name = "Setor X", basePrice = 20.0, maxCapacity = 10, openHour = "08:00", closeHour = "22:00", durationLimitMinutes = 240, currentOcupied = 5)
        val spot = Spot(lat = 10.0, lng = 20.0, ocupied = true, sector = sector)
        val garageEntry = Garage(licensePlate, entryTime, parkedTime, spot, null, "PARKED")

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(garageEntry))

        val result = garageService.postPlateStatus(licensePlate)

        assertEquals(licensePlate, result.licensePlate)
        assertTrue(result.priceUntilNow > 0.0)
        assertEquals(entryTime, result.entryTime)
        assertEquals(parkedTime, result.timeParked)
        assertEquals(spot.lat, result.lat)
        assertEquals(spot.lng, result.lng)
        verify(garageRepo).findById(licensePlate)
    }

    @Test
    fun postPlateStatusShouldReturnDTOWithCorrectInfoWhenFoundAndOnlyEntered() {
        val licensePlate = "ENTERED123"
        val entryTime = Instant.now().minusSeconds(7200)
        val sector = Sector(name = "Setor Y", basePrice = 10.0, maxCapacity = 10, openHour = "08:00", closeHour = "22:00", durationLimitMinutes = 240, currentOcupied = 0)
        val spot = Spot(lat = 30.0, lng = 40.0, ocupied = false, sector = sector)
        val garageEntry = Garage(licensePlate, entryTime, null, spot, null, "ENTRY")

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.of(garageEntry))

        val result = garageService.postPlateStatus(licensePlate)

        assertEquals(licensePlate, result.licensePlate)
        assertTrue(result.priceUntilNow > 0.0)
        assertEquals(entryTime, result.entryTime)
        assertNull(result.timeParked)
        assertEquals(spot.lat, result.lat)
        assertEquals(spot.lng, result.lng)
        verify(garageRepo).findById(licensePlate)
    }

    @Test
    fun postPlateStatusShouldReturnEmptyDTOWhenNotFound() {
        val licensePlate = "NOTFOUND"

        whenever(garageRepo.findById(licensePlate)).thenReturn(Optional.empty())

        val result = garageService.postPlateStatus(licensePlate)

        assertNull(result.licensePlate)
        assertEquals(0.0, result.priceUntilNow)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
        assertNull(result.lat)
        assertNull(result.lng)
        verify(garageRepo).findById(licensePlate)
    }

    @Test
    fun postSpotStatusShouldReturnDTOWithCorrectInfoWhenOccupied() {
        val lat = 10.0
        val lng = 20.0
        val licensePlate = "ABC1234"
        val entryTime = Instant.now().minusSeconds(7200)
        val parkedTime = Instant.now().minusSeconds(3600)
        val sector = Sector(name = "Setor X", basePrice = 20.0, maxCapacity = 10, openHour = "08:00", closeHour = "22:00", durationLimitMinutes = 240, currentOcupied = 5)
        val spot = Spot(lat = lat, lng = lng, ocupied = true, sector = sector)
        val garageEntry = Garage(licensePlate, entryTime, parkedTime, spot, null, "PARKED")

        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)
        whenever(garageRepo.findBySpotAndStatus(spot, "PARKED")).thenReturn(Optional.of(garageEntry))

        val result = garageService.postSpotStatus(lat, lng)

        assertTrue(result.ocupied)
        assertEquals(licensePlate, result.licensePlate)
        assertTrue(result.priceUntilNow > 0.0)
        assertEquals(entryTime, result.entryTime)
        assertEquals(parkedTime, result.timeParked)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(garageRepo).findBySpotAndStatus(spot, "PARKED")
    }

    @Test
    fun postSpotStatusShouldReturnDTOWhenSpotNotOccupied() {
        val lat = 10.0
        val lng = 20.0
        val sector = Sector(name = "Setor X", basePrice = 20.0, maxCapacity = 10, openHour = "08:00", closeHour = "22:00", durationLimitMinutes = 240, currentOcupied = 5)
        val spot = Spot(lat = lat, lng = lng, ocupied = false, sector = sector)

        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)
        whenever(garageRepo.findBySpotAndStatus(spot, "PARKED")).thenReturn(Optional.empty())

        val result = garageService.postSpotStatus(lat, lng)

        assertFalse(result.ocupied)
        assertNull(result.licensePlate)
        assertEquals(0.0, result.priceUntilNow)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(garageRepo).findBySpotAndStatus(spot, "PARKED")
    }

    @Test
    fun postSpotStatusShouldReturnEmptyDTOWhenSpotNotFound() {
        val lat = 99.0
        val lng = 99.0

        whenever(spotRepo.findByLatAndLng(lat, lng)).thenReturn(null)

        val result = garageService.postSpotStatus(lat, lng)

        assertFalse(result.ocupied)
        assertNull(result.licensePlate)
        assertEquals(0.0, result.priceUntilNow)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
        verify(spotRepo).findByLatAndLng(lat, lng)
        verify(garageRepo, never()).findBySpotAndStatus(any(), any())
    }

    @Test
    fun getRevenueShouldReturnRevenueDTOWhenFound() {
        val date = LocalDate.now()
        val sectorName = "Setor C"
        val expectedAmount = 150.75
        val revenue = Revenue(date = date, sectorName = sectorName, amount = expectedAmount)

        whenever(revenueRepo.findByDateAndSectorName(date, sectorName)).thenReturn(revenue)

        val result = garageService.getRevenue(date, sectorName)

        assertEquals(expectedAmount, result.amount, 0.001)
        assertEquals("BRL", result.currency)
        assertNotNull(result.timestamp)
        verify(revenueRepo).findByDateAndSectorName(date, sectorName)
    }

    @Test
    fun getRevenueShouldReturnZeroAmountWhenNotFound() {
        val date = LocalDate.now()
        val sectorName = "Setor Inexistente"

        whenever(revenueRepo.findByDateAndSectorName(date, sectorName)).thenReturn(null)

        val result = garageService.getRevenue(date, sectorName)

        assertEquals(0.0, result.amount, 0.001)
        assertEquals("BRL", result.currency)
        assertNotNull(result.timestamp)
        verify(revenueRepo).findByDateAndSectorName(date, sectorName)
    }

    @Test
    fun getGarageShouldReturnGarageInfoDTOWithAllSectorsAndSpots() {
        val sector1 = Sector(id = 1L, name = "Setor A", basePrice = 10.0, maxCapacity = 100, openHour = "08:00", closeHour = "22:00", durationLimitMinutes = 240, currentOcupied = 10)
        val sector2 = Sector(id = 2L, name = "Setor B", basePrice = 15.0, maxCapacity = 50, openHour = "07:00", closeHour = "23:00", durationLimitMinutes = 300, currentOcupied = 5)
        val spot1 = Spot(id = 101L, lat = -23.5, lng = -46.6, ocupied = false, sector = sector1)
        val spot2 = Spot(id = 102L, lat = -23.6, lng = -46.7, ocupied = true, sector = sector2)

        whenever(sectorRepo.findAll()).thenReturn(listOf(sector1, sector2))
        whenever(spotRepo.findAll()).thenReturn(listOf(spot1, spot2))

        val result = garageService.getGarage()

        assertEquals(2, result.garage.size)
        assertEquals(2, result.spots.size)

        val sectorInfo1 = result.garage.find { it.sector == "Setor A" }
        assertNotNull(sectorInfo1)
        assertEquals(10.0, sectorInfo1!!.basePrice)
        assertEquals(100, sectorInfo1.maxCapacity)

        val spotInfo1 = result.spots.find { it.id == 101L }
        assertNotNull(spotInfo1)
        assertEquals("Setor A", spotInfo1!!.sector)
        assertEquals(-23.5, spotInfo1.lat)
        assertEquals(-46.6, spotInfo1.lng)

        verify(sectorRepo).findAll()
        verify(spotRepo).findAll()
    }
}