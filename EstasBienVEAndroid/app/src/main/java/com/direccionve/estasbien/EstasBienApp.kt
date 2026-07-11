package com.direccionve.estasbien

import android.app.Application
import com.direccionve.estasbien.service.EarthquakeCheckWorker

class EstasBienApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start periodic earthquake checks
        EarthquakeCheckWorker.schedule(this)
    }
}
