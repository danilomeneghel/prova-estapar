package com.estapar.dto

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ToDoRequestUpdate(
    @field:NotBlank(message = "Campo Nome deve ser preenchido!") 
    val nome: String, 

    @field:NotBlank(message = "Campo Descrição deve ser preenchido!") 
    val descricao: String, 
    
    val status: Boolean
)