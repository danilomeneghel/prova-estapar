package com.estapar.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.format.DateTimeParseException

@ExtendWith(MockitoExtension::class)
class WebhookServiceTest {

    @Mock
    private lateinit var parkingService: ParkingService

    @InjectMocks
    private lateinit var webhookService: WebhookService

    @Test
    fun processWebhookEventShouldThrowExceptionWhenEventTypeIsMissing() {
        val payload: Map<String, Any> = mapOf("license_plate" to "ABC1234")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing required field: event_type in webhook payload.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldProcessEntryEventSuccessfully() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()
        val payload: Map<String, Any> = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate,
            "entry_time" to entryTime.toString()
        )
        assertDoesNotThrow {
            webhookService.processWebhookEvent(payload)
        }
        verify(parkingService).registerEntry(eq(licensePlate), eq(entryTime))
    }

    @Test
    fun processWebhookEventShouldProcessEntryEventWithCurrentTimeWhenEntryTimeIsMissing() {
        val licensePlate = "ABC1234"
        val payload: Map<String, Any> = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate
        )
        val captureInstant = argumentCaptor<Instant>()
        assertDoesNotThrow {
            webhookService.processWebhookEvent(payload)
        }
        verify(parkingService).registerEntry(eq(licensePlate), captureInstant.capture())
        assertTrue(captureInstant.firstValue.isBefore(Instant.now().plusSeconds(2)))
        assertTrue(captureInstant.firstValue.isAfter(Instant.now().minusSeconds(2)))
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForEntryEventWhenLicensePlateIsMissing() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "ENTRY",
            "entry_time" to Instant.now().toString()
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing or empty license_plate for ENTRY event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForEntryEventWhenLicensePlateIsEmpty() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to "",
            "entry_time" to Instant.now().toString()
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing or empty license_plate for ENTRY event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowDateTimeParseExceptionForInvalidEntryTime() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to "ABC1234",
            "entry_time" to "invalid-time"
        )
        assertThrows(DateTimeParseException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldProcessParkedEventSuccessfully() {
        val licensePlate = "ABC1234"
        val lat = 10.0
        val lng = 20.0
        val payload: Map<String, Any> = mapOf(
            "event_type" to "PARKED",
            "license_plate" to licensePlate,
            "lat" to lat,
            "lng" to lng
        )
        assertDoesNotThrow {
            webhookService.processWebhookEvent(payload)
        }
        verify(parkingService).assignSpot(eq(licensePlate), eq(lat), eq(lng))
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLicensePlateIsMissing() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "PARKED",
            "lat" to 10.0,
            "lng" to 20.0
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing or empty license_plate for PARKED event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLicensePlateIsEmpty() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "",
            "lat" to 10.0,
            "lng" to 20.0
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing or empty license_plate for PARKED event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLatIsMissing() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "ABC1234",
            "lng" to 20.0
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing lat for PARKED event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLngIsMissing() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "ABC1234",
            "lat" to 10.0
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing lng for PARKED event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldProcessExitEventSuccessfully() {
        val licensePlate = "ABC1234"
        val exitTime = Instant.now()
        val payload: Map<String, Any> = mapOf(
            "event_type" to "EXIT",
            "license_plate" to licensePlate,
            "exit_time" to exitTime.toString()
        )
        assertDoesNotThrow {
            webhookService.processWebhookEvent(payload)
        }
        verify(parkingService).handleExit(eq(licensePlate), eq(exitTime))
    }

    @Test
    fun processWebhookEventShouldProcessExitEventWithCurrentTimeWhenExitTimeIsMissing() {
        val licensePlate = "ABC1234"
        val payload: Map<String, Any> = mapOf(
            "event_type" to "EXIT",
            "license_plate" to licensePlate
        )
        val captureInstant = argumentCaptor<Instant>()
        assertDoesNotThrow {
            webhookService.processWebhookEvent(payload)
        }
        verify(parkingService).handleExit(eq(licensePlate), captureInstant.capture())
        assertTrue(captureInstant.firstValue.isBefore(Instant.now().plusSeconds(2)))
        assertTrue(captureInstant.firstValue.isAfter(Instant.now().minusSeconds(2)))
    }

    @Test
    fun processWebhookEventShouldThrowExceptionWhenLicensePlateIsNullForExitEvent() {
        val exitTime = Instant.now()
        val payloadWithNullLicensePlate: Map<String, Any?> = mapOf(
            "event_type" to "EXIT",
            "license_plate" to null,
            "exit_time" to exitTime.toString()
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            val filteredPayload: Map<String, Any> = payloadWithNullLicensePlate
                .filterValues { it != null }
                .mapValues { it.value!! }
            webhookService.processWebhookEvent(filteredPayload)
        }
        assertEquals("Missing or empty license_plate for EXIT event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionWhenLicensePlateIsEmptyForExitEvent() {
        val exitTime = Instant.now()
        val payload: Map<String, Any> = mapOf(
            "event_type" to "EXIT",
            "license_plate" to "",
            "exit_time" to exitTime.toString()
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Missing or empty license_plate for EXIT event.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowDateTimeParseExceptionForInvalidExitTime() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "EXIT",
            "license_plate" to "ABC1234",
            "exit_time" to "invalid-time"
        )
        assertThrows(DateTimeParseException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForUnknownEventType() {
        val payload: Map<String, Any> = mapOf(
            "event_type" to "UNKNOWN_EVENT",
            "license_plate" to "ABC1234"
        )
        val exception = assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertEquals("Unknown event_type: UNKNOWN_EVENT.", exception.message)
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowRuntimeExceptionWhenParkingServiceFails() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()
        val payload: Map<String, Any> = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate,
            "entry_time" to entryTime.toString()
        )
        val errorMessage = "Database connection lost"
        whenever(parkingService.registerEntry(any(), any())).thenThrow(RuntimeException(errorMessage))

        val exception = assertThrows(RuntimeException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        assertTrue(exception.message?.contains("Failed to process webhook event 'ENTRY': $errorMessage") ?: false)
        verify(parkingService).registerEntry(eq(licensePlate), eq(entryTime))
    }
}