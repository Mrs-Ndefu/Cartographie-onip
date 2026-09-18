package com.onip.cartoonip.ui.households

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.onip.cartoonip.data.model.HouseholdDto
import com.onip.cartoonip.data.model.PersonDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdDetailScreen(householdId: String, navController: NavHostController) {
    val viewModel: HouseholdDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { HouseholdDetailViewModel(householdId) } },
    )
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.household?.codeMenage ?: "Ménage") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }, enabled = uiState.household != null && !uiState.isDeleting) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null && uiState.household == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.household != null -> HouseholdDetailContent(household = uiState.household!!, modifier = Modifier.padding(padding))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce ménage ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun HouseholdDetailContent(household: HouseholdDto, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { InfoCard("Statut", household.status.name) }
        item { InfoCard("Agent", household.agentUsername ?: "—") }

        val chef = household.chef
        if (chef != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Chef de ménage", style = MaterialTheme.typography.titleSmall)
                        PersonLine(chef)
                    }
                }
            }
        }

        if (household.membres.isNotEmpty()) {
            item { Text("Membres (${household.membres.size})", style = MaterialTheme.typography.titleSmall) }
            items(household.membres) { member ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) { PersonLine(member) }
                }
            }
        }

        val address = household.address
        if (address != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Adresse", style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(address.rue, address.numero, address.quartier, address.commune, address.ville).joinToString(", "))
                    }
                }
            }
        }

        val meta = household.meta
        if (meta != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Encodage", style = MaterialTheme.typography.titleSmall)
                        Text("Fait à : ${meta.faitA ?: "—"}")
                        Text("Date : ${meta.dateEncodage ?: "—"}")
                        Text("Agent cartographe : ${meta.agentCarthographe ?: "—"}")
                        Text("Renseignant : ${meta.renseignant ?: "—"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonLine(person: PersonDto) {
    Text(listOfNotNull(person.nom, person.postnom, person.prenom).joinToString(" "))
    Text(
        listOfNotNull(person.relation, person.sexe?.name, person.dateNaissance).joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
