package com.onip.cartoonip.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.onip.cartoonip.data.model.DashboardStatsDto
import com.onip.cartoonip.data.model.MonthlyCount
import com.onip.cartoonip.ui.common.AppScaffold
import com.onip.cartoonip.ui.navigation.Routes

@Composable
fun DashboardScreen(navController: NavHostController, viewModel: DashboardViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(navController = navController, currentRoute = Routes.DASHBOARD, title = "Tableau de bord") { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.stats != null -> DashboardContent(stats = uiState.stats!!, fullName = viewModel.adminFullName, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun DashboardContent(stats: DashboardStatsDto, fullName: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Bonjour, $fullName", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Total ménages", stats.total.toString(), Modifier.weight(1f))
                StatCard("Complets", stats.countComplet.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Brouillons", stats.countBrouillon.toString(), Modifier.weight(1f))
                StatCard("À vérifier", stats.countAVerifier.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Enregistrements", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LabeledCount("Aujourd'hui", stats.registrationCounts.today)
                        LabeledCount("Ce mois", stats.registrationCounts.thisMonth)
                        LabeledCount("Cette année", stats.registrationCounts.thisYear)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Répartition par sexe", style = MaterialTheme.typography.titleSmall)
                    val male = stats.sexDistribution["M"] ?: 0L
                    val female = stats.sexDistribution["F"] ?: 0L
                    val total = (male + female).coerceAtLeast(1L)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hommes : $male (${male * 100 / total}%)")
                        Text("Femmes : $female (${female * 100 / total}%)")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Enregistrements par mois (12 derniers mois)", style = MaterialTheme.typography.titleSmall)
                    MonthlyBarChart(data = stats.monthlyRegistrations, modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LabeledCount(label: String, value: Long) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MonthlyBarChart(data: List<MonthlyCount>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxCount = (data.maxOfOrNull { it.count } ?: 0L).coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val barWidth = size.width / (data.size * 1.5f)
        val gap = barWidth * 0.5f
        data.forEachIndexed { index, monthly ->
            val barHeight = (monthly.count.toFloat() / maxCount) * size.height
            val x = index * (barWidth + gap)
            drawRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}
