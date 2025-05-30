package com.estapar.controller

import com.estapar.service.WebhookService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.format.DateTimeParseException

@Controller("/webhook")
class WebhookGarageController(
    private val webhookService: WebhookService
) {

    @Post
    fun receive(@Body payload: Map<String, Any>): HttpResponse<Map<String, Any>> {
        return try {
            val responsePayload = webhookService.processWebhookEvent(payload)
            HttpResponse.ok(responsePayload)
        } catch (e: IllegalArgumentException) {
            HttpResponse.badRequest(mapOf("error" to "Invalid event payload: ${e.message}"))
        } catch (e: DateTimeParseException) {
            HttpResponse.badRequest(mapOf("error" to "Invalid date format in payload: ${e.message}"))
        } catch (e: RuntimeException) {
            HttpResponse.serverError(mapOf("error" to "Failed to process event: ${e.message}"))
        } catch (e: Exception) {
            HttpResponse.serverError(mapOf("error" to "An unexpected error occurred: ${e.message}"))
        }
    }
}