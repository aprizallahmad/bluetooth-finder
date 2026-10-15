package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Bobot lebar kolom tabel daftar perangkat: Perangkat | RSSI | Jarak.
 * Dipakai bersama oleh header dan setiap baris data sehingga garis selalu sejajar.
 */
val DeviceTableWeights = listOf(1.4f, 0.8f, 1.2f)

/**
 * Menggambar garis tabel (garis atas, garis bawah, dan pemisah kolom) **sekali di level baris**.
 *
 * Karena digambar berdasarkan ukuran baris (bukan per sel), semua garis lurus, tebal garis
 * seragam 1 dp, dan tidak ada garis ganda antar kolom yang berdampingan.
 */
@Composable
fun Modifier.deviceTableGrid(): Modifier {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    return drawBehind {
        val stroke = 1.dp.toPx()
        val totalWeight = DeviceTableWeights.sum()

        // Pemisah kolom (termasuk tepi kiri & kanan tabel)
        var accumulated = 0f
        val columnPositions = mutableListOf(0f)
        DeviceTableWeights.forEach { weight ->
            accumulated += weight
            columnPositions.add(size.width * accumulated / totalWeight)
        }
        columnPositions.forEach { x ->
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
        }

        // Garis atas & bawah baris
        drawLine(lineColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = stroke)
        drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke)
    }
}

/**
 * Sel tabel: hanya wadah konten dengan padding konsisten.
 * Garis berasal dari [deviceTableGrid] milik baris induknya.
 */
@Composable
fun TableCell(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** Sel tabel berisi satu baris teks rata tengah (dipakai header & kolom RSSI/Jarak). */
@Composable
fun TitleText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
) {
    TableCell(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = fontWeight,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            textAlign = TextAlign.Center,
        )
    }
}
