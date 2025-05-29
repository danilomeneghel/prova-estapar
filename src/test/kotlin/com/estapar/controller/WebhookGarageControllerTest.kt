package com.estapar.controller

import com.estapar.service.WebhookService
import io.micronaut.http.HttpResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
class WebhookGarageControllerTest {

    @Mock
    private lateinit var webhookService: WebhookService

    @InjectMocks
    private lateinit var webhookGarageController: WebhookGarageController

    @Test
    fun `receive should return HTTP 200 OK for successful event processing`() {
        val payload = mapOf(
            "event_type" to "ENTRY",
            "licensePlate" to "ABC1234",
            "entryTime" to "2024-05-29T10:00:00Z"
        )

        whenever(webhookService.processWebhookEvent(eq(payload))).thenAnswer { } 

        val response = webhookGarageController.receive(payload)

        assertThat(response.status.code).isEqualTo(200)
        assertThat(response.body.isPresent).isFalse() 
        verify(webhookService).processWebhookEvent(eq(payload))
    }

    @Test
    fun `receive should return HTTP 400 Bad Request for IllegalArgumentException`() {
        val payload = mapOf(
            "invalid_key" to "some_value" 
        )
        val errorMessage = "Missing 'event_type' in payload."

        doThrow(IllegalArgumentException(errorMessage))
            .whenever(webhookService)
            .processWebhookEvent(eq(payload))

        val response = webhookGarageController.receive(payload)

        assertThat(response.status.code).isEqualTo(400)
        assertThat(response.body.get()).isEqualTo("Invalid event payload: $errorMessage")
        verify(webhookService).processWebhookEvent(eq(payload))
    }

    @Test
    fun `receive should return HTTP 500 Server Error for generic Exception`() {
        val payload = mapOf(
            "event_type" to "ENTRY",
            "licensePlate" to "ABC1234",
            "entryTime" to "2024-05-29T10:00:00Z"
        )
        val errorMessage = "Database connection failed."

        doThrow(RuntimeException(errorMessage))
            .whenever(webhookService)
            .processWebhookEvent(eq(payload))

        val response = webhookGarageController.receive(payload)

        assertThat(response.status.code).isEqualTo(500)
        assertThat(response.body.get()).isEqualTo("An unexpected error occurred: $errorMessage")
        verify(webhookService).processWebhookEvent(eq(payload))
    }
}