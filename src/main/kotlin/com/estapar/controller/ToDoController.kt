package com.estapar.controller

import com.estapar.model.ToDo
import com.estapar.dto.ToDoRequest
import com.estapar.dto.ToDoRequestUpdate
import com.estapar.service.ToDoService
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import javax.validation.Valid

@Validated
@Controller("/todo")
class ToDoController(private val toDoService: ToDoService) {

    @Get
    fun listar(@QueryValue status: Boolean?, pageable: Pageable): HttpResponse<Page<ToDo>>{
        return HttpResponse.ok(toDoService.listar(status, pageable))
    }

    @Get("/{id}")
    fun buscar(@PathVariable id: Long): HttpResponse<ToDo> {
        return HttpResponse.ok(toDoService.buscar(id))
    }

    @Post
    fun salvar(@Body @Valid toDoRequest: ToDoRequest): HttpResponse<ToDo> {
        return HttpResponse.created(toDoService.salvar(toDoRequest))
    }

    @Put("/{id}")
    fun atualizar(@PathVariable id: Long, @Body @Valid toDo: ToDoRequestUpdate): HttpResponse<ToDo> {
        return HttpResponse.ok(toDoService.atualizar(id, toDo))
    }

    @Delete("/{id}")
    fun deletar(@PathVariable id: Long): HttpResponse<Unit> {
        toDoService.deletar(id)
        return HttpResponse.noContent()
    }
}