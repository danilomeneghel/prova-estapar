package com.estapar.service

import com.estapar.model.Garage
import com.estapar.model.Revenue
import com.estapar.model.Sector
import com.estapar.model.Spot
import com.estapar.repository.GarageRepository
import com.estapar.repository.RevenueRepository
import com.estapar.repository.SectorRepository
import com.estapar.repository.SpotRepository
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@MicronautTest
class ParkingServiceTest {

    @Inject
    lateinit var parkingService: ParkingService

    @MockBean(SectorRepository::class)
    open fun sectorRepository(): SectorRepository {
        return mock(SectorRepository::class.java)
    }

    @MockBean(SpotRepository::class)
    open fun spotRepository(): SpotRepository {
        return mock(SpotRepository::class.java)
    }

    @MockBean(GarageRepository::class)
    open fun garageRepository(): GarageRepository {
        return mock(GarageRepository::class.java)
    }

    @MockBean(RevenueRepository::class)
    open fun revenueRepository(): RevenueRepository {
        return mock(RevenueRepository::class.java)
    }

    @Inject
    lateinit var sectorRepo: SectorRepository
    @Inject
    lateinit var spotRepo: SpotRepository
    @Inject
    lateinit var garageRepo: GarageRepository
    @Inject
    lateinit var revenueRepo: RevenueRepository

    @BeforeEach
    fun setup() {
        reset(sectorRepo, spotRepo, garageRepo, revenueRepo)
    }

    @Test
    fun `registerEntry should save new garage entry if plate does not exist`() {
        val plate = "ABC-1234"
        val entryTime = Instant.now()
        `when`(garageRepo.existsById(plate)).thenReturn(false)
        `when`(garageRepo.save(any<Garage>())).thenReturn(Garage(plate, entryTime))

        parkingService.registerEntry(plate, entryTime)

        verify(garageRepo, times(1)).save(any<Garage>())
    }

    @Test
    fun `registerEntry should not save new garage entry if plate already exists`() {
        val plate = "ABC-1234"
        val entryTime = Instant.now()
        `when`(garageRepo.existsById(plate)).thenReturn(true)

        parkingService.registerEntry(plate, entryTime)

        verify(garageRepo, never()).save(any<Garage>())
    }

    @Test
    fun `assignSpot should assign spot and update entry and sector`() {
        val plate = "ABC-1234"
        val lat = 10.0
        val lng = 20.0
        val entryTime = Instant.now().minusSeconds(3600)
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 0)
        val spot = Spot(1L, lat, lng, false, sector)
        val entry = Garage(plate, entryTime, null, null, null, "ENTRY")

        println("AssignSpot Test - Initial sector.currentOcupied: ${sector.currentOcupied}")

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)
        `when`(spotRepo.save(any<Spot>())).thenAnswer { invocation ->
            val savedSpot = invocation.arguments[0] as Spot
            savedSpot.ocupied = true
            savedSpot
        }
        `when`(garageRepo.save(any<Garage>())).thenAnswer { invocation ->
            val savedGarage = invocation.arguments[0] as Garage
            savedGarage.parkedTime = Instant.now()
            savedGarage.spot = spot
            savedGarage.status = "PARKED"
            savedGarage
        }
        `when`(sectorRepo.save(any<Sector>())).thenAnswer { invocation ->
            val savedSector = invocation.arguments[0] as Sector
            savedSector.currentOcupied++
            savedSector
        }

        parkingService.assignSpot(plate, lat, lng)

        println("AssignSpot Test - After service call sector.currentOcupied: ${sector.currentOcupied}")

        assertTrue(spot.ocupied, "Spot should be occupied")
        assertEquals("PARKED", entry.status, "Entry status should be PARKED")
        assertEquals(spot, entry.spot, "Entry spot should be the assigned spot")

        verify(spotRepo, times(1)).save(spot)
        verify(garageRepo, times(1)).save(entry)
        verify(sectorRepo, times(1)).save(sector)
    }

    @Test
    fun `assignSpot should not assign spot if vehicle entry not found`() {
        val plate = "ABC-1234"
        val lat = 10.0
        val lng = 20.0

        `when`(garageRepo.findById(plate)).thenReturn(Optional.empty())

        parkingService.assignSpot(plate, lat, lng)

        verify(spotRepo, never()).findByLatAndLng(anyDouble(), anyDouble())
        verify(spotRepo, never()).save(any<Spot>())
        verify(garageRepo, never()).save(any<Garage>())
        verify(sectorRepo, never()).save(any<Sector>())
    }

    @Test
    fun `assignSpot should not assign spot if spot not found`() {
        val plate = "ABC-1234"
        val lat = 10.0
        val lng = 20.0
        val entry = Garage(plate, Instant.now())

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(null)

        parkingService.assignSpot(plate, lat, lng)

        verify(spotRepo, times(1)).findByLatAndLng(lat, lng)
        verify(spotRepo, never()).save(any<Spot>())
        verify(garageRepo, never()).save(any<Garage>())
        verify(sectorRepo, never()).save(any<Sector>())
    }

    @Test
    fun `assignSpot should not assign spot if sector is at max capacity`() {
        val plate = "ABC-1234"
        val lat = 10.0
        val lng = 20.0
        val entry = Garage(plate, Instant.now())
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 5)
        val spot = Spot(1L, lat, lng, false, sector)

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)

        parkingService.assignSpot(plate, lat, lng)

        verify(spotRepo, never()).save(any<Spot>())
        verify(garageRepo, never()).save(any<Garage>())
        verify(sectorRepo, never()).save(any<Sector>())
    }

    @Test
    fun `assignSpot should not assign spot if spot is already occupied`() {
        val plate = "ABC-1234"
        val lat = 10.0
        val lng = 20.0
        val entry = Garage(plate, Instant.now())
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 0)
        val spot = Spot(1L, lat, lng, true, sector)

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)

        parkingService.assignSpot(plate, lat, lng)

        verify(spotRepo, never()).save(any<Spot>())
        verify(garageRepo, never()).save(any<Garage>())
        verify(sectorRepo, never()).save(any<Sector>())
    }

    @Test
    fun `handleExit should process exit, calculate revenue, release spot and update counts`() {
        val plate = "ABC-1234"
        val entryTime = Instant.now().minusSeconds(7200)
        val parkedTime = Instant.now().minusSeconds(3600)
        val exitTime = Instant.now()
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 3)
        val spot = Spot(1L, 10.0, 20.0, true, sector)
        val entry = Garage(plate, entryTime, parkedTime, spot, null, "PARKED")
        val existingRevenue = Revenue(1L, "A", 50.0, LocalDate.now())

        println("HandleExit Test - Initial existingRevenue.amount: ${existingRevenue.amount}")
        println("HandleExit Test - Initial sector.currentOcupied: ${sector.currentOcupied}")
        println("HandleExit Test - Initial spot.ocupied: ${spot.ocupied}")
        println("HandleExit Test - Initial entry.status: ${entry.status}")
        println("HandleExit Test - Initial entry.spot: ${entry.spot}")

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(revenueRepo.findByDateAndSectorName(any<LocalDate>(), eq(sector.name))).thenReturn(existingRevenue)

        `when`(revenueRepo.save(any<Revenue>())).thenAnswer { invocation ->
            val savedRevenue = invocation.arguments[0] as Revenue
            savedRevenue
        }
        `when`(spotRepo.save(any<Spot>())).thenAnswer { invocation ->
            val savedSpot = invocation.arguments[0] as Spot
            savedSpot.ocupied = false
            savedSpot
        }
        `when`(sectorRepo.save(any<Sector>())).thenAnswer { invocation ->
            val savedSector = invocation.arguments[0] as Sector
            savedSector.currentOcupied--
            savedSector
        }
        `when`(garageRepo.save(any<Garage>())).thenAnswer { invocation ->
            val savedGarage = invocation.arguments[0] as Garage
            savedGarage.exitTime = exitTime
            savedGarage.status = "EXIT"
            savedGarage.spot = null
            savedGarage
        }

        parkingService.handleExit(plate, exitTime)

        println("HandleExit Test - After service call existingRevenue.amount: ${existingRevenue.amount}")
        println("HandleExit Test - After service call sector.currentOcupied: ${sector.currentOcupied}")
        println("HandleExit Test - After service call spot.ocupied: ${spot.ocupied}")
        println("HandleExit Test - After service call entry.status: ${entry.status}")
        println("HandleExit Test - After service call entry.spot: ${entry.spot}")

        assertFalse(spot.ocupied, "Spot should be unoccupied")
        assertEquals("EXIT", entry.status, "Entry status should be EXIT")
        assertNull(entry.spot, "Entry spot should be nullified")

        verify(revenueRepo, times(1)).save(existingRevenue)
        verify(spotRepo, times(1)).save(spot)
        verify(sectorRepo, times(1)).save(sector)
        verify(garageRepo, times(1)).save(entry)

        assertTrue(existingRevenue.amount > 50.0, "Revenue amount should be greater than 50.0 after exit calculation")
    }

    @Test
    fun `handleExit should create new revenue entry if none exists for date and sector`() {
        val plate = "ABC-1234"
        val entryTime = Instant.now().minusSeconds(7200)
        val parkedTime = Instant.now().minusSeconds(3600)
        val exitTime = Instant.now()
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 3)
        val spot = Spot(1L, 10.0, 20.0, true, sector)
        val entry = Garage(plate, entryTime, parkedTime, spot, null, "PARKED")

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(revenueRepo.findByDateAndSectorName(any<LocalDate>(), eq(sector.name))).thenReturn(null)
        `when`(revenueRepo.save(any<Revenue>())).thenAnswer { invocation ->
            invocation.arguments[0] as Revenue
        }
        `when`(spotRepo.save(any<Spot>())).thenAnswer { invocation ->
            val s = invocation.arguments[0] as Spot
            s.ocupied = false
            s
        }
        `when`(sectorRepo.save(any<Sector>())).thenAnswer { invocation ->
            val sec = invocation.arguments[0] as Sector
            sec.currentOcupied--
            sec
        }
        `when`(garageRepo.save(any<Garage>())).thenAnswer { invocation ->
            val g = invocation.arguments[0] as Garage
            g.exitTime = exitTime
            g.status = "EXIT"
            g.spot = null
            g
        }

        parkingService.handleExit(plate, exitTime)

        verify(revenueRepo, times(1)).save(any<Revenue>())
        verify(spotRepo, times(1)).save(spot)
        verify(sectorRepo, times(1)).save(sector)
    }

    @Test
    fun `handleExit should not process if plate is blank`() {
        val plate = ""
        val exitTime = Instant.now()

        parkingService.handleExit(plate, exitTime)

        verify(garageRepo, never()).findById(anyString())
    }

    @Test
    fun `handleExit should not process if vehicle entry not found`() {
        val plate = "ABC-1234"
        val exitTime = Instant.now()

        `when`(garageRepo.findById(plate)).thenReturn(Optional.empty())

        parkingService.handleExit(plate, exitTime)

        verify(spotRepo, never()).save(any<Spot>())
        verify(sectorRepo, never()).save(any<Sector>())
        verify(revenueRepo, never()).save(any<Revenue>())
    }

    @Test
    fun `handleExit should update entry status even if spot is null`() {
        val plate = "ABC-1234"
        val entryTime = Instant.now().minusSeconds(3600)
        val exitTime = Instant.now()
        val entry = Garage(plate, entryTime, null, null, null, "ENTRY")

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))
        `when`(garageRepo.save(any<Garage>())).thenAnswer { invocation ->
            val g = invocation.arguments[0] as Garage
            g.exitTime = exitTime
            g.status = "EXIT"
            g.spot = null
            g
        }

        parkingService.handleExit(plate, exitTime)

        assertEquals("EXIT", entry.status)
        assertNull(entry.spot)
        verify(spotRepo, never()).save(any<Spot>())
        verify(sectorRepo, never()).save(any<Sector>())
        verify(revenueRepo, never()).save(any<Revenue>())
        verify(garageRepo, times(1)).save(entry)
    }

    @Test
    fun `postPlateStatus should return correct status for parked vehicle`() {
        val plate = "ABC-1234"
        val entryTime = Instant.now().minusSeconds(3600)
        val parkedTime = Instant.now().minusSeconds(1800)
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 1)
        val spot = Spot(1L, 10.0, 20.0, true, sector)
        val entry = Garage(plate, entryTime, parkedTime, spot, null, "PARKED")

        `when`(garageRepo.findById(plate)).thenReturn(Optional.of(entry))

        val result = parkingService.postPlateStatus(plate)

        assertNotNull(result)
        assertEquals(plate, result.licensePlate)
        assertTrue(result.priceUntilNow > 0.0)
        assertEquals(entryTime, result.entryTime)
        assertEquals(parkedTime, result.timeParked)
        assertEquals(spot.lat, result.lat)
        assertEquals(spot.lng, result.lng)
    }

    @Test
    fun `postPlateStatus should return default status for non-existent vehicle`() {
        val plate = "XYZ-9876"

        `when`(garageRepo.findById(plate)).thenReturn(Optional.empty())

        val result = parkingService.postPlateStatus(plate)

        assertNotNull(result)
        assertNull(result.licensePlate)
        assertEquals(0.0, result.priceUntilNow)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
        assertNull(result.lat)
        assertNull(result.lng)
    }

    @Test
    fun `postSpotStatus should return correct status for occupied spot`() {
        val lat = 10.0
        val lng = 20.0
        val plate = "ABC-1234"
        val entryTime = Instant.now().minusSeconds(3600)
        val parkedTime = Instant.now().minusSeconds(1800)
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 1)
        val spot = Spot(1L, lat, lng, true, sector)
        val entry = Garage(plate, entryTime, parkedTime, spot, null, "PARKED")

        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)
        `when`(garageRepo.findBySpotAndStatus(eq(spot), eq("PARKED"))).thenReturn(Optional.of(entry))

        val result = parkingService.postSpotStatus(lat, lng)

        assertNotNull(result)
        assertTrue(result.ocupied)
        assertEquals(plate, result.licensePlate)
        assertTrue(result.priceUntilNow > 0.0)
        assertEquals(entryTime, result.entryTime)
        assertEquals(parkedTime, result.timeParked)
    }

    @Test
    fun `postSpotStatus should return correct status for unoccupied spot`() {
        val lat = 10.0
        val lng = 20.0
        val sector = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 0)
        val spot = Spot(1L, lat, lng, false, sector)

        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(spot)
        `when`(garageRepo.findBySpotAndStatus(eq(spot), eq("PARKED"))).thenReturn(Optional.empty())

        val result = parkingService.postSpotStatus(lat, lng)

        assertNotNull(result)
        assertFalse(result.ocupied)
        assertNull(result.licensePlate)
        assertEquals(0.0, result.priceUntilNow)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
    }

    @Test
    fun `postSpotStatus should return default status for non-existent spot`() {
        val lat = 10.0
        val lng = 20.0

        `when`(spotRepo.findByLatAndLng(lat, lng)).thenReturn(null)

        val result = parkingService.postSpotStatus(lat, lng)

        assertNotNull(result)
        assertFalse(result.ocupied)
        assertNull(result.licensePlate)
        assertEquals(0.0, result.priceUntilNow)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
    }

    @Test
    fun `getRevenue should return correct revenue for existing date and sector`() {
        val date = LocalDate.now()
        val sectorName = "A"
        val revenueAmount = 150.75
        val revenue = Revenue(1L, sectorName, revenueAmount, date)

        `when`(revenueRepo.findByDateAndSectorName(date, sectorName)).thenReturn(revenue)

        val result = parkingService.getRevenue(date, sectorName)

        assertNotNull(result)
        assertEquals(revenueAmount, result.amount)
        assertEquals("BRL", result.currency)
        assertEquals(date.atStartOfDay(ZoneId.systemDefault()).toString(), result.timestamp)
    }

    @Test
    fun `getRevenue should return zero amount for non-existent revenue`() {
        val date = LocalDate.now()
        val sectorName = "B"

        `when`(revenueRepo.findByDateAndSectorName(date, sectorName)).thenReturn(null)

        val result = parkingService.getRevenue(date, sectorName)

        assertNotNull(result)
        assertEquals(0.0, result.amount)
        assertEquals("BRL", result.currency)
        assertEquals(date.atStartOfDay(ZoneId.systemDefault()).toString(), result.timestamp)
    }

    @Test
    fun `getGarage should return all sectors and spots info`() {
        val sector1 = Sector(1L, "A", 10.0, 5, "08:00", "18:00", 240, 0)
        val sector2 = Sector(2L, "B", 12.0, 10, "07:00", "19:00", 300, 0)
        val spot1 = Spot(1L, 10.0, 20.0, false, sector1)
        val spot2 = Spot(2L, 30.0, 40.0, true, sector2)

        `when`(sectorRepo.findAll()).thenReturn(listOf(sector1, sector2))
        `when`(spotRepo.findAll()).thenReturn(listOf(spot1, spot2))

        val result = parkingService.getGarage()

        assertNotNull(result)
        assertEquals(2, result.garage.size)
        assertEquals(2, result.spots.size)

        assertEquals("A", result.garage[0].sector)
        assertEquals(10.0, result.garage[0].basePrice)
        assertEquals(5, result.garage[0].maxCapacity)

        assertEquals("B", result.garage[1].sector)
        assertEquals(12.0, result.garage[1].basePrice)
        assertEquals(10, result.garage[1].maxCapacity)

        assertEquals(1L, result.spots[0].id)
        assertEquals("A", result.spots[0].sector)
        assertEquals(10.0, result.spots[0].lat)
        assertEquals(20.0, result.spots[0].lng)
        assertFalse(result.spots[0].occupied)

        assertEquals(2L, result.spots[1].id)
        assertEquals("B", result.spots[1].sector)
        assertEquals(30.0, result.spots[1].lat)
        assertEquals(40.0, result.spots[1].lng)
        assertTrue(result.spots[1].occupied)
    }

    @Test
    fun `getGarage should return empty lists if no sectors or spots exist`() {
        `when`(sectorRepo.findAll()).thenReturn(emptyList())
        `when`(spotRepo.findAll()).thenReturn(emptyList())

        val result = parkingService.getGarage()

        assertNotNull(result)
        assertTrue(result.garage.isEmpty())
        assertTrue(result.spots.isEmpty())
    }
}