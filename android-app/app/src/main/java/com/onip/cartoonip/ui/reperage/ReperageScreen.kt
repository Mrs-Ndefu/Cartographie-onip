package com.onip.cartoonip.ui.reperage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.model.CapturedSite
import com.onip.cartoonip.ui.theme.OnipBlue
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReperageScreen(onBack: () -> Unit, onNew: () -> Unit, onEdit: (String) -> Unit, viewModel: ReperageViewModel = viewModel()) {
    val sites by viewModel.sites.collectAsState()
    val syncingIds by viewModel.syncingIds.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<CapturedSite?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    val pendingCount = sites.count { it.syncedAt == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repérage de sites", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = "Nouvelle fiche") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ReperageSummaryHeader(total = sites.size, pending = pendingCount)

            if (sites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            "Aucune fiche de repérage pour l'instant.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sites, key = { it.id }) { site ->
                        SiteRow(
                            site = site,
                            isSyncing = syncingIds.contains(site.id),
                            onRetry = { viewModel.retrySync(site) },
                            onEdit = { onEdit(site.id) },
                            onDelete = { deleteTarget = site },
                        )
                    }
                }
            }
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Supprimer cette fiche ?") },
                text = { Text("${target.displayName} sera supprimée de cet appareil. Cette action est définitive.") },
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
private fun ReperageSummaryHeader(total: Int, pending: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "$total fiche${if (total != 1) "s" else ""} de repérage",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = OnipBlue,
        )
        if (pending > 0) {
            Text(
                "$pending en attente de synchronisation",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SiteRow(
    site: CapturedSite,
    isSyncing: Boolean,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val synced = site.syncedAt != null
    val accentColor = if (synced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accentColor))

            Column(Modifier.weight(1f).padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        site.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnipBlue,
                        modifier = Modifier.weight(1f),
                    )
                    SyncBadge(synced = synced, isSyncing = isSyncing, onRetry = onRetry)
                }
                if (site.typeSite != null) {
                    Text(
                        TYPE_SITE_LABELS[site.typeSite] ?: site.typeSite,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    formatCreatedAt(site.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
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
private fun SyncBadge(synced: Boolean, isSyncing: Boolean, onRetry: () -> Unit) {
    when {
        isSyncing -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
        synced -> Icon(Icons.Filled.CheckCircle, contentDescription = "Synchronisé", tint = MaterialTheme.colorScheme.secondary)
        else -> IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Sync, contentDescription = "Relancer la synchronisation", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatCreatedAt(raw: String): String =
    runCatching { DATE_TIME_FORMAT.format(Instant.parse(raw)) }.getOrDefault(raw)

private val TYPE_SITE_LABELS = mapOf(
    "ECOLE" to "École",
    "EGLISE" to "Église",
    "ADMINISTRATIF" to "Administratif",
    "HOPITAL" to "Hôpital",
    "AUTRE" to "Autre",
)
