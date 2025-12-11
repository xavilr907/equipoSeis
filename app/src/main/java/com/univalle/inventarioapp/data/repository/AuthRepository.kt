package com.univalle.inventarioapp.data.repository

import com.google.firebase.auth.FirebaseUser
import com.univalle.inventarioapp.data.model.Resource

/**
 * Interfaz del repositorio de autenticación.
 * Define contratos para operaciones de login y registro con Firebase Auth.
 */
interface AuthRepository {

    /**
     * Inicia sesión con email y contraseña.
     * @param email Email del usuario
     * @param password Contraseña del usuario (6-10 dígitos numéricos)
     * @return Resource<FirebaseUser> con el resultado de la operación
     */
    suspend fun signIn(email: String, password: String): Resource<FirebaseUser>

    /**
     * Registra un nuevo usuario con email y contraseña.
     * @param email Email del usuario
     * @param password Contraseña del usuario (6-10 dígitos numéricos)
     * @return Resource<FirebaseUser> con el resultado de la operación
     */
    suspend fun signUp(email: String, password: String): Resource<FirebaseUser>

    /**
     * Obtiene el usuario actualmente autenticado.
     * @return FirebaseUser? Usuario actual o null si no hay sesión
     */
    fun getCurrentUser(): FirebaseUser?
}

