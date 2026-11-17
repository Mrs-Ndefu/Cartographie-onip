package com.onip.cartoonip.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onip.cartoonip.data.quartiersFor

private const val OTHER_LABEL = "Autre (saisie libre)"

/**
 * Champ « Quartier/Groupement » : liste des quartiers de la commune choisie (référentiel
 * « Subdivision ADM-RDC », cf. DrcLocations), avec « Autre (saisie libre) » en secours. Sans
 * quartier connu pour la commune (ou sans commune), simple saisie libre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuartierField(
    ville: String,
    commune: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Quartiers de la zone de l'agent : s'ils sont renseignés, seuls ceux-là sont proposés (ceux
    // de la commune choisie s'il y en a, sinon tous), sans saisie libre.
    allowed: List<String> = emptyList(),
) {
    val restricted = allowed.isNotEmpty()
    val options = remember(ville, commune, allowed) {
        val ofCommune = quartiersFor(ville, commune)
        if (!restricted) ofCommune
        else allowed.filter { a -> ofCommune.any { it.equals(a, ignoreCase = true) } }.ifEmpty { allowed }
    }
    var other by remember(ville, commune) {
        mutableStateOf(value.isNotBlank() && options.none { it.equals(value, ignoreCase = true) })
    }
    var expanded by remember { mutableStateOf(false) }

    if (options.isEmpty()) {
        OutlinedTextField(
            value = value, onValueChange = { onValueChange(it.uppercase()) },
            label = { Text("Quartier/Groupement") }, singleLine = true,
            modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it },
        modifier = modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = if (other) OTHER_LABEL else options.firstOrNull { it.equals(value, ignoreCase = true) } ?: value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Quartier/Groupement") },
            supportingText = {
                Text(if (restricted) "${options.size} quartier(s) de votre zone" else "${options.size} quartier(s) dans cette commune")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    other = false
                    onValueChange(name.uppercase())
                    expanded = false
                })
            }
            if (!restricted) {
                DropdownMenuItem(text = { Text(OTHER_LABEL) }, onClick = {
                    other = true
                    onValueChange("")
                    expanded = false
                })
            }
        }
    }
    if (other) {
        OutlinedTextField(
            value = value, onValueChange = { onValueChange(it.uppercase()) },
            label = { Text("Quartier/Groupement") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}
