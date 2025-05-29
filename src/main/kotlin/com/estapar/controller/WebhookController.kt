package com.estapar.controller

import com.estapar.service.GarageService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

@Controller("/webhook")
class WebhookController(
    private val garageService: GarageService
) {

    @Post
    fun receive(@Body payload: Map<String, Any>): HttpResponse<Any> {
        return try {
            garageService.processWebhookEvent(payload)
            HttpResponse.ok()
        } catch (e: IllegalArgumentException) {
            HttpResponse.badRequest("Invalid event payload: ${e.message}")
        } catch (e: Exception) {
            HttpResponse.serverError("An unexpected error occurred: ${e.message}")
        }
    }
}