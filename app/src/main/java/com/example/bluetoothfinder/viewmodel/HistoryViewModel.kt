package com.example.bluetoothfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothfinder.data.local.DeviceHistoryEntity
import com.example.bluetoothfinder.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Screen 3 (Riwayat Perangkat / History Log).
 * Membaca tabel `device_history` pada database lokal (Room).
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: DeviceRepository,
) : ViewModel() {

    val history: StateFlow<List<DeviceHistoryEntity>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _messages.tryEmit("Riwayat perangkat dibersihkan")
        }
    }
}
