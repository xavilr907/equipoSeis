package com.univalle.inventarioapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val fromWidget = intent.getBooleanExtra("fromWidget", false)

        // CRITERIO 1 HU3: Persistencia de sesión
        // Si no hay usuario, enviarlo al login
        if (user == null) {
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
