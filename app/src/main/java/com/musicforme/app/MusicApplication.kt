package com.musicforme.app

import android.app.Application
import com.musicforme.app.data.local.AppDatabase
import com.musicforme.app.data.local.MediaStoreScanner
import com.musicforme.app.data.repository.MusicRepository
import com.musicforme.app.service.PlayerManager

class MusicApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        val scanner = MediaStoreScanner(this)
        repository = MusicRepository(scanner, database.favoriteDao())

        // Initialize PlayerManager with application context
        PlayerManager.init(this)
    }

    companion object {
        lateinit var instance: MusicApplication
            private set
    }
}
