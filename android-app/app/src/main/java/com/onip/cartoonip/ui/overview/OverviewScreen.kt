package com.onip.cartoonip.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(onBack: () -> Unit) {
    val households by AppContainer.captureStore.households.collectAsState()
    val stats = remember(households) { computeOverviewStats(households) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aperçu") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("Aujourd'hui", stats.today, Modifier.weight(1f))
                    StatTile("Ce mois", stats.thisMonth, Modifier.weight(1f))
                    StatTile("Cette année", stats.thisYear, Modifier.weight(1f))
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Répartition par sexe", style = MaterialTheme.typography.titleSmall)
                        val total = (stats.male + stats.female).coerceAtLeast(1)
                        val malePct = stats.male * 100 / total
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.weight(malePct.toFloat().coerceAtLeast(0.001f))
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            ) {}
                            Row(
                                modifier = Modifier.weight((100 - malePct).toFloat().coerceAtLeast(0.001f))
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(MaterialTheme.colorScheme.tertiary),
                            ) {}
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Hommes : ${stats.male} ($malePct%)", style = MaterialTheme.typography.bodySmall)
                            Text("Femmes : ${stats.female} (${100 - malePct}%)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Historique journalier (14 derniers jours)", style = MaterialTheme.typography.titleSmall)
                        DailyHistoryChart(
                            data = stats.dailyHistory,
                            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 12.dp),
                        )
                    }
                }
            }

            item {
                Text(
                    "${stats.total} ménage${if (stats.total != 1) "s" else ""} enregistré${if (stats.total != 1) "s" else ""} sur cet appareil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DailyHistoryChart(data: List<DailyCount>, modifier: Modifier = Modifier) {
    val maxCount = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        data.forEach { day ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val heightFraction = (day.count.toFloat() / maxCount).coerceAtLeast(if (day.count > 0) 0.06f else 0f)
                    if (heightFraction < 1f) {
                        Row(modifier = Modifier.fillMaxWidth().weight((1f - heightFraction).coerceAtLeast(0.001f))) {}
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(heightFraction.coerceAtLeast(0.001f))
                            .background(MaterialTheme.colorScheme.primary),
                    ) {}
                }
                Text(
                    day.label.substringBefore("/"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
