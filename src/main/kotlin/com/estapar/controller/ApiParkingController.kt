package com.estapar.controller

import com.estapar.service.GarageService
import io.micronaut.http.annotation.*
import java.time.LocalDate
import com.estapar.dto.PlateStatusDTO
import com.estapar.dto.SpotStatusDTO
import com.estapar.dto.RevenueDTO
import com.estapar.dto.GarageInfoDTO

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

    @Get("/revenue")
    fun revenue(@QueryValue date: String, @QueryValue sector: String): RevenueDTO {
        val parsedDate = LocalDate.parse(date)
        return garageService.getRevenue(parsedDate, sector)
    }

    @Get("/garage")
    fun garageStatus(): GarageInfoDTO {
        return garageService.getGarage()
    }
}