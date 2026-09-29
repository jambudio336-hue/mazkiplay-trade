package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazkiplay.trade.ui.theme.Bear
import com.mazkiplay.trade.ui.theme.Gold

/** No feature content is composed until Android reports validated internet. */
@Composable
fun OnlineRequiredScreen() {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ONLINE CONNECTION REQUIRED", color = Bear, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, textAlign = TextAlign.Center)
        Text("Mazkiplay Trade tidak dibuka dalam mode offline.", color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        Text("Sambungkan Wi-Fi atau data seluler. Aplikasi akan membuka otomatis setelah koneksi tervalidasi dan feed market dapat dijangkau.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        CircularProgressIndicator(color = Gold, modifier = Modifier.padding(top = 24.dp))
        Text("Menunggu koneksi internet…", color = Gold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
        Text("Semua fitur online-only: market data, signal, news, calendar, bot, AI provider, dan live events.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
    }
}
