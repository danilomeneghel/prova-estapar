package com.estapar.repository

import com.estapar.model.ToDo
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable

@Repository
interface ToDoRepository: JpaRepository<ToDo, Long> {

    fun findByStatus(status: Boolean, pageable: Pageable): Page<ToDo>
}