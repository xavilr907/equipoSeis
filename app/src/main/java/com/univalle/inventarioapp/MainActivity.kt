package com.univalle.inventarioapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ===== PRUEBA RÁPIDA DE FIRESTORE =====
        val db = Firebase.firestore

        val testProduct = hashMapOf(
            "code" to "0001",
            "name" to "Producto Prueba",
            "priceCents" to 1500,   // int/long
            "quantity" to 5         // int
        )

        db.collection("products")
            .add(testProduct)
            .addOnSuccessListener { doc ->
                Log.d("FIREBASE_TEST", "Guardado con id: ${doc.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_TEST", "Error: ${e.message}", e)
            }
        // ======================================
    }
}
