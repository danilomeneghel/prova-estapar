package com.estapar.controller

import com.estapar.dto.*
import com.estapar.service.GarageService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@Controller("/api")
class ApiParkingController(
    private val garageService: GarageService
) {
    @Post("/plate-status")
    fun plateStatus(@Body body: Map<String, String>): PlateStatusDTO {
        return garageService.postPlateStatus(body["license_plate"] ?: "")
    }

    @Post("/spot-status")
    fun spotStatus(@Body body: Map<String, Double>): SpotStatusDTO {
        return garageService.postSpotStatus(body["lat"] ?: 0.0, body["lng"] ?: 0.0)
    }

    @Post("/revenue")
    fun revenue(@Body revenueRequest: RevenueRequestDTO): RevenueDTO {
        return garageService.getRevenue(revenueRequest.date, revenueRequest.sector)
    }

    @Get("/garage")
    fun garageStatus(): GarageInfoDTO {
        return garageService.getGarage()
    }
}