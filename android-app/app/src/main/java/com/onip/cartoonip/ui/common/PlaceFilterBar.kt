package com.onip.cartoonip.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Lieu d'un élément de liste (foyer, fiche, site), pour les filtres. */
data class PlaceValues(
    val province: String = "",
    val ville: String = "",
    val commune: String = "",
    val quartier: String = "",
)

/** Filtre de lieu choisi ("" = pas de filtre à ce niveau). */
data class PlaceFilter(
    val province: String = "",
    val ville: String = "",
    val commune: String = "",
    val quartier: String = "",
) {
    val isActive: Boolean get() = province.isNotEmpty() || ville.isNotEmpty() || commune.isNotEmpty() || quartier.isNotEmpty()

    fun matches(p: PlaceValues): Boolean =
        same(province, p.province) && same(ville, p.ville) && same(commune, p.commune) && same(quartier, p.quartier)

    private fun same(filter: String, value: String) = filter.isEmpty() || filter.equals(value.trim(), ignoreCase = true)
}

enum class PlaceLevel(val label: String, val all: String) {
    PROVINCE("Province", "Toutes"),
    VILLE("Ville", "Toutes"),
    COMMUNE("Commune", "Toutes"),
    QUARTIER("Quartier", "Tous"),
}

/**
 * Barre de filtres de lieu enchaînés (Province -> Ville -> Commune -> Quartier) : chaque liste ne
 * propose que les valeurs présentes dans [entries] compatibles avec les niveaux au-dessus, et
 * changer un niveau vide ceux du dessous. [levels] choisit les niveaux affichés (ex. Commune +
 * Quartier pour la liste du chef d'équipe, dont la zone fixe déjà province et ville).
 */
@Composable
fun PlaceFilterBar(
    entries: List<PlaceValues>,
    filter: PlaceFilter,
    onChange: (PlaceFilter) -> Unit,
    modifier: Modifier = Modifier,
    levels: List<PlaceLevel> = PlaceLevel.entries,
) {
    fun values(level: PlaceLevel): List<String> {
        val scoped = entries.filter { e ->
            (level.ordinal <= PlaceLevel.PROVINCE.ordinal || filter.province.isEmpty() || filter.province.equals(e.province.trim(), true)) &&
                (level.ordinal <= PlaceLevel.VILLE.ordinal || filter.ville.isEmpty() || filter.ville.equals(e.ville.trim(), true)) &&
                (level.ordinal <= PlaceLevel.COMMUNE.ordinal || filter.commune.isEmpty() || filter.commune.equals(e.commune.trim(), true))
        }
        return scoped.map {
            when (level) {
                PlaceLevel.PROVINCE -> it.province
                PlaceLevel.VILLE -> it.ville
                PlaceLevel.COMMUNE -> it.commune
                PlaceLevel.QUARTIER -> it.quartier
            }.trim().uppercase()
        }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        levels.forEach { level ->
            val current = when (level) {
                PlaceLevel.PROVINCE -> filter.province
                PlaceLevel.VILLE -> filter.ville
                PlaceLevel.COMMUNE -> filter.commune
                PlaceLevel.QUARTIER -> filter.quartier
            }
            FilterDropdown(
                label = level.label,
                allLabel = level.all,
                current = current,
                options = values(level),
                onSelect = { value ->
                    onChange(
                        when (level) {
                            PlaceLevel.PROVINCE -> PlaceFilter(province = value)
                            PlaceLevel.VILLE -> filter.copy(ville = value, commune = "", quartier = "")
                            PlaceLevel.COMMUNE -> filter.copy(commune = value, quartier = "")
                            PlaceLevel.QUARTIER -> filter.copy(quartier = value)
                        },
                    )
                },
            )
        }
        if (filter.isActive) {
            TextButton(onClick = { onChange(PlaceFilter()) }) { Text("Effacer") }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    allLabel: String,
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        // Boutons assez grands pour être touchés facilement sur le terrain.
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                "$label : " + current.ifEmpty { allLabel },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (current.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(""); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("Aucune valeur", color = MaterialTheme.colorScheme.outline) }, onClick = { expanded = false })
            }
        }
    }
}
