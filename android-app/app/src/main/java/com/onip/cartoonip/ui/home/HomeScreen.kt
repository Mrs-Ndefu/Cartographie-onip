package com.onip.cartoonip.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.ui.navigation.Routes
import com.onip.cartoonip.ui.theme.OnipBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val households by AppContainer.captureStore.households.collectAsState()
    val sites by AppContainer.siteStore.sites.collectAsState()
    val pendingSites = sites.count { it.syncedAt == null }
    val role by AppContainer.agentRole.collectAsState()
    // Le Repérage de site est réservé au chef d'équipe (et aux comptes admin-tier de test) —
    // un AGENT n'a accès qu'à l'Adressage, cf. SiteService#sync côté backend qui applique la
    // même règle si jamais cet écran était contourné.
    val canAccessReperage = role != null && role != "AGENT"

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
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HomeActionCard(
                title = "Adressage",
                subtitle = "Enregistrer un nouveau foyer",
                count = households.size,
                countLabel = if (households.size > 1) "foyers enregistrés" else "foyer enregistré",
                icon = Icons.Filled.Add,
                onClick = { onNavigate(Routes.CAPTURE) },
            )
            if (canAccessReperage) {
                HomeActionCard(
                    title = "Repérage",
                    subtitle = "Évaluer un site candidat (bâtiment, terrain)",
                    count = sites.size,
                    countLabel = if (sites.size > 1) "fiches de site" else "fiche de site",
                    icon = Icons.Filled.LocationOn,
                    onClick = { onNavigate(Routes.REPERAGE) },
                )
                if (pendingSites > 0) {
                    Text(
                        "$pendingSites fiche(s) de site en attente de synchronisation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
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
