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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import com.onip.cartoonip.data.model.AgentZone
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
fun ReperageScreen(
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onVisitSite: (String) -> Unit = {},
    viewModel: ReperageViewModel = viewModel(),
) {
    val sites by viewModel.sites.collectAsState()
    val zoneVisits by viewModel.zoneVisits.collectAsState()
    val agentZone by viewModel.agentZone.collectAsState()
    val otherFiches by viewModel.otherFiches.collectAsState()
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
            // Chef d'équipe : une fiche se commence en touchant un site de la liste.
            if (!viewModel.showZoneSites) {
                FloatingActionButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = "Nouvelle fiche") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (viewModel.showZoneSites) {
                ZoneSitesList(
                    zone = agentZone,
                    visits = zoneVisits,
                    pending = pendingCount,
                    onVisit = { visit ->
                        val fiche = visit.localFiche
                        when {
                            fiche != null -> onEdit(fiche.id)
                            visit.visited -> viewModel.showMessage("${visit.option.nomSite} a déjà une fiche de repérage envoyée au serveur.")
                            else -> onVisitSite(visit.option.id)
                        }
                    },
                    otherFiches = otherFiches,
                    otherRow = { site ->
                        SiteRow(
                            site = site,
                            isSyncing = syncingIds.contains(site.id),
                            onRetry = { viewModel.retrySync(site) },
                            onEdit = { onEdit(site.id) },
                            onDelete = { deleteTarget = site },
                        )
                    },
                )
                return@Column
            }

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

// Liste du repérage du chef d'équipe = les sites importés pour sa zone (tableau de bord :
// Zones > Sites). Chaque site est « À visiter » (orange) puis passe en vert « Visité » dès que sa
// fiche est remplie. Toucher un site à visiter ouvre une fiche déjà remplie ; un site visité
// depuis cet appareil rouvre sa fiche.
private val VisitedGreen = Color(0xFF178A17)
private val IncompleteAmber = Color(0xFFB58105)
private val ToVisitOrange = Color(0xFFD9822B)

@Composable
private fun ZoneSitesList(
    zone: AgentZone?,
    visits: List<ZoneSiteVisit>,
    pending: Int,
    onVisit: (ZoneSiteVisit) -> Unit,
    otherFiches: List<CapturedSite>,
    otherRow: @Composable (CapturedSite) -> Unit,
) {
    val visitedCount = visits.count { it.visited }
    val completeCount = visits.count { it.visited && it.complete }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                if (zone != null) "Zone ${zone.code} — sites à visiter" else "Aucune zone affectée",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnipBlue,
            )
            if (zone != null && zone.place.isNotBlank()) {
                Text(zone.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (visits.isNotEmpty()) {
                Text(
                    "$visitedCount / ${visits.size} site${if (visits.size != 1) "s" else ""} visité${if (visitedCount != 1) "s" else ""}" +
                        if (visitedCount > 0) " — $completeCount complet${if (completeCount != 1) "s" else ""}, ${visitedCount - completeCount} incomplet${if (visitedCount - completeCount != 1) "s" else ""}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                LinearProgressIndicator(
                    progress = { visitedCount.toFloat() / visits.size },
                    color = VisitedGreen,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            if (pending > 0) {
                Text(
                    "$pending fiche${if (pending != 1) "s" else ""} en attente de synchronisation",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (visits.isEmpty() && otherFiches.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (zone == null) {
                        "Vous n'êtes affecté à aucune zone : demandez à votre superviseur ou à l'administrateur de vous en attribuer une."
                    } else {
                        "Aucun site à visiter dans votre zone pour l'instant. L'administrateur les importe depuis le tableau de bord (Zones > Sites)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Sites à visiter d'abord, puis ceux déjà visités.
            items(visits.sortedBy { it.visited }, key = { it.option.id }) { visit ->
                ZoneSiteRow(visit = visit, onClick = { onVisit(visit) })
            }
            if (otherFiches.isNotEmpty()) {
                item(key = "autres-fiches") {
                    Text(
                        "Autres fiches (hors liste des sites de la zone)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                items(otherFiches, key = { it.id }) { site -> otherRow(site) }
            }
        }
    }
}

@Composable
private fun ZoneSiteRow(visit: ZoneSiteVisit, onClick: () -> Unit) {
    val accent = when {
        !visit.visited -> ToVisitOrange
        visit.complete -> VisitedGreen
        else -> IncompleteAmber
    }
    val fiche = visit.localFiche
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(Modifier.weight(1f).padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        visit.option.nomSite,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnipBlue,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        if (visit.visited && visit.complete) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            when {
                                !visit.visited -> "À visiter"
                                visit.complete -> " Visité complet"
                                else -> "Visité incomplet"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                    }
                }
                val detail = listOfNotNull(visit.option.adresse, visit.option.commune).filter { it.isNotBlank() }
                if (detail.isNotEmpty()) {
                    Text(
                        detail.joinToString(" — "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // Visité incomplet : rappelle ce qu'il reste à renseigner.
                val missing = fiche?.missingFields.orEmpty()
                if (visit.visited && !visit.complete && missing.isNotEmpty()) {
                    Text(
                        "À compléter : " + missing.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = IncompleteAmber,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    when {
                        fiche != null && fiche.syncedAt == null -> "Fiche remplie — en attente de synchronisation"
                        fiche != null -> "Fiche remplie et synchronisée — toucher pour l'ouvrir"
                        visit.visited -> "Fiche déjà envoyée au serveur"
                        else -> "Toucher pour remplir la fiche de repérage"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fiche != null && fiche.syncedAt == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
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
