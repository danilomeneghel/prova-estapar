package com.estapar.repository

import com.estapar.model.Revenue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class RevenueRepositoryTest {

    @Mock
    private lateinit var revenueRepository: RevenueRepository

    @Test
    fun `findByDateAndSectorName should return revenue when found`() {
        val date = LocalDate.of(2024, 5, 29)
        val sectorName = "TestSector"
        val expectedRevenue = Revenue(id = 1L, date = date, sectorName = sectorName, amount = 150.0)

        whenever(revenueRepository.findByDateAndSectorName(date, sectorName)).thenReturn(expectedRevenue)

        val result = revenueRepository.findByDateAndSectorName(date, sectorName)

        assertThat(result).isEqualTo(expectedRevenue)
        verify(revenueRepository).findByDateAndSectorName(eq(date), eq(sectorName))
    }

    @Test
    fun `findByDateAndSectorName should return null when not found`() {
        val date = LocalDate.of(2024, 5, 30)
        val sectorName = "NonExistentSector"

        whenever(revenueRepository.findByDateAndSectorName(date, sectorName)).thenReturn(null)

        val result = revenueRepository.findByDateAndSectorName(date, sectorName)

        assertThat(result).isNull()
        verify(revenueRepository).findByDateAndSectorName(eq(date), eq(sectorName))
    }

    @Test
    fun `findById should return optional of revenue when found`() {
        val id = 1L
        val expectedRevenue = Revenue(id = id, date = LocalDate.now(), sectorName = "A", amount = 100.0)

        whenever(revenueRepository.findById(id)).thenReturn(Optional.of(expectedRevenue))

        val result = revenueRepository.findById(id)

        assertThat(result).isEqualTo(Optional.of(expectedRevenue))
        verify(revenueRepository).findById(eq(id))
    }

    @Test
    fun `save should persist a new revenue`() {
        val newRevenue = Revenue(date = LocalDate.now(), sectorName = "New", amount = 50.0)
        
        whenever(revenueRepository.save(argThat { entity -> entity.date == newRevenue.date && entity.sectorName == newRevenue.sectorName && entity.amount == newRevenue.amount }))
            .doAnswer { invocation ->
                val revenueArg = invocation.getArgument<Revenue>(0)
                revenueArg.id = 1L
                revenueArg
            }

        val result = revenueRepository.save(newRevenue)

        assertThat(result.id).isNotNull()
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.date).isEqualTo(newRevenue.date)
        assertThat(result.sectorName).isEqualTo(newRevenue.sectorName)
        assertThat(result.amount).isEqualTo(newRevenue.amount)
        verify(revenueRepository).save(argThat { entity -> entity.date == newRevenue.date && entity.sectorName == newRevenue.sectorName && entity.amount == newRevenue.amount })
    }

    @Test
    fun `deleteById should remove revenue by id`() {
        val idToDelete = 1L

        revenueRepository.deleteById(idToDelete)

        verify(revenueRepository).deleteById(eq(idToDelete))
    }

    @Test
    fun `findAll should return all revenues`() {
        val revenues = listOf(
            Revenue(id = 1L, date = LocalDate.now(), sectorName = "A", amount = 10.0),
            Revenue(id = 2L, date = LocalDate.now(), sectorName = "B", amount = 20.0)
        )
        whenever(revenueRepository.findAll()).thenReturn(revenues)

        val result = revenueRepository.findAll()

        assertThat(result).isEqualTo(revenues)
        verify(revenueRepository).findAll()
    }
}