package com.estapar.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import java.time.Instant
import java.time.format.DateTimeParseException

@Singleton
open class WebhookService(
    private val parkingService: ParkingService,
    private val objectMapper: ObjectMapper
) {

    fun processWebhookEvent(payload: Map<String, Any>): Map<String, Any> {
        val eventType = payload["event_type"] as? String
        val licensePlate = payload["license_plate"] as? String

        if (eventType == null) {
            throw IllegalArgumentException("Missing required field: event_type in webhook payload.")
        }

        try {
            when (eventType) {
                "ENTRY" -> {
                    if (licensePlate.isNullOrBlank()) {
                        throw IllegalArgumentException("Missing or empty license_plate for ENTRY event.")
                    }
                    val entryTimeStr = payload["entry_time"] as? String
                    val entryTime = entryTimeStr?.let { Instant.parse(it) } ?: Instant.now()
                    parkingService.registerEntry(licensePlate, entryTime)
                }
                "PARKED" -> {
                    if (licensePlate.isNullOrBlank()) {
                        throw IllegalArgumentException("Missing or empty license_plate for PARKED event.")
                    }
                    val lat = (payload["lat"] as? Number)?.toDouble()
                    val lng = (payload["lng"] as? Number)?.toDouble()
                    if (lat == null || lng == null) {
                        throw IllegalArgumentException("Missing lat or lng for PARKED event.")
                    }
                    parkingService.assignSpot(licensePlate, lat, lng)
                }
                "EXIT" -> {
                    val exitTimeStr = payload["exit_time"] as? String
                    val exitTime = exitTimeStr?.let { Instant.parse(it) } ?: Instant.now()
                    parkingService.handleExit(licensePlate.orEmpty(), exitTime)
                }
                else -> throw IllegalArgumentException("Unknown event_type: $eventType.")
            }
            return payload
        } catch (e: DateTimeParseException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("Failed to process event '$eventType' for plate '$licensePlate': ${e.message}", e)
        }
    }
}