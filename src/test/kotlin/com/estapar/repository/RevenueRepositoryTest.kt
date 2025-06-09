package com.estapar.repository

import com.estapar.model.Revenue
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@MicronautTest
class RevenueRepositoryTest {

    @Inject
    lateinit var revenueRepository: RevenueRepository

    @BeforeEach
    fun setup() {
        revenueRepository.deleteAll()
    }

    @Test
    fun `should save and find revenue entry`() {
        val date = LocalDate.of(2023, 1, 15)
        val revenue = Revenue(sectorName = "Sector A", amount = 150.75, date = date)
        val savedRevenue = revenueRepository.save(revenue)

        assertNotNull(savedRevenue.id)
        assertEquals("Sector A", savedRevenue.sectorName)
        assertEquals(150.75, savedRevenue.amount, 0.001)
        assertEquals(date, savedRevenue.date)

        val foundRevenue = revenueRepository.findById(savedRevenue.id!!).orElse(null)
        assertNotNull(foundRevenue)
        assertEquals(savedRevenue.id, foundRevenue.id)
        assertEquals("Sector A", foundRevenue.sectorName)
    }

    @Test
    fun `should update revenue entry`() {
        val date = LocalDate.of(2023, 2, 10)
        val revenue = Revenue(sectorName = "Sector B", amount = 200.00, date = date)
        val savedRevenue = revenueRepository.save(revenue)

        savedRevenue.amount = 250.50
        val updatedRevenue = revenueRepository.update(savedRevenue)

        assertNotNull(updatedRevenue)
        assertEquals(savedRevenue.id, updatedRevenue.id)
        assertEquals(250.50, updatedRevenue.amount, 0.001)

        val foundRevenue = revenueRepository.findById(savedRevenue.id!!).orElse(null)
        assertNotNull(foundRevenue)
        assertEquals(250.50, foundRevenue.amount, 0.001)
    }

    @Test
    fun `should find by date and sector name`() {
        val date1 = LocalDate.of(2023, 3, 5)
        val date2 = LocalDate.of(2023, 3, 6)
        val revenue1 = Revenue(sectorName = "Sector C", amount = 300.00, date = date1)
        val revenue2 = Revenue(sectorName = "Sector C", amount = 400.00, date = date2)
        val revenue3 = Revenue(sectorName = "Sector D", amount = 500.00, date = date1)

        revenueRepository.saveAll(listOf(revenue1, revenue2, revenue3))

        val foundRevenue1 = revenueRepository.findByDateAndSectorName(date1, "Sector C")
        assertNotNull(foundRevenue1)
        assertEquals(revenue1.id, foundRevenue1!!.id)
        assertEquals(300.00, foundRevenue1.amount, 0.001)

        val foundRevenue2 = revenueRepository.findByDateAndSectorName(date2, "Sector C")
        assertNotNull(foundRevenue2)
        assertEquals(revenue2.id, foundRevenue2!!.id)
        assertEquals(400.00, foundRevenue2.amount, 0.001)

        val notFoundRevenue = revenueRepository.findByDateAndSectorName(date1, "Sector E")
        assertNull(notFoundRevenue)

        val notFoundDate = revenueRepository.findByDateAndSectorName(LocalDate.of(2023, 3, 7), "Sector C")
        assertNull(notFoundDate)
    }

    @Test
    fun `should delete revenue entry`() {
        val revenue = Revenue(sectorName = "Sector F", amount = 100.00, date = LocalDate.of(2023, 4, 20))
        val savedRevenue = revenueRepository.save(revenue)

        var foundRevenue = revenueRepository.findById(savedRevenue.id!!).orElse(null)
        assertNotNull(foundRevenue)

        revenueRepository.delete(savedRevenue)

        foundRevenue = revenueRepository.findById(savedRevenue.id!!).orElse(null)
        assertNull(foundRevenue)
    }

    @Test
    fun `should return null when finding non-existent revenue by date and sector`() {
        val date = LocalDate.of(2024, 5, 1)
        val nonExistentRevenue = revenueRepository.findByDateAndSectorName(date, "NonExistentSector")
        assertNull(nonExistentRevenue)
    }
}