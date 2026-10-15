package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bottom bar dengan tombol kontrol pemindaian: Stop (merah) dan Start/Scan (biru).
 *
 * @param startEnabled dinonaktifkan saat scan sedang berjalan atau ada kendala izin.
 * @param stopEnabled dinonaktifkan saat scan tidak berjalan.
 * @param startLabel label tombol start, "Scanning..." bila pemindaian berjalan.
 */
@Composable
fun BotBar(
    onStopClick: () -> Unit,
    onScanClick: () -> Unit,
    startEnabled: Boolean = true,
    stopEnabled: Boolean = true,
    startLabel: String = "Scan",
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shadowElevation = 24.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onStopClick,
                modifier = Modifier.weight(1f),
                colors = ButtonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.4f),
                    disabledContentColor = Color.White,
                ),
                enabled = stopEnabled,
                shape = CircleShape.copy(all = CornerSize(8.dp)),
            ) {
                Text(text = "Stop")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier.weight(1f),
                colors = ButtonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.4f),
                    disabledContentColor = Color.White,
                ),
                enabled = startEnabled,
                shape = CircleShape.copy(all = CornerSize(8.dp)),
            ) {
                Text(text = startLabel)
            }
        }
    }
}
