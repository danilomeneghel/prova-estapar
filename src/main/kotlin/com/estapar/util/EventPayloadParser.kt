package com.estapar.util

import java.time.Instant

object EventPayloadParser {
    fun parseInstant(value: Any?): Instant {
        return when (value) {
            is Long -> Instant.ofEpochMilli(value)
            is String -> Instant.parse(value)
            else -> throw IllegalArgumentException("Invalid or missing timestamp. Expected Long or String, got ${value?.javaClass?.simpleName}")
        }
    }

    fun parseNumber(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid or missing number. Expected Number or parseable String, got ${value?.javaClass?.simpleName}")
            else -> throw IllegalArgumentException("Invalid or missing number. Expected Number or String, got ${value?.javaClass?.simpleName}")
        }
    }
}