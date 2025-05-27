package com.estapar.service

import com.estapar.model.ToDo
import com.estapar.dto.ToDoRequest
import com.estapar.dto.ToDoRequestUpdate
import com.estapar.repository.ToDoRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import jakarta.inject.Singleton
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Singleton
open class ToDoService(private val toDoRepository: ToDoRepository) {
    
    fun listar(status: Boolean?, pageable: Pageable): Page<ToDo>? {
        if(status != null){
            return toDoRepository.findByStatus(status, pageable)
        }
        return toDoRepository.findAll(pageable)
    }

    fun buscar(id: Long): ToDo? {
        return toDoRepository.findById(id).orElseThrow{EntityNotFoundException("ToDo Não encontrado!")}
    }

    fun salvar(toDoRequest: ToDoRequest): ToDo {
        val toDo = ToDo()
        toDo.nome = toDoRequest.nome
        toDo.descricao = toDoRequest.descricao
        toDo.status = toDoRequest.status
        return toDoRepository.save(toDo)
    }
    
    fun atualizar(id: Long, toDo: ToDoRequestUpdate): ToDo? {
        val toDoDb = buscar(id)
        toDoDb?.nome = toDo.nome
        toDoDb?.descricao = toDo.descricao
        toDoDb?.status = toDo.status
        return toDoDb?.let { toDoRepository.update(it) }

    }

    fun deletar(id: Long): Any {
        val toDoDb = buscar(id)
        return toDoRepository.delete(toDoDb!!)
    }

}