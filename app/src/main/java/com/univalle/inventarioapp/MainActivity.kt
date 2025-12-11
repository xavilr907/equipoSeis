package com.univalle.inventarioapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.univalle.inventarioapp.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        // ✅ CORRECTO: Usa AuthRepository inyectado por Hilt
        val currentUser = authRepository.getCurrentUser()

        val fromWidget = intent.getBooleanExtra("fromWidget", false)

        // CRITERIO 1 HU3: Persistencia de sesión
        // Si no hay usuario, enviarlo al login
        if (currentUser == null) {
            val loginIntent = Intent(this, LoginActivity::class.java).apply {
                putExtra("fromWidget", fromWidget)
            }
            startActivity(loginIntent)
            finish()
            return
        }

        // Si está logueado, continúa con HomeFragment (nav_graph.xml)
    }
}
