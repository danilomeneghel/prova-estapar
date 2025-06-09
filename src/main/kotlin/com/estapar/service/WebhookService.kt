package com.estapar.service

import jakarta.inject.Singleton
import java.time.Instant
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory

@Singleton
open class WebhookService(
    private val parkingService: ParkingService
) {
    private val LOG = LoggerFactory.getLogger(WebhookService::class.java)

    fun processWebhookEvent(payload: Map<String, Any>) {
        val eventType = payload["event_type"] as? String
            ?: throw IllegalArgumentException("Missing required field: event_type in webhook payload.")
        val licensePlate = payload["license_plate"] as? String

        LOG.info("Processing webhook event of type: {}", eventType)

        try {
            when (eventType) {
                "ENTRY" -> {
                    val requiredLicensePlate = licensePlate?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Missing or empty license_plate for ENTRY event.")
                    val entryTimeStr = payload["entry_time"] as? String
                    val entryTime = entryTimeStr?.let { Instant.parse(it) } ?: Instant.now()
                    parkingService.registerEntry(requiredLicensePlate, entryTime)
                    LOG.info("Webhook event ENTRY dispatched to ParkingService for plate: {}", requiredLicensePlate)
                }
                "PARKED" -> {
                    val requiredLicensePlate = licensePlate?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Missing or empty license_plate for PARKED event.")
                    val lat = (payload["lat"] as? Number)?.toDouble() ?: throw IllegalArgumentException("Missing lat for PARKED event.")
                    val lng = (payload["lng"] as? Number)?.toDouble() ?: throw IllegalArgumentException("Missing lng for PARKED event.")
                    parkingService.assignSpot(requiredLicensePlate, lat, lng)
                    LOG.info("Webhook event PARKED dispatched to ParkingService for plate: {}, lat: {}, lng: {}", requiredLicensePlate, lat, lng)
                }
                "EXIT" -> {
                    val requiredLicensePlate = licensePlate?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Missing or empty license_plate for EXIT event.")
                    val exitTimeStr = payload["exit_time"] as? String
                    val exitTime = exitTimeStr?.let { Instant.parse(it) } ?: Instant.now()
                    parkingService.handleExit(requiredLicensePlate, exitTime)
                    LOG.info("Webhook event EXIT dispatched to ParkingService for plate: {}", requiredLicensePlate)
                }
                else -> throw IllegalArgumentException("Unknown event_type: $eventType.")
            }
        } catch (e: DateTimeParseException) {
            LOG.error("Date parsing error in webhook payload for event type {}: {}", eventType, e.message)
            throw e
        } catch (e: IllegalArgumentException) {
            LOG.error("Invalid argument in webhook payload for event type {}: {}", eventType, e.message)
            throw e
        } catch (e: Exception) {
            LOG.error("Failed to process webhook event type {}: {}", eventType, e.message, e)
            throw RuntimeException("Failed to process webhook event '$eventType': ${e.message}", e)
        }
    }
}