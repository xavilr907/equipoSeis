package com.univalle.inventarioapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.univalle.inventarioapp.data.model.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación del repositorio de autenticación usando Firebase Auth.
 * Maneja operaciones de login y registro de manera asíncrona.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Resource.Success(user)
            } else {
                Resource.Error("Usuario no encontrado")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error desconocido al iniciar sesión")
        }
    }

    override suspend fun signUp(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Resource.Success(user)
            } else {
                Resource.Error("Error al crear usuario")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error desconocido al registrar usuario")
        }
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}

