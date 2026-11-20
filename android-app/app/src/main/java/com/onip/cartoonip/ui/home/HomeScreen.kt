package com.onip.cartoonip.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.SecureSync
import com.onip.cartoonip.ui.common.PlaceFilter
import com.onip.cartoonip.ui.common.PlaceFilterBar
import com.onip.cartoonip.ui.common.PlaceValues
import com.onip.cartoonip.ui.common.SyncPasswordDialog
import com.onip.cartoonip.ui.navigation.Routes
import com.onip.cartoonip.ui.theme.OnipBlue
import kotlinx.coroutines.launch

private val SyncedGreen = Color(0xFF178A17)
private val PendingOrange = Color(0xFFD9822B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val households by AppContainer.captureStore.households.collectAsState()
    val sites by AppContainer.siteStore.sites.collectAsState()
    val purged by AppContainer.sessionManager.purged.collectAsState()
    val role by AppContainer.agentRole.collectAsState()
    // Le Repérage de site est réservé au chef d'équipe (et aux comptes admin-tier de test) —
    // un AGENT n'a accès qu'à l'Adressage, cf. SiteService#sync côté backend qui applique la
    // même règle si jamais cet écran était contourné.
    val canAccessReperage = role != null && role != "AGENT"

    // Données envoyées depuis plus de 24 h : effacées de l'appareil à l'ouverture de l'accueil.
    LaunchedEffect(Unit) { SecureSync.purgeSynced() }

    // Filtre Province / Ville / Commune / Quartier : compteurs et envoi limités à ce lieu.
    var placeFilter by remember { mutableStateOf(PlaceFilter()) }
    val shownHouseholds = households.filter { placeFilter.matches(PlaceValues(it.province, it.ville, it.commune, it.quartier)) }
    val shownSites = if (!canAccessReperage) emptyList()
        else sites.filter { placeFilter.matches(PlaceValues(it.province, it.ville, it.commune, it.quartierGroupement)) }

    // Synchronisé = envoyé au serveur (y compris les données déjà effacées de l'appareil, sauf
    // quand un filtre de lieu est choisi : leur lieu n'est plus connu) ; en attente = encore sur
    // l'appareil, à envoyer.
    val withPurged = !placeFilter.isActive
    val householdsPending = shownHouseholds.count { !it.isFullySynced }
    val householdsSynced = shownHouseholds.count { it.isFullySynced } + (if (withPurged) purged.households else 0)
    val sitesPending = shownSites.count { it.syncedAt == null }
    val sitesSynced = shownSites.count { it.syncedAt != null } + (if (withPurged && canAccessReperage) purged.sites else 0)
    val totalPending = householdsPending + sitesPending

    var showSend by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accueil") },
                actions = {
                    TextButton(onClick = { onNavigate(Routes.OVERVIEW) }) { Text("Aperçu") }
                    TextButton(onClick = { onNavigate(Routes.JOURNAL) }) { Text("Journal") }
                    TextButton(onClick = { onNavigate(Routes.PROFILE) }) { Text("Profil") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val placeEntries = households.map { PlaceValues(it.province, it.ville, it.commune, it.quartier) } +
                (if (canAccessReperage) sites.map { PlaceValues(it.province, it.ville, it.commune, it.quartierGroupement) } else emptyList())
            if (placeEntries.isNotEmpty()) {
                PlaceFilterBar(entries = placeEntries, filter = placeFilter, onChange = { placeFilter = it })
            }
            SyncDashboard(
                filterActive = placeFilter.isActive,
                householdsSynced = householdsSynced,
                householdsPending = householdsPending,
                showSites = canAccessReperage,
                sitesSynced = sitesSynced,
                sitesPending = sitesPending,
                onSend = { showSend = true },
            )
            HomeActionCard(
                title = "Adressage",
                subtitle = "Enregistrer un nouveau foyer",
                count = households.size,
                countLabel = if (households.size > 1) "foyers sur l'appareil" else "foyer sur l'appareil",
                icon = Icons.Filled.Add,
                onClick = { onNavigate(Routes.CAPTURE) },
            )
            if (canAccessReperage) {
                HomeActionCard(
                    title = "Repérage",
                    subtitle = "Évaluer un site candidat (bâtiment, terrain)",
                    count = sites.size,
                    countLabel = if (sites.size > 1) "fiches sur l'appareil" else "fiche sur l'appareil",
                    icon = Icons.Filled.LocationOn,
                    onClick = { onNavigate(Routes.REPERAGE) },
                )
            }
        }
    }

    if (showSend) {
        SyncPasswordDialog(
            count = totalPending,
            onDismiss = { showSend = false },
            onFinished = { message ->
                showSend = false
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            send = { password ->
                SecureSync.send(
                    password,
                    households = shownHouseholds.filterNot { it.isFullySynced },
                    sites = shownSites.filter { it.syncedAt == null },
                )
            },
        )
    }
}

/**
 * Tableau de bord de synchronisation : combien de données ont été envoyées au serveur et combien
 * restent à envoyer, par type, avec le bouton d'envoi (mot de passe demandé).
 */
@Composable
private fun SyncDashboard(
    filterActive: Boolean,
    householdsSynced: Int,
    householdsPending: Int,
    showSites: Boolean,
    sitesSynced: Int,
    sitesPending: Int,
    onSend: () -> Unit,
) {
    val synced = householdsSynced + sitesSynced
    val pending = householdsPending + sitesPending
    val total = synced + pending
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = OnipBlue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (filterActive) "Synchronisation — lieu filtré" else "Synchronisation",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnipBlue,
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Synchronisées", synced, SyncedGreen, Modifier.weight(1f))
                StatTile("Reste à envoyer", pending, PendingOrange, Modifier.weight(1f))
            }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { synced.toFloat() / total },
                    color = SyncedGreen,
                    trackColor = PendingOrange.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow("Foyers (adressage)", householdsSynced, householdsPending)
            if (showSites) DetailRow("Fiches de repérage", sitesSynced, sitesPending)

            if (pending > 0) {
                Button(onClick = onSend, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Envoyer au serveur ($pending)")
                }
            } else {
                Text(
                    "Tout est envoyé.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SyncedGreen,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Text(
                "L'envoi demande votre mot de passe. Les données envoyées sont effacées de l'appareil 24 h après l'envoi ; celles en attente restent jusqu'à leur envoi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun DetailRow(label: String, synced: Int, pending: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("$synced envoyé(s)", style = MaterialTheme.typography.labelMedium, color = SyncedGreen, fontWeight = FontWeight.SemiBold)
        Text("  ·  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(
            "$pending en attente",
            style = MaterialTheme.typography.labelMedium,
            color = if (pending > 0) PendingOrange else MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    count: Int,
    countLabel: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = OnipBlue, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnipBlue)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                if (count > 0) {
                    Text(
                        "$count $countLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
