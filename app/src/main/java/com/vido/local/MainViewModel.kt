package com.vido.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vido.local.data.ScanResult
import com.vido.local.data.PlayerMode
import com.vido.local.data.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VidoApplication
    private val repository = app.mediaRepository

    private val _systemVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val systemVideos: StateFlow<List<VideoItem>> = _systemVideos.asStateFlow()
    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()
    private val _playbackQueue = MutableStateFlow<List<VideoItem>>(emptyList())
    val playbackQueue: StateFlow<List<VideoItem>> = _playbackQueue.asStateFlow()
    val privateVideos = repository.privateVideos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val privateDirectories = repository.privateDirectories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val privateExtensions = app.settingsRepository.privateExtensions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )
    val showPrivateThumbnails = app.settingsRepository.showPrivateThumbnails.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )
    val autoPlayNext = app.settingsRepository.autoPlayNext.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )
    val playerMode = app.settingsRepository.playerMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlayerMode.IN_APP,
    )

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private val scanMutex = Mutex()

    fun loadSystemVideos() = viewModelScope.launch {
        _systemVideos.value = repository.loadSystemVideos()
    }

    fun selectVideo(video: VideoItem, queue: List<VideoItem> = listOf(video)) {
        _selectedVideo.value = video
        _playbackQueue.value = queue.ifEmpty { listOf(video) }
    }

    fun selectQueueItem(video: VideoItem) {
        _selectedVideo.value = video
    }

    fun addPrivateDirectory(uri: String, name: String) = viewModelScope.launch {
        repository.addDirectory(uri, name)
        refreshPrivateDirectories(app.settingsRepository.privateExtensions.first())
    }

    fun removePrivateDirectory(uri: String) = viewModelScope.launch {
        repository.removeDirectory(uri)
    }

    fun scanPrivateDirectories() = viewModelScope.launch {
        refreshPrivateDirectories(app.settingsRepository.privateExtensions.first())
    }

    private suspend fun refreshPrivateDirectories(extensions: Set<String> = privateExtensions.value) {
        scanMutex.withLock {
            _isScanning.value = true
            _scanResult.value = runCatching {
                repository.scanPrivateDirectories(extensions)
            }.getOrNull()
            _isScanning.value = false
        }
    }

    fun addExtension(value: String, onInvalid: () -> Unit) = viewModelScope.launch {
        if (!app.settingsRepository.addExtension(value)) onInvalid()
        else refreshPrivateDirectories(app.settingsRepository.privateExtensions.first())
    }

    fun removeExtension(value: String) = viewModelScope.launch {
        app.settingsRepository.removeExtension(value)
        refreshPrivateDirectories(app.settingsRepository.privateExtensions.first())
    }

    fun setShowPrivateThumbnails(enabled: Boolean) = viewModelScope.launch {
        app.settingsRepository.setShowPrivateThumbnails(enabled)
    }

    fun setAutoPlayNext(enabled: Boolean) = viewModelScope.launch {
        app.settingsRepository.setAutoPlayNext(enabled)
    }

    fun setPlayerMode(mode: PlayerMode) = viewModelScope.launch {
        app.settingsRepository.setPlayerMode(mode)
    }

    suspend fun playbackPosition(uri: String): Long = repository.playbackPosition(uri)

    fun savePlaybackPosition(uri: String, positionMs: Long) = viewModelScope.launch {
        repository.savePlaybackPosition(uri, positionMs)
    }
}
