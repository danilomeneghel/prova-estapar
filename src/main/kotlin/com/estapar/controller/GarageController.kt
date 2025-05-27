package com.estapar.controller

import com.estapar.service.GarageService
import io.micronaut.http.annotation.*
import java.time.LocalDate
import io.micronaut.validation.Validated
import javax.validation.Valid

@Validated
@Controller("/api")
class GarageController(
    private val garageService: GarageService
) {
    @Post("/plate-status")
    fun plateStatus(@Body @Valid body: Map<String, String>) =
        garageService.getStatusByPlate(body["license_plate"] ?: "")

    @Post("/spot-status")
    fun spotStatus(@Body @Valid body: Map<String, Double>) =
        garageService.getStatusBySpot(body["lat"] ?: 0.0, body["lng"] ?: 0.0)

    @Get("/revenue")
    fun revenue(@QueryValue date: String, @QueryValue sector: String): Map<String, Any> {
        val parsedDate = LocalDate.parse(date)
        return garageService.getRevenue(parsedDate, sector)
    }

    @Get("/garage")
    fun garageStatus(): Map<String, Any> =
        garageService.getGarageStatus()
    
}