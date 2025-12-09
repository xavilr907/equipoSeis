package com.univalle.inventarioapp.data.model

/**
 * Sealed class genérica para manejar estados de operaciones asíncronas.
 * Usado para encapsular respuestas de Firebase Auth y Firestore.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

