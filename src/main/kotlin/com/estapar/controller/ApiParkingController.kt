package com.estapar.controller

import com.estapar.dto.*
import com.estapar.service.ParkingService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@Controller("/api")
class ApiParkingController(
    private val parkingService: ParkingService
) {
    @Post("/plate-status")
    fun plateStatus(@Body body: Map<String, String>): PlateStatusDTO {
        return parkingService.postPlateStatus(body["license_plate"] ?: "")
    }

    @Post("/spot-status")
    fun spotStatus(@Body body: Map<String, Double>): SpotStatusDTO {
        return parkingService.postSpotStatus(body["lat"] ?: 0.0, body["lng"] ?: 0.0)
    }

    @Post("/revenue")
    fun revenue(@Body revenueRequest: RevenueRequestDTO): RevenueDTO {
        return parkingService.getRevenue(revenueRequest.date, revenueRequest.sector)
    }

    @Get("/garage")
    fun garageStatus(): GarageInfoDTO {
        return parkingService.getGarage()
    }
}