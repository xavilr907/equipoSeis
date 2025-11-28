package com.univalle.inventarioapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

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

        // Si no hay usuario, enviarlo al login
        if (user == null) {
            val loginIntent = Intent(this, LoginActivity::class.java).apply {
                putExtra("fromWidget", fromWidget)  // mantenemos el origen
            }
            startActivity(loginIntent)
            finish()
            return
        }

        // Si sí está logueado, simplemente continúa con HomeFragment
        // (Tu navegación ya controlará qué fragment mostrar)
    }
}
