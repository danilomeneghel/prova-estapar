package com.estapar.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.format.DateTimeParseException
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class WebhookServiceTest {

    @Mock
    private lateinit var parkingService: ParkingService

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var webhookService: WebhookService

    @Test
    fun processWebhookEventShouldThrowExceptionWhenEventTypeIsMissing() {
        val payload = mapOf("license_plate" to "ABC1234")

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldProcessEntryEventSuccessfully() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()
        val payload = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate,
            "entry_time" to entryTime.toString()
        )

        whenever(parkingService.registerEntry(any(), any())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload)

        verify(parkingService).registerEntry(eq(licensePlate), eq(entryTime))
        assertEquals(payload, result)
    }

    @Test
    fun processWebhookEventShouldProcessEntryEventWithCurrentTimeWhenEntryTimeIsMissing() {
        val licensePlate = "ABC1234"
        val payload = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate
        )

        val captureInstant = argumentCaptor<Instant>()

        whenever(parkingService.registerEntry(any(), any<Instant>())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload)

        verify(parkingService).registerEntry(eq(licensePlate), captureInstant.capture())
        assertTrue(captureInstant.firstValue.isBefore(Instant.now().plusSeconds(1)))
        assertTrue(captureInstant.firstValue.isAfter(Instant.now().minusSeconds(1)))
        assertEquals(payload, result)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForEntryEventWhenLicensePlateIsMissing() {
        val payload = mapOf(
            "event_type" to "ENTRY",
            "entry_time" to Instant.now().toString()
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForEntryEventWhenLicensePlateIsEmpty() {
        val payload = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to "",
            "entry_time" to Instant.now().toString()
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowDateTimeParseExceptionForInvalidEntryTime() {
        val payload = mapOf(
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
        val payload = mapOf(
            "event_type" to "PARKED",
            "license_plate" to licensePlate,
            "lat" to lat,
            "lng" to lng
        )

        whenever(parkingService.assignSpot(any(), any(), any())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload)

        verify(parkingService).assignSpot(eq(licensePlate), eq(lat), eq(lng))
        assertEquals(payload, result)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLicensePlateIsMissing() {
        val payload = mapOf(
            "event_type" to "PARKED",
            "lat" to 10.0,
            "lng" to 20.0
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLicensePlateIsEmpty() {
        val payload = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "",
            "lat" to 10.0,
            "lng" to 20.0
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLatIsMissing() {
        val payload = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "ABC1234",
            "lng" to 20.0
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowExceptionForParkedEventWhenLngIsMissing() {
        val payload = mapOf(
            "event_type" to "PARKED",
            "license_plate" to "ABC1234",
            "lat" to 10.0
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldProcessExitEventSuccessfully() {
        val licensePlate = "ABC1234"
        val exitTime = Instant.now()
        val payload = mapOf(
            "event_type" to "EXIT",
            "license_plate" to licensePlate,
            "exit_time" to exitTime.toString()
        )

        whenever(parkingService.handleExit(any(), any())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload)

        verify(parkingService).handleExit(eq(licensePlate), eq(exitTime))
        assertEquals(payload, result)
    }

    @Test
    fun processWebhookEventShouldProcessExitEventWithCurrentTimeWhenExitTimeIsMissing() {
        val licensePlate = "ABC1234"
        val payload = mapOf(
            "event_type" to "EXIT",
            "license_plate" to licensePlate
        )

        val captureInstant = argumentCaptor<Instant>()

        whenever(parkingService.handleExit(any(), any<Instant>())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload)

        verify(parkingService).handleExit(eq(licensePlate), captureInstant.capture())
        assertTrue(captureInstant.firstValue.isBefore(Instant.now().plusSeconds(1)))
        assertTrue(captureInstant.firstValue.isAfter(Instant.now().minusSeconds(1)))
        assertEquals(payload, result)
    }

    @Test
    fun processWebhookEventShouldHandleExitEventWhenLicensePlateIsNull() {
        val exitTime = Instant.now()
        val payload: Map<String, Any?> = mapOf(
            "event_type" to "EXIT",
            "license_plate" to null,
            "exit_time" to exitTime.toString()
        )

        whenever(parkingService.handleExit(any(), any())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload as Map<String, Any>)

        verify(parkingService).handleExit(eq(""), eq(exitTime))
        assertEquals(payload, result)
    }

    @Test
    fun processWebhookEventShouldHandleExitEventWhenLicensePlateIsEmpty() {
        val exitTime = Instant.now()
        val payload = mapOf(
            "event_type" to "EXIT",
            "license_plate" to "",
            "exit_time" to exitTime.toString()
        )

        whenever(parkingService.handleExit(any(), any())).thenAnswer { }

        val result = webhookService.processWebhookEvent(payload)

        verify(parkingService).handleExit(eq(""), eq(exitTime))
        assertEquals(payload, result)
    }


    @Test
    fun processWebhookEventShouldThrowDateTimeParseExceptionForInvalidExitTime() {
        val payload = mapOf(
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
        val payload = mapOf(
            "event_type" to "UNKNOWN_EVENT",
            "license_plate" to "ABC1234"
        )

        assertThrows(IllegalArgumentException::class.java) {
            webhookService.processWebhookEvent(payload)
        }
        verifyNoInteractions(parkingService)
    }

    @Test
    fun processWebhookEventShouldThrowRuntimeExceptionWhenGarageServiceFails() {
        val licensePlate = "ABC1234"
        val entryTime = Instant.now()
        val payload = mapOf(
            "event_type" to "ENTRY",
            "license_plate" to licensePlate,
            "entry_time" to entryTime.toString()
        )

        val errorMessage = "Database connection lost"
        whenever(parkingService.registerEntry(any(), any())).thenThrow(RuntimeException(errorMessage))

        val exception = assertThrows(RuntimeException::class.java) {
            webhookService.processWebhookEvent(payload)
        }

        assertTrue(exception.message?.contains("Failed to process event 'ENTRY' for plate 'ABC1234': $errorMessage") ?: false)
        verify(parkingService).registerEntry(eq(licensePlate), eq(entryTime))
    }
}