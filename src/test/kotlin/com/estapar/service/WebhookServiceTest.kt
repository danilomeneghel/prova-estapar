package com.estapar.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals

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
        val licensePlate = "ABC1234"
        val payload = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate
        )

        val result = webhookService.processWebhookEvent(payload)

        verify(garageService).registerEntry(eq(licensePlate), any<Instant>())
        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        assertEquals(payload, result)
    }

    @Test
    fun `processWebhookEvent should handle PARKED event correctly`() {
        val licensePlate = "XYZ5678"
        val lat = -23.5
        val lng = -46.6
        val payload = mapOf(
            "event_type" to "PARKED",
            "license_plate" to licensePlate,
            "lat" to lat,
            "lng" to lng
        )

        val result = webhookService.processWebhookEvent(payload)

        verify(garageService).assignSpot(eq(licensePlate), eq(lat), eq(lng))
        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        assertEquals(payload, result)
    }

    @Test
    fun `processWebhookEvent should handle EXIT event correctly`() {
        val licensePlate = "DEF9012"
        val payload = mapOf(
            "event_type" to "EXIT",
            "license_plate" to licensePlate
        )

        val result = webhookService.processWebhookEvent(payload)

        verify(garageService).handleExit(eq(licensePlate), any<Instant>())
        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        assertEquals(payload, result)
    }

    @Test
    fun `processWebhookEvent should throw IllegalArgumentException if license_plate is missing`() {
        val payload = mapOf(
            "event_type" to "ENTRY"
        )

        assertThatThrownBy { webhookService.processWebhookEvent(payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing required fields: license_plate or event_type")

        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        verify(garageService, never()).registerEntry(any(), any())
        verify(garageService, never()).assignSpot(any(), any(), any())
        verify(garageService, never()).handleExit(any(), any())
    }

    @Test
    fun `processWebhookEvent should throw IllegalArgumentException if event_type is missing`() {
        val payload = mapOf(
            "license_plate" to "TEST1234"
        )

        assertThatThrownBy { webhookService.processWebhookEvent(payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing required fields: license_plate or event_type")

        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        verify(garageService, never()).registerEntry(any(), any())
        verify(garageService, never()).assignSpot(any(), any(), any())
        verify(garageService, never()).handleExit(any(), any())
    }

    @Test
    fun `processWebhookEvent should throw IllegalArgumentException if lat or lng is missing for PARKED event`() {
        val payload = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "ABC1234"
        )

        
        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        verify(garageService, never()).registerEntry(any(), any())
        verify(garageService, never()).assignSpot(any(), any(), any())
        verify(garageService, never()).handleExit(any(), any())
    }

    @Test
    fun `processWebhookEvent should throw IllegalArgumentException if event_type is unknown`() {
        val payload = mapOf(
            "event_type" to "UNKNOWN_EVENT",
            "license_plate" to "XYZ7890"
        )

        
        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
        verify(garageService, never()).registerEntry(any(), any())
        verify(garageService, never()).assignSpot(any(), any(), any())
        verify(garageService, never()).handleExit(any(), any())
    }

    @Test
    fun `processWebhookEvent should throw RuntimeException on garageService failure`() {
        val licensePlate = "FAIL123"
        val payload = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate
        )
        val failureMessage = "Database error during save"

        doThrow(RuntimeException(failureMessage))
            .whenever(garageService)
            .registerEntry(eq(licensePlate), any())

        assertThatThrownBy { webhookService.processWebhookEvent(payload) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Failed to save data to database for event type ENTRY: $failureMessage")

        verify(objectMapper, never()).convertValue(any<Map<String, Any>>(), any<Class<*>>())
    }
}