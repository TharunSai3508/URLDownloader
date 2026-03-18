package com.example.urldownloader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urldownloader.data.MediaExtractor
import com.example.urldownloader.download.Downloader
import com.example.urldownloader.model.MediaItem
import com.example.urldownloader.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val items: List<MediaItem>) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedUrls = MutableStateFlow<Set<String>>(emptySet())
    val selectedUrls: StateFlow<Set<String>> = _selectedUrls.asStateFlow()

    private var allItems: List<MediaItem> = emptyList()
    private var activeFilter: MediaType? = null

    // ── Analysis ──────────────────────────────────────────────────────────────

    fun analyze(url: String) {
        if (MediaExtractor.resolveInput(url) == null) return
        _selectedUrls.value = emptySet()
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                withContext(Dispatchers.IO) { MediaExtractor.extract(url) }
            }.onSuccess { items ->
                allItems     = items
                activeFilter = null
                _uiState.value = if (items.isEmpty())
                    UiState.Error("No downloadable content found at this URL.")
                else
                    UiState.Success(items)
            }.onFailure { e ->
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to analyze URL. Check your connection.")
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    fun setFilter(type: MediaType?) {
        activeFilter = type
        _uiState.value = UiState.Success(
            if (type == null) allItems else allItems.filter { it.type == type }
        )
    }

    fun getActiveFilter(): MediaType? = activeFilter

    fun getAvailableTypes(): List<MediaType> = allItems.map { it.type }.distinct()

    // ── Selection ─────────────────────────────────────────────────────────────

    fun toggleSelect(url: String) {
        _selectedUrls.update { if (url in it) it - url else it + url }
    }

    fun selectAll() {
        val visible = ((_uiState.value as? UiState.Success)?.items ?: allItems).map { it.url }
        _selectedUrls.value = visible.toSet()
    }

    fun deselectAll() {
        _selectedUrls.value = emptySet()
    }

    fun downloadSelected(context: Context) {
        val items = allItems.filter { it.url in _selectedUrls.value }
        Downloader.downloadAll(context, items)
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    fun clear() {
        allItems     = emptyList()
        activeFilter = null
        _selectedUrls.value = emptySet()
        _uiState.value = UiState.Idle
    }
}
