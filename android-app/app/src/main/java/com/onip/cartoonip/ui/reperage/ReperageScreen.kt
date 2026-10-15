package com.onip.cartoonip.ui.reperage

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.model.ReperagePoint
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReperageScreen(onBack: () -> Unit, viewModel: ReperageViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val points by viewModel.points.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) viewModel.markHere()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repérage") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Marquez les positions à visiter avant d'y faire l'adressage complet — utile pour une porte "
                        + "fermée, un accès refusé, ou pour reconnaître une zone avant la tournée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.locationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    enabled = !uiState.locating,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    if (uiState.locating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Marquer ce point")
                    }
                }
            }

            if (points.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Aucun point repéré pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val aVisiter = points.filterNot { it.visite }
                val visites = points.filter { it.visite }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "${aVisiter.size} à visiter" + if (visites.isNotEmpty()) " · ${visites.size} traité(s)" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(aVisiter + visites, key = { it.id }) { point ->
                        ReperagePointCard(
                            point = point,
                            onVisiteChange = { viewModel.setVisite(point.id, it) },
                            onNoteChange = { viewModel.updateNote(point.id, it) },
                            onDelete = { viewModel.delete(point.id) },
                        )
                    }
                    item { Column(Modifier.padding(bottom = 16.dp)) {} }
                }
            }
        }
    }
}

@Composable
private fun ReperagePointCard(
    point: ReperagePoint,
    onVisiteChange: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var note by remember(point.id) { mutableStateOf(point.note) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = point.visite, onCheckedChange = onVisiteChange)
                Column(Modifier.weight(1f)) {
                    Text(
                        formatCoordinates(point.latitude, point.longitude),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    point.locationPrecision?.let {
                        Text(
                            "Précision : ${it.roundToInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer ce point")
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it; onNoteChange(it) },
                label = { Text("Note (facultatif)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

private fun formatCoordinates(latitude: Double, longitude: Double): String =
    "%.5f, %.5f".format(latitude, longitude)
