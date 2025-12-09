package com.univalle.inventarioapp

import android.app.Application
import androidx.room.Room
import com.univalle.inventarioapp.data.local.AppDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "inventory.db" // nombre de la base de datos
        )
            // si cambias la versión o estructura, borra y crea la BD nueva automáticamente
            .fallbackToDestructiveMigration()
            .build()
    }
}
