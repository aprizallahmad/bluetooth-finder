package com.example.bluetoothfinder.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bluetoothfinder.ui.screen.component.BotBar
import com.example.bluetoothfinder.ui.screen.component.HeaderData
import com.example.bluetoothfinder.ui.theme.BluetoothFinderTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController? = null,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Bluetooth Finder")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Blue.copy(alpha = 0.4f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
            )
        },
        bottomBar = {
            BotBar(
                onStopClick = { },
                onScanClick = {}
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SearchBar(
                query = "",
                onQueryChange = { },
                onSearch = { },
                active = false,
                onActiveChange = { },
                placeholder = { Text(text = "Search") },
                leadingIcon = { },
                trailingIcon = { },
            ) { }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
            ) {
                item {
                    HeaderData()
                }

                items(23) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController?.navigate("detail")
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TitleText(text = "RSSI $it", modifier = Modifier.weight(1f))
                        TitleText(text = "$it Meter", modifier = Modifier.weight(1f))
                        TitleText(
                            text = (
                                    if (it < 1) "Sangat Dekat"
                                    else if (it <= 3) "Kuat"
                                    else if (it <= 10) "Lemah"
                                    else if (it > 20) "Sangat Lemah / Putus Putuh"
                                    else "Sinyal Hilang (Lost) "
                                    ),
                            modifier = Modifier.weight(1f)
                        )

                    }
                }
            }
        }
    }
}


@Composable
fun TitleText(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val color = Color.Black

                // 1. Border Top
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )

                // 2. Border Bottom
                drawLine(
                    color = color,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )

                // 3. Border Start (Kiri) - Opsional jika ingin aktif
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = strokeWidth
                )

                // 4. Border End (Kanan) - Aktif untuk pembatas antar kolom
                drawLine(
                    color = color,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
//            .border(
//                width = 1.dp,
//                color = Black,
//                shape = ShapeDefaults.Small.copy(
//                    topStart = ShapeDefaults.Small.topStart,
//
//                )
//            )
            .padding(horizontal = 4.dp),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothFinderTheme {
        HomeScreen()
    }
}
