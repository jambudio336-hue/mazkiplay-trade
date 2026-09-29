package com.mazkiplay.trade.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.data.model.AcademyLesson
import com.mazkiplay.trade.ui.components.Pill
import com.mazkiplay.trade.ui.components.SectionCard
import com.mazkiplay.trade.ui.theme.Bull
import com.mazkiplay.trade.ui.theme.Gold

@Composable
fun AcademyScreen(app: MazkiplayApp) {
    var selected by remember { mutableStateOf<AcademyLesson?>(null) }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("M4ZK1PLAYNUSANTARA ACADEMY", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Belajar → paper trade → analisa → risk → journal", style = MaterialTheme.typography.bodySmall) }
        item { SectionCard(title = "Original educational library", subtitle = "Reader tersedia offline setelah dibuka", trailing = { Pill("EDUCATION", Gold) }) { Text("Materi ini original untuk Mazkiplay Trade. Bukan rekomendasi investasi, ajakan membeli/menjual aset, atau jaminan keuntungan. Trading berisiko kehilangan modal.", style = MaterialTheme.typography.bodySmall) } }
        items(app.featurePack.lessons) { lesson ->
            SectionCard(modifier = Modifier.clickable { selected = lesson }, title = "${lesson.icon} ${lesson.title}", subtitle = lesson.summary, trailing = { Pill("v${lesson.version}", Bull) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { selected = lesson }) { Text("READ") }; Text("DOWNLOAD · local reader", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp)) }
            }
        }
    }
    selected?.let { lesson ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(lesson.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { lesson.sections.forEachIndexed { i, text -> Text("${i + 1}. $text") }; Text("M4zk1pLayNusantara · Mazkiplay Trade Academy · Educational material · Version ${lesson.version}", style = MaterialTheme.typography.labelSmall, color = Gold) } }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Tutup") } })
    }
}
