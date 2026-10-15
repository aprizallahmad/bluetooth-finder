package com.example.bluetoothfinder.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.bluetoothfinder.domain.SignalCategory

/**
 * Warna indikator per kategori sinyal:
 * hijau = sangat dekat -> abu = sinyal hilang.
 */
val SignalCategory.color: Color
    get() = when (this) {
        SignalCategory.VERY_STRONG -> Color(0xFF16A34A)
        SignalCategory.STRONG -> Color(0xFF22C55E)
        SignalCategory.GOOD -> Color(0xFFF59E0B)
        SignalCategory.WEAK -> Color(0xFFF97316)
        SignalCategory.VERY_WEAK -> Color(0xFFEF4444)
        SignalCategory.LOST -> Color(0xFF9CA3AF)
    }

/** Warna RSSI mentah agar konsisten dengan warna kategorinya. */
fun signalColor(rssi: Int): Color = SignalCategory.fromRssi(rssi).color
