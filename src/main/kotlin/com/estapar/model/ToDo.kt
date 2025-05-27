package com.estapar.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class ToDo() {
    @Id
    @GeneratedValue
    val id: Long? = null
    
    var nome: String? = null

    var descricao: String? = null 
    
    var status: Boolean = false
    
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    val criadoEm: LocalDateTime = LocalDateTime.now()
}