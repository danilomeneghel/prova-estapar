package com.estapar.exception

import java.time.LocalDateTime

class ErrorMessage(
    var status: Int? = null,
    var message: String? = null,
    var path: String? = null
) 
{
    val timeStamp: String = LocalDateTime.now().toString()
}