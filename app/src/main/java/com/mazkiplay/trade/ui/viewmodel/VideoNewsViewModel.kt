package com.mazkiplay.trade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.VideoNewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoNewsViewModel(private val app: MazkiplayApp) : ViewModel() {
    val videos: StateFlow<List<VideoNewsItem>> = app.videoNewsRepository.videos
    val lastUpdated: StateFlow<Long> = app.videoNewsRepository.lastUpdated
    val error: StateFlow<String?> = app.videoNewsRepository.error

    private val _selected = MutableStateFlow<VideoNewsItem?>(null)
    val selected: StateFlow<VideoNewsItem?> = _selected.asStateFlow()
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    init {
        viewModelScope.launch {
            videos.collect { list ->
                if (_selected.value == null || _selected.value?.videoId !in list.map { it.videoId }) {
                    _selected.value = list.firstOrNull()
                    _playing.value = false
                }
            }
        }
    }

    fun select(item: VideoNewsItem) {
        _selected.value = item
        _playing.value = false
    }

    fun togglePlaying() { _playing.value = !_playing.value }
    fun stop() { _playing.value = false }
    fun refresh() { viewModelScope.launch { app.videoNewsRepository.refresh() } }
}
