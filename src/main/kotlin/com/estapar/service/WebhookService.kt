package com.estapar.service

import com.estapar.dto.EntryEventDTO
import com.estapar.dto.ExitEventDTO
import com.estapar.dto.ParkedEventDTO
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton

@Singleton
class WebhookService(
    private val garageService: GarageService,
    private val objectMapper: ObjectMapper
) {
    fun processWebhookEvent(payload: Map<String, Any>) {
        val eventType = payload["event_type"] as? String
            ?: throw IllegalArgumentException("Missing 'event_type' in payload.")

        when (eventType) {
            "ENTRY" -> {
                val entryEvent = objectMapper.convertValue(payload, EntryEventDTO::class.java)
                garageService.registerEntry(entryEvent.licensePlate, entryEvent.entryTime)
            }
            "PARKED" -> {
                val parkedEvent = objectMapper.convertValue(payload, ParkedEventDTO::class.java)
                garageService.assignSpot(parkedEvent.licensePlate, parkedEvent.lat, parkedEvent.lng)
            }
            "EXIT" -> {
                val exitEvent = objectMapper.convertValue(payload, ExitEventDTO::class.java)
                garageService.handleExit(exitEvent.licensePlate, exitEvent.exitTime)
            }
            else -> throw IllegalArgumentException("Unknown event_type: $eventType")
        }
    }
}