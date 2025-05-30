package com.estapar.controller

import com.estapar.service.WebhookService
import io.micronaut.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.format.DateTimeParseException

@ExtendWith(MockitoExtension::class)
class WebhookParkingControllerTest {

    @Mock
    private lateinit var webhookService: WebhookService

    @InjectMocks
    private lateinit var webhookParkingController: WebhookParkingController

    @Test
    fun receiveShouldReturnOkResponseWhenServiceProcessesSuccessfully() {
        val payload = mapOf("event" to "parking_entry", "license_plate" to "ABC1234")
        val serviceResponse = mapOf("status" to "success", "message" to "Event processed")

        whenever(webhookService.processWebhookEvent(payload)).thenReturn(serviceResponse)

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.OK, response.status)
        assertEquals(serviceResponse, response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnBadRequestWhenIllegalArgumentExceptionIsThrown() {
        val payload = mapOf("event" to "parking_entry")
        val errorMessage = "Missing required field"

        whenever(webhookService.processWebhookEvent(payload)).thenThrow(IllegalArgumentException(errorMessage))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
        assertEquals(mapOf("error" to "Invalid event payload: $errorMessage"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnBadRequestWhenDateTimeParseExceptionIsThrown() {
        val payload = mapOf("event" to "parking_exit", "exit_time" to "invalid-date")
        val errorMessage = "Text 'invalid-date' could not be parsed"

        whenever(webhookService.processWebhookEvent(payload)).thenThrow(DateTimeParseException(errorMessage, "invalid-date", 0))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
        assertEquals(mapOf("error" to "Invalid date format in payload: $errorMessage"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnServerErrorWhenRuntimeExceptionIsThrown() {
        val payload = mapOf("event" to "generic_error")
        val errorMessage = "Database connection lost"

        whenever(webhookService.processWebhookEvent(payload)).thenThrow(RuntimeException(errorMessage))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals(mapOf("error" to "Failed to process event: $errorMessage"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnServerErrorWhenGenericExceptionIsThrown() {
        val payload = mapOf("event" to "unknown_error")
        val errorMessage = "An unknown error occurred in service"

        whenever(webhookService.processWebhookEvent(payload)).thenThrow(IllegalStateException(errorMessage))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals(mapOf("error" to "Failed to process event: $errorMessage"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }
}