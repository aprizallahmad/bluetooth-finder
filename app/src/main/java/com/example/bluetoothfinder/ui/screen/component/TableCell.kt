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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Sel tabel dengan garis pembatas di keempat sisinya (dipakai header & baris data).
 */
@Composable
fun BorderedCell(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                // Top & bottom
                drawLine(lineColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
                // Start & end
                drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth)
                drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth)
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** Sel tabel berisi satu baris teks (dipakai untuk kolom RSSI dan Jarak). */
@Composable
fun TitleText(text: String, modifier: Modifier = Modifier) {
    BorderedCell(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
