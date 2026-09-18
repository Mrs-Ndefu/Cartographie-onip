package com.onip.cartoonip.ui.journal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.model.CapturedHousehold
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onBack: () -> Unit, onEdit: (String) -> Unit, viewModel: JournalViewModel = viewModel()) {
    val households by viewModel.households.collectAsState()
    val syncingIds by viewModel.syncingIds.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<CapturedHousehold?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    val pendingCount = households.count { !it.isFullySynced }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Journal")
                        Text(
                            "${households.size} ménage${if (households.size != 1) "s" else ""}" +
                                if (pendingCount > 0) " · $pendingCount en attente" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    if (pendingCount > 0) {
                        TextButton(onClick = viewModel::syncAllPending) { Text("Tout synchroniser") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (households.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        "Aucun ménage enregistré pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(households, key = { it.id }) { household ->
                    JournalRow(
                        household = household,
                        isSyncing = syncingIds.contains(household.id),
                        onRetry = { viewModel.retrySync(household) },
                        onEdit = { onEdit(household.id) },
                        onDelete = { deleteTarget = household },
                    )
                }
            }
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Supprimer ce ménage ?") },
                text = { Text("${target.codeMenage} — ${target.chefFullName.ifBlank { "(sans nom)" }} sera supprimé de cet appareil. Cette action est définitive.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(target)
                        deleteTarget = null
                    }) { Text("Supprimer") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Annuler") }
                },
            )
        }
    }
}

@Composable
private fun JournalRow(
    household: CapturedHousehold,
    isSyncing: Boolean,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accentColor = if (household.isFullySynced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accentColor))

            if (household.photoPath != null) {
                JournalThumbnail(path = household.photoPath)
            }

            Column(Modifier.weight(1f).padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(household.codeMenage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    SyncBadge(household = household, isSyncing = isSyncing, onRetry = onRetry)
                }
                Text(
                    household.chefFullName.ifBlank { "(sans nom)" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    listOfNotNull(household.commune.ifBlank { null }, household.quartier.ifBlank { null }).joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        TIME_FORMAT.format(runCatching { Instant.parse(household.createdAt) }.getOrDefault(Instant.now())),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    if (household.membres.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Groups, contentDescription = "Membres",
                            modifier = Modifier.size(14.dp).padding(start = 10.dp), tint = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            " ${household.membres.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    if (household.photoPath != null) {
                        Icon(
                            Icons.Filled.PhotoCamera, contentDescription = "Photo jointe",
                            modifier = Modifier.size(14.dp).padding(start = 10.dp), tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).padding(start = 4.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalThumbnail(path: String) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }
    bitmapState.value?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo de la fiche",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(88.dp).padding(8.dp).clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun SyncBadge(household: CapturedHousehold, isSyncing: Boolean, onRetry: () -> Unit) {
    when {
        isSyncing -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
        household.isFullySynced -> Icon(Icons.Filled.CheckCircle, contentDescription = "Synchronisé", tint = MaterialTheme.colorScheme.secondary)
        else -> IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Sync, contentDescription = "Relancer la synchronisation", tint = MaterialTheme.colorScheme.error)
        }
    }
}
