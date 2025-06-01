package com.estapar.controller

import com.estapar.service.WebhookService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory

@Controller("/webhook")
class WebhookParkingController(
    private val webhookService: WebhookService
) {
    private val LOG = LoggerFactory.getLogger(WebhookParkingController::class.java)

    @Post
    fun receive(@Body payload: Map<String, Any>): HttpResponse<Map<String, Any>> {
        val eventType = payload["event_type"] as? String ?: "UNKNOWN"
        val licensePlate = payload["license_plate"] as? String ?: "N/A"

        LOG.info("Received webhook event. Type: {}, Plate: {}", eventType, licensePlate)

        return try {
            webhookService.processWebhookEvent(payload)
            HttpResponse.ok(mapOf("status" to "success", "message" to "Event processed successfully"))
        } catch (e: IllegalArgumentException) {
            LOG.warn("Bad Request for event (Type: {}, Plate: {}): {}", eventType, licensePlate, e.message)
            HttpResponse.badRequest(mapOf("error" to "Invalid event payload: ${e.message}"))
        } catch (e: DateTimeParseException) {
            LOG.warn("Bad Request (Date Parse) for event (Type: {}, Plate: {}): {}", eventType, licensePlate, e.message)
            HttpResponse.badRequest(mapOf("error" to "Invalid date format in payload: ${e.message}"))
        } catch (e: RuntimeException) {
            LOG.error("Runtime Error processing event (Type: {}, Plate: {}): {}", eventType, licensePlate, e.message, e)
            HttpResponse.serverError(mapOf("error" to "Failed to process event: ${e.message}"))
        } catch (e: Exception) {
            LOG.error("Unexpected Error processing event (Type: {}, Plate: {}): {}", eventType, licensePlate, e.message, e)
            HttpResponse.serverError(mapOf("error" to "An unexpected error occurred: ${e.message}"))
        }
    }
}