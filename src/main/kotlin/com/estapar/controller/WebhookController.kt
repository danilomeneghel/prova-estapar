package com.estapar.controller

import com.estapar.service.GarageService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.time.Instant

@Controller("/webhook")
class WebhookController(
    private val garageService: GarageService
) {

    @Post
    fun receive(@Body payload: Map<String, Any>): HttpResponse<Any> {
        val eventType = payload["event_type"] as? String ?: return HttpResponse.badRequest()
        when (eventType) {
            "ENTRY" -> {
                val plate = payload["license_plate"] as String
                val entryTime = Instant.parse(payload["entry_time"] as String)
                garageService.registerEntry(plate, entryTime)
            }
            "PARKED" -> {
                val plate = payload["license_plate"] as String
                val lat = (payload["lat"] as Number).toDouble()
                val lng = (payload["lng"] as Number).toDouble()
                garageService.assignSpot(plate, lat, lng)
            }
            "EXIT" -> {
                val plate = payload["license_plate"] as String
                val exitTime = Instant.parse(payload["exit_time"] as String)
                garageService.handleExit(plate, exitTime)
            }
        }
        return HttpResponse.ok()
    }
    
}