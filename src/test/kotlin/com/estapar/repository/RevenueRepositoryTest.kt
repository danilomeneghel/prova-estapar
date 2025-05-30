package com.estapar.repository

import com.estapar.model.Revenue
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@MicronautTest(environments = ["test"])
class RevenueRepositoryTest {

    @Inject
    lateinit var revenueRepository: RevenueRepository

    @BeforeEach
    fun setup() {
        revenueRepository.deleteAll()
    }

    @Test
    fun saveShouldPersistNewRevenue() {
        val date = LocalDate.now()
        val sectorName = "Centro"
        val amount = 150.75
        val newRevenue = Revenue(sectorName = sectorName, amount = amount, date = date)

        val savedRevenue = revenueRepository.save(newRevenue)

        assertNotNull(savedRevenue.id)
        assertEquals(sectorName, savedRevenue.sectorName)
        assertEquals(amount, savedRevenue.amount, 0.001)
        assertEquals(date, savedRevenue.date)
    }

    @Test
    fun findByIdShouldReturnOptionalOfRevenueWhenFound() {
        val date = LocalDate.now().minusDays(1)
        val sectorName = "Setor A"
        val amount = 200.00
        val existingRevenue = Revenue(sectorName = sectorName, amount = amount, date = date)
        val savedRevenue = revenueRepository.save(existingRevenue)

        val foundRevenueOptional = revenueRepository.findById(savedRevenue.id!!)

        assertTrue(foundRevenueOptional.isPresent)
        val foundRevenue = foundRevenueOptional.get()
        assertEquals(savedRevenue.id, foundRevenue.id)
        assertEquals(sectorName, foundRevenue.sectorName)
        assertEquals(amount, foundRevenue.amount, 0.001)
        assertEquals(date, foundRevenue.date)
    }

    @Test
    fun findByIdShouldReturnEmptyOptionalWhenNotFound() {
        val foundRevenueOptional = revenueRepository.findById(999L)

        assertFalse(foundRevenueOptional.isPresent)
    }

    @Test
    fun findByDateAndSectorNameShouldReturnRevenueWhenFound() {
        val date = LocalDate.now()
        val sectorName = "Setor B"
        val amount = 50.50
        val existingRevenue = Revenue(sectorName = sectorName, amount = amount, date = date)
        revenueRepository.save(existingRevenue)

        val foundRevenue = revenueRepository.findByDateAndSectorName(date, sectorName)

        assertNotNull(foundRevenue)
        assertEquals(sectorName, foundRevenue!!.sectorName)
        assertEquals(amount, foundRevenue.amount, 0.001)
        assertEquals(date, foundRevenue.date)
    }

    @Test
    fun findByDateAndSectorNameShouldReturnNullWhenNotFound() {
        val date = LocalDate.now().minusDays(2)
        val sectorName = "Setor Inexistente"

        val foundRevenue = revenueRepository.findByDateAndSectorName(date, sectorName)

        assertNull(foundRevenue)
    }

    @Test
    fun updateShouldModifyExistingRevenue() {
        val date = LocalDate.now()
        val sectorName = "Setor C"
        val initialAmount = 100.00
        val existingRevenue = Revenue(sectorName = sectorName, amount = initialAmount, date = date)
        val savedRevenue = revenueRepository.save(existingRevenue)

        val updatedAmount = 120.00
        val revenueToUpdate = savedRevenue.copy(amount = updatedAmount)
        val updatedRevenue = revenueRepository.update(revenueToUpdate)

        assertNotNull(updatedRevenue)
        assertEquals(savedRevenue.id, updatedRevenue.id)
        assertEquals(updatedAmount, updatedRevenue.amount, 0.001)
    }

    @Test
    fun deleteShouldRemoveRevenue() {
        val date = LocalDate.now()
        val sectorName = "Setor D"
        val existingRevenue = Revenue(sectorName = sectorName, amount = 75.00, date = date)
        val savedRevenue = revenueRepository.save(existingRevenue)

        assertNotNull(revenueRepository.findById(savedRevenue.id!!))

        revenueRepository.delete(savedRevenue)

        assertFalse(revenueRepository.findById(savedRevenue.id!!).isPresent)
    }

    @Test
    fun findAllShouldReturnAllRevenues() {
        val revenue1 = Revenue(sectorName = "S1", amount = 10.0, date = LocalDate.now())
        val revenue2 = Revenue(sectorName = "S2", amount = 20.0, date = LocalDate.now().minusDays(1))
        revenueRepository.saveAll(listOf(revenue1, revenue2))

        val allRevenues = revenueRepository.findAll().toList()

        assertEquals(2, allRevenues.size)
        assertTrue(allRevenues.any { it.sectorName == "S1" })
        assertTrue(allRevenues.any { it.sectorName == "S2" })
    }
}