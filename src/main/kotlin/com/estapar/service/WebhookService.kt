package com.estapar.service

import com.estapar.model.*
import com.estapar.repository.*
import jakarta.inject.Singleton
import java.time.Instant
import com.fasterxml.jackson.databind.ObjectMapper

@Singleton
open class WebhookService(
    private val garageService: GarageService,
    private val objectMapper: ObjectMapper
) {

    fun processWebhookEvent(payload: Map<String, Any>): Map<String, Any> {
        val eventType = payload["event_type"] as? String
        val licensePlate = payload["license_plate"] as? String // CORRIGIDO AQUI
        val lat = (payload["lat"] as? Number)?.toDouble()
        val lng = (payload["lng"] as? Number)?.toDouble()

        if (licensePlate == null || eventType == null) {
            throw IllegalArgumentException("Missing required fields: license_plate or event_type")
        }

        try {
            when (eventType) {
                "ENTRY" -> {
                    val entryTime = Instant.now()
                    garageService.registerEntry(licensePlate, entryTime)
                }
                "PARKED" -> {
                    if (lat == null || lng == null) {
                        throw IllegalArgumentException("Missing lat or lng for PARKED event")
                    }
                    garageService.assignSpot(licensePlate, lat, lng)
                }
                "EXIT" -> {
                    val exitTime = Instant.now()
                    garageService.handleExit(licensePlate, exitTime)
                }
                else -> throw IllegalArgumentException("Unknown event_type: $eventType")
            }
            return payload
        } catch (e: Exception) {
            throw RuntimeException("Failed to save data to database for event type $eventType: ${e.message}", e)
        }
    }
}