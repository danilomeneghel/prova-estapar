package com.estapar.service

import com.estapar.dto.EntryEventDTO
import com.estapar.dto.ExitEventDTO
import com.estapar.dto.ParkedEventDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import org.assertj.core.api.Assertions.assertThatThrownBy

@ExtendWith(MockitoExtension::class)
class WebhookServiceTest {

    @Mock
    private lateinit var garageService: GarageService

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var webhookService: WebhookService

    @Test
    fun `processWebhookEvent should handle ENTRY event correctly`() {
        val entryTime = Instant.now()
        val payload = mapOf(
            "event_type" to "ENTRY",
            "licensePlate" to "ABC1234",
            "entryTime" to entryTime.toString()
        )
        val entryEventDTO = EntryEventDTO(licensePlate = "ABC1234", entryTime = entryTime)

        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(EntryEventDTO::class.java))).thenReturn(entryEventDTO)

        webhookService.processWebhookEvent(payload)

        verify(garageService).registerEntry(eq(entryEventDTO.licensePlate), eq(entryEventDTO.entryTime))
        verify(objectMapper).convertValue(eq(payload), eq(EntryEventDTO::class.java))
    }

    @Test
    fun `processWebhookEvent should handle PARKED event correctly`() {
        val lat = -23.5
        val lng = -46.6
        val payload = mapOf(
            "event_type" to "PARKED",
            "licensePlate" to "XYZ5678",
            "lat" to lat,
            "lng" to lng
        )
        val parkedEventDTO = ParkedEventDTO(licensePlate = "XYZ5678", lat = lat, lng = lng)

        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(ParkedEventDTO::class.java))).thenReturn(parkedEventDTO)

        webhookService.processWebhookEvent(payload)

        verify(garageService).assignSpot(eq(parkedEventDTO.licensePlate), eq(parkedEventDTO.lat), eq(parkedEventDTO.lng))
        verify(objectMapper).convertValue(eq(payload), eq(ParkedEventDTO::class.java))
    }

    @Test
    fun `processWebhookEvent should handle EXIT event correctly`() {
        val exitTime = Instant.now()
        val payload = mapOf(
            "event_type" to "EXIT",
            "licensePlate" to "DEF9012",
            "exitTime" to exitTime.toString()
        )
        val exitEventDTO = ExitEventDTO(licensePlate = "DEF9012", exitTime = exitTime)

        whenever(objectMapper.convertValue(any<Map<String, Any>>(), eq(ExitEventDTO::class.java))).thenReturn(exitEventDTO)

        webhookService.processWebhookEvent(payload)

        verify(garageService).handleExit(eq(exitEventDTO.licensePlate), eq(exitEventDTO.exitTime))
        verify(objectMapper).convertValue(eq(payload), eq(ExitEventDTO::class.java))
    }

    @Test
    fun `processWebhookEvent should throw IllegalArgumentException if event_type is missing`() {
        val payload = mapOf(
            "some_other_key" to "value"
        )

        assertThatThrownBy { webhookService.processWebhookEvent(payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing 'event_type' in payload.")

        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        verify(garageService, never()).registerEntry(any(), any())
        verify(garageService, never()).assignSpot(any(), any(), any())
        verify(garageService, never()).handleExit(any(), any())
    }

    @Test
    fun `processWebhookEvent should throw IllegalArgumentException if event_type is unknown`() {
        val payload = mapOf(
            "event_type" to "UNKNOWN_EVENT",
            "data" to "some_data"
        )

        assertThatThrownBy { webhookService.processWebhookEvent(payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Unknown event_type: UNKNOWN_EVENT")

        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        verify(garageService, never()).registerEntry(any(), any())
        verify(garageService, never()).assignSpot(any(), any(), any())
        verify(garageService, never()).handleExit(any(), any())
    }
}