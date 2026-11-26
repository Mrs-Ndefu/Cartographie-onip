package com.onip.cartoonip.ui.reperage

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.DRC_PROVINCES
import com.onip.cartoonip.data.DRC_VILLES
import com.onip.cartoonip.data.SiteQuestionnaire
import com.onip.cartoonip.data.communesForVille
import com.onip.cartoonip.data.model.ZoneSiteOption
import com.onip.cartoonip.data.villesForProvince
import com.onip.cartoonip.ui.common.QuartierField
import kotlin.math.roundToInt

private val upperKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
private val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReperageFormScreen(
    onBack: () -> Unit,
    editId: String? = null,
    zoneSiteId: String? = null,
    viewModel: ReperageFormViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
    }
    // Ouverte depuis « Sites à visiter » : le site est déjà choisi.
    LaunchedEffect(zoneSiteId) {
        if (zoneSiteId != null) viewModel.preselectZoneSite(zoneSiteId)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) viewModel.captureLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Modifier la fiche" else "Nouvelle fiche de site") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("Q7. Coordonnées GPS (latitude, longitude, altitude, précision)") {
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    enabled = !uiState.locating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.locating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.latitude != null) "Relever à nouveau la position" else "Relever la position GPS")
                    }
                }
                uiState.locationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                if (uiState.latitude != null && uiState.longitude != null) {
                    Text(
                        "%.5f, %.5f".format(uiState.latitude, uiState.longitude) +
                            (uiState.altitude?.let { " · altitude %.0f m".format(it) } ?: "") +
                            (uiState.gpsPrecision?.let { " · précision %d m".format(it.roundToInt()) } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            SectionCard("Q7. Identité du site / bureau") {
                if (viewModel.restrictToZoneSites) {
                    val zoneSites by viewModel.zoneSites.collectAsState()
                    ZoneSiteIdentification(
                        state = uiState,
                        options = zoneSites,
                        zoneCommunes = viewModel.zoneCommunes,
                        onSelect = viewModel::onZoneSiteSelected,
                        onCommuneChange = viewModel::onCommuneChange,
                    )
                    QuartierField(uiState.ville, uiState.commune, uiState.quartierGroupement, viewModel::onQuartierGroupementChange)
                    FormField("f. Avenue / Rue / Village", uiState.villageAvenueRue, viewModel::onVillageAvenueRueChange)
                    FormField("j. Numéro de téléphone (personne de contact)", uiState.numeroTelephone, viewModel::onNumeroTelephoneChange, keyboard = numberKeyboard, capitalize = false)
                    FormField("Série GPS / Smartphone", uiState.serieTelephoneGps, viewModel::onSerieTelephoneGpsChange, capitalize = false)
                    return@SectionCard
                }
                SiteGeoFields(
                    province = uiState.province,
                    ville = uiState.ville,
                    commune = uiState.commune,
                    onProvinceChange = viewModel::onProvinceChange,
                    onVilleChange = viewModel::onVilleChange,
                    onCommuneChange = viewModel::onCommuneChange,
                )
                QuartierField(uiState.ville, uiState.commune, uiState.quartierGroupement, viewModel::onQuartierGroupementChange)
                FormField("f. Avenue / Rue / Village", uiState.villageAvenueRue, viewModel::onVillageAvenueRueChange)
                FormField("a. Nom officiel du bureau / site", uiState.nomSite, viewModel::onNomSiteChange)
                FormField("j. Numéro de téléphone (personne de contact)", uiState.numeroTelephone, viewModel::onNumeroTelephoneChange, keyboard = numberKeyboard, capitalize = false)
                FormField("Série GPS / Smartphone", uiState.serieTelephoneGps, viewModel::onSerieTelephoneGpsChange, capitalize = false)
            }

            // Questionnaire terrain (Module 0 à 5 + rubriques de l'ancienne fiche) : les sections
            // qui ne s'appliquent pas au statut du bureau / site (Q1) sont sautées.
            viewModel.sections
                .filterNot { SiteQuestionnaire.hiddenBy(it.hideIf, uiState.answers) }
                .forEach { section ->
                    SectionCard(section.title) {
                        QuestionnaireItems(section.items, uiState.answers, viewModel::onAnswer)
                    }
                }

            Button(
                onClick = { showSaveConfirm = true },
                enabled = !uiState.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Enregistrer")
                }
            }
            uiState.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (showSaveConfirm) {
            AlertDialog(
                onDismissRequest = { showSaveConfirm = false },
                title = { Text("Voulez-vous enregistrer ces informations ?") },
                confirmButton = {
                    TextButton(onClick = {
                        showSaveConfirm = false
                        viewModel.save {
                            Toast.makeText(context, "Fiche de site enregistrée avec succès", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }) { Text("Oui") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveConfirm = false }) { Text("Annuler") }
                },
            )
        }
    }
}

// Fiche du chef d'équipe : le nom du site se choisit dans la liste des sites de sa zone, ce qui
// remplit province, ville/territoire (non modifiables : ceux de la zone), commune (modifiable
// parmi les communes de la zone) et adresse.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneSiteIdentification(
    state: SiteFormState,
    options: List<ZoneSiteOption>,
    zoneCommunes: List<String>,
    onSelect: (ZoneSiteOption) -> Unit,
    onCommuneChange: (String) -> Unit,
) {
    var siteExpanded by remember { mutableStateOf(false) }
    if (options.isEmpty()) {
        Text(
            "Aucun site n'est encore affecté à votre zone. Demandez à l'administrateur de les importer " +
                "(tableau de bord : Zones > Sites), puis rouvrez la fiche avec une connexion.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    } else {
        ExposedDropdownMenuBox(
            expanded = siteExpanded, onExpandedChange = { siteExpanded = it },
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            OutlinedTextField(
                value = state.nomSite,
                onValueChange = {},
                readOnly = true,
                label = { Text("a. Nom officiel du bureau / site") },
                placeholder = { Text("Choisir un site de votre zone") },
                supportingText = { Text("${options.size} site(s) dans votre zone") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = siteExpanded, onDismissRequest = { siteExpanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.nomSite, fontWeight = FontWeight.SemiBold)
                                val detail = listOfNotNull(option.adresse, option.commune).joinToString(" — ")
                                if (detail.isNotBlank()) {
                                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        onClick = {
                            onSelect(option)
                            siteExpanded = false
                        },
                    )
                }
            }
        }
    }

    OutlinedTextField(
        value = state.province, onValueChange = {}, readOnly = true,
        label = { Text("b. Province") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
    OutlinedTextField(
        value = state.ville, onValueChange = {}, readOnly = true,
        label = { Text("c. Ville / Territoire") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
    if (zoneCommunes.isEmpty()) {
        OutlinedTextField(
            value = state.commune, onValueChange = {}, readOnly = true,
            label = { Text("d. Commune / Secteur / Chefferie") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        return
    }
    var communeExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = communeExpanded, onExpandedChange = { communeExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = state.commune,
            onValueChange = {},
            readOnly = true,
            label = { Text("d. Commune / Secteur / Chefferie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = communeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = communeExpanded, onDismissRequest = { communeExpanded = false }) {
            zoneCommunes.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onCommuneChange(name)
                    communeExpanded = false
                })
            }
        }
    }
}

// Province -> ville -> commune en listes déroulantes en cascade, sans restriction de zone (une
// tournée de repérage de site n'est pas bornée à la zone assignée de l'agent, contrairement à la
// capture de ménage — cf. CaptureScreen.VilleCommuneFields pour la version avec restriction).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteGeoFields(
    province: String,
    ville: String,
    commune: String,
    onProvinceChange: (String) -> Unit,
    onVilleChange: (String) -> Unit,
    onCommuneChange: (String) -> Unit,
) {
    val villeNames = remember(province) { villesForProvince(province).map { it.name } }
    var villeOther by remember(province) {
        mutableStateOf(ville.isNotBlank() && DRC_VILLES.none { it.name.equals(ville, ignoreCase = true) })
    }
    var provinceExpanded by remember { mutableStateOf(false) }
    var villeExpanded by remember { mutableStateOf(false) }
    var communeExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = provinceExpanded, onExpandedChange = { provinceExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = DRC_PROVINCES.firstOrNull { it.equals(province, ignoreCase = true) } ?: province,
            onValueChange = {},
            readOnly = true,
            label = { Text("b. Province") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = provinceExpanded, onDismissRequest = { provinceExpanded = false }) {
            DRC_PROVINCES.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    villeOther = false
                    onProvinceChange(name)
                    provinceExpanded = false
                })
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = villeExpanded, onExpandedChange = { villeExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = if (villeOther) "Autre (saisie libre)" else ville,
            onValueChange = {},
            readOnly = true,
            label = { Text("c. Ville / Territoire") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = villeExpanded, onDismissRequest = { villeExpanded = false }) {
            villeNames.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    villeOther = false
                    onVilleChange(name)
                    villeExpanded = false
                })
            }
            DropdownMenuItem(text = { Text("Autre (saisie libre)") }, onClick = {
                villeOther = true
                onVilleChange("")
                villeExpanded = false
            })
        }
    }
    if (villeOther) {
        OutlinedTextField(
            value = ville, onValueChange = { onVilleChange(it.uppercase()) },
            label = { Text("c. Ville / Territoire") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }

    val communeOptions = if (villeOther) emptyList() else communesForVille(ville)
    var communeOther by remember(ville) {
        mutableStateOf(commune.isNotBlank() && communeOptions.none { it.equals(commune, ignoreCase = true) })
    }

    if (communeOptions.isEmpty()) {
        OutlinedTextField(
            value = commune, onValueChange = { onCommuneChange(it.uppercase()) },
            label = { Text("d. Commune / Secteur / Chefferie") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = communeExpanded, onExpandedChange = { communeExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = if (communeOther) "Autre (saisie libre)" else commune,
            onValueChange = {},
            readOnly = true,
            label = { Text("d. Commune / Secteur / Chefferie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = communeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = communeExpanded, onDismissRequest = { communeExpanded = false }) {
            communeOptions.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    communeOther = false
                    onCommuneChange(name)
                    communeExpanded = false
                })
            }
            DropdownMenuItem(text = { Text("Autre (saisie libre)") }, onClick = {
                communeOther = true
                onCommuneChange("")
                communeExpanded = false
            })
        }
    }
    if (communeOther) {
        OutlinedTextField(
            value = commune, onValueChange = { onCommuneChange(it.uppercase()) },
            label = { Text("d. Commune / Secteur / Chefferie") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
internal fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardOptions = upperKeyboard,
    capitalize: Boolean = true,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = if (capitalize) upperKeyboard else keyboard,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

private val radioScale = 0.75f

@Composable
internal fun ChoiceGroup(options: List<Pair<String, String>>, selected: String?, onSelect: (String?) -> Unit) {
    Column {
        options.forEach { (value, text) ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .selectable(selected = selected == value, onClick = { onSelect(if (selected == value) null else value) })
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == value, onClick = null, modifier = Modifier.scale(radioScale))
                Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 2.dp))
            }
        }
    }
}

@Composable
internal fun YesNoRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Row(
                modifier = Modifier.selectable(selected = value == true, onClick = { onChange(if (value == true) null else true) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == true, onClick = null, modifier = Modifier.scale(radioScale))
                Text("Oui", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(16.dp))
            Row(
                modifier = Modifier.selectable(selected = value == false, onClick = { onChange(if (value == false) null else false) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == false, onClick = null, modifier = Modifier.scale(radioScale))
                Text("Non", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
