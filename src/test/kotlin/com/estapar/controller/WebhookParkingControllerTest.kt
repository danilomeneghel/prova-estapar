package com.estapar.controller

import com.estapar.service.WebhookService
import io.micronaut.http.HttpStatus
import io.micronaut.http.HttpResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.format.DateTimeParseException
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class WebhookParkingControllerTest {

    @Mock
    private lateinit var webhookService: WebhookService

    @InjectMocks
    private lateinit var webhookParkingController: WebhookParkingController

    @Test
    fun receiveShouldReturnOkResponseWhenServiceProcessesSuccessfully() {
        val payload = mapOf("event_type" to "ENTRY", "license_plate" to "ABC1234")

        whenever(webhookService.processWebhookEvent(payload)).thenAnswer { }

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.OK, response.status)
        assertEquals(mapOf("status" to "success", "message" to "Event processed successfully"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnBadRequestWhenIllegalArgumentExceptionIsThrown() {
        val payload = mapOf("license_plate" to "ABC1234")
        val errorMessage = "Missing required field: event_type in webhook payload."

        whenever(webhookService.processWebhookEvent(payload)).thenThrow(IllegalArgumentException(errorMessage))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
        assertEquals(mapOf("error" to "Invalid event payload: $errorMessage"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnBadRequestWhenDateTimeParseExceptionIsThrown() {
        val payload = mapOf("event_type" to "EXIT", "license_plate" to "ABC1234", "exit_time" to "invalid-date")
        val errorMessage = "Text 'invalid-date' could not be parsed"

        whenever(webhookService.processWebhookEvent(payload)).thenThrow(DateTimeParseException(errorMessage, "invalid-date", 0))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
        assertEquals(mapOf("error" to "Invalid date format in payload: $errorMessage"), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnServerErrorWhenRuntimeExceptionIsThrown() {
        val payload = mapOf("event_type" to "ENTRY", "license_plate" to "ABC1234", "entry_time" to Instant.now().toString())
        val internalServiceOriginalMessage = "Database connection lost"
        val eventType = payload["event_type"] as String // "ENTRY"
        // Esta é a mensagem EXATA que o WebhookService produziria (encapsulando a original)
        val webhookServiceProducedMessage = "Failed to process webhook event '$eventType': $internalServiceOriginalMessage"
        // Esta é a mensagem FINAL que o Controller retorna no corpo da resposta
        val expectedControllerErrorMessage = "Failed to process event: $webhookServiceProducedMessage"

        // O mock do service deve lançar a RuntimeException com a mensagem que o service *realmente* produziria.
        whenever(webhookService.processWebhookEvent(payload)).thenThrow(RuntimeException(webhookServiceProducedMessage))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals(mapOf("error" to expectedControllerErrorMessage), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }

    @Test
    fun receiveShouldReturnServerErrorWhenGenericExceptionIsThrown() {
        val payload = mapOf("event_type" to "UNKNOWN_EVENT", "license_plate" to "XYZ7890")
        val internalServiceOriginalMessage = "An arbitrary error occurred in service"
        val eventType = payload["event_type"] as String // "UNKNOWN_EVENT"
        // Esta é a mensagem EXATA que o WebhookService produziria (encapsulando a original)
        val webhookServiceProducedMessage = "Failed to process webhook event '$eventType': $internalServiceOriginalMessage"
        // Esta é a mensagem FINAL que o Controller retorna no corpo da resposta
        val expectedControllerErrorMessage = "Failed to process event: $webhookServiceProducedMessage"

        // O mock do service deve lançar uma RuntimeException com a mensagem que o service *realmente* produziria.
        whenever(webhookService.processWebhookEvent(payload)).thenThrow(RuntimeException(webhookServiceProducedMessage))

        val response = webhookParkingController.receive(payload)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
        assertEquals(mapOf("error" to expectedControllerErrorMessage), response.body())
        verify(webhookService).processWebhookEvent(payload)
    }
}