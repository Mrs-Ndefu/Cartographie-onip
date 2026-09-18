package com.onip.cartoonip.ui.households

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.onip.cartoonip.data.model.HouseholdDto
import com.onip.cartoonip.data.model.HouseholdStatus
import com.onip.cartoonip.ui.common.AppScaffold
import com.onip.cartoonip.ui.navigation.Routes

@Composable
fun HouseholdListScreen(navController: NavHostController, viewModel: HouseholdListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(navController = navController, currentRoute = Routes.HOUSEHOLDS, title = "Ménages") { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null && uiState.households.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.households, key = { it.id }) { household ->
                    HouseholdRow(household) {
                        navController.navigate(Routes.householdDetail(household.id))
                    }
                }
                item {
                    if (uiState.hasMore) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator()
                            } else {
                                Button(onClick = viewModel::loadMore) { Text("Charger plus") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseholdRow(household: HouseholdDto, onClick: () -> Unit) {
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(household.codeMenage ?: "(sans code)", style = MaterialTheme.typography.titleMedium)
                Text(statusLabel(household.status), style = MaterialTheme.typography.labelMedium)
            }
            val chef = household.chef
            if (chef != null) {
                Text(
                    listOfNotNull(chef.nom, chef.postnom, chef.prenom).joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val address = household.address
            if (address != null) {
                Text(
                    listOfNotNull(address.commune, address.quartier).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun statusLabel(status: HouseholdStatus): String = when (status) {
    HouseholdStatus.COMPLET -> "Complet"
    HouseholdStatus.BROUILLON -> "Brouillon"
    HouseholdStatus.A_VERIFIER -> "À vérifier"
}
