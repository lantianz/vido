package com.vido.local

import android.app.Application
import com.vido.local.data.AppDatabase
import com.vido.local.data.MediaRepository
import com.vido.local.data.SettingsRepository

class VidoApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val mediaRepository by lazy {
        MediaRepository(this, database.privateMediaDao(), database.directoryDao(), database.playbackDao())
    }
}
