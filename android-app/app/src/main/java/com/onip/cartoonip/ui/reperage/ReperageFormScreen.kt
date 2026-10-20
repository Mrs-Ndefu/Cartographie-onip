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
import com.onip.cartoonip.data.communesForVille
import com.onip.cartoonip.data.villesForProvince
import kotlin.math.roundToInt

private val upperKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
private val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReperageFormScreen(
    onBack: () -> Unit,
    editId: String? = null,
    viewModel: ReperageFormViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) viewModel.loadForEdit(editId)
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
            SectionCard("GPS") {
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

            SectionCard("I. Identification administrative du site") {
                SiteGeoFields(
                    province = uiState.province,
                    ville = uiState.ville,
                    commune = uiState.commune,
                    onProvinceChange = viewModel::onProvinceChange,
                    onVilleChange = viewModel::onVilleChange,
                    onCommuneChange = viewModel::onCommuneChange,
                )
                FormField("Quartier/Groupement", uiState.quartierGroupement, viewModel::onQuartierGroupementChange)
                FormField("Village/Avenue/Rue", uiState.villageAvenueRue, viewModel::onVillageAvenueRueChange)
                FormField("Nom du site", uiState.nomSite, viewModel::onNomSiteChange)
                FormField("Numéro téléphone", uiState.numeroTelephone, viewModel::onNumeroTelephoneChange, keyboard = numberKeyboard, capitalize = false)
                FormField("Série téléphone/GPS", uiState.serieTelephoneGps, viewModel::onSerieTelephoneGpsChange, capitalize = false)
            }

            SectionCard("II. Type de site") {
                ChoiceGroup(
                    options = listOf("ECOLE" to "École", "EGLISE" to "Église", "ADMINISTRATIF" to "Administratif", "HOPITAL" to "Hôpital", "AUTRE" to "Autre"),
                    selected = uiState.typeSite,
                    onSelect = viewModel::onTypeSiteChange,
                )
                if (uiState.typeSite == "AUTRE") {
                    FormField("Préciser", uiState.typeSiteAutre, viewModel::onTypeSiteAutreChange)
                }
            }

            SectionCard("III. Statut juridique et propriété") {
                Text("Propriété du site", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                ChoiceGroup(
                    options = listOf("ETAT_DOMAINE_PUBLIC" to "État / Domaine public", "PRIVE" to "Privé"),
                    selected = uiState.proprieteStatut,
                    onSelect = viewModel::onProprieteStatutChange,
                )
                Text("Options d'occupation", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                ChoiceGroup(
                    options = listOf("ACHAT" to "Achat", "PARTENARIAT" to "Partenariat / Mise à disposition", "LOCATION" to "Location"),
                    selected = uiState.optionOccupation,
                    onSelect = viewModel::onOptionOccupationChange,
                )
                YesNoRow("Disponibilité immédiate", uiState.disponibiliteImmediate, viewModel::onDisponibiliteImmediateChange)
                if (uiState.disponibiliteImmediate == false) {
                    FormField("Délai estimé", uiState.delaiEstime, viewModel::onDelaiEstimeChange, capitalize = false)
                }
                FormField("Nom et contact du propriétaire/gestionnaire", uiState.proprietaireNomContact, viewModel::onProprietaireNomContactChange)
            }

            SectionCard("IV. Caractéristiques physiques et infrastructures") {
                Text("Type d'infrastructure", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                ChoiceGroup(
                    options = listOf("BATIMENT_EXISTANT" to "Bâtiment existant", "TERRAIN_CONSTRUCTION" to "Terrain pour construction", "AUTRE" to "Autre"),
                    selected = uiState.typeInfrastructure,
                    onSelect = viewModel::onTypeInfrastructureChange,
                )
                if (uiState.typeInfrastructure == "AUTRE") {
                    FormField("Préciser", uiState.typeInfrastructureAutre, viewModel::onTypeInfrastructureAutreChange)
                }
                FormField("Superficie bâtiment (m²)", uiState.superficieBatiment, viewModel::onSuperficieBatimentChange, keyboard = numberKeyboard, capitalize = false)
                FormField("Superficie terrain (m²)", uiState.superficieTerrain, viewModel::onSuperficieTerrainChange, keyboard = numberKeyboard, capitalize = false)
                FormField("Nombre de pièces/bureaux disponibles", uiState.nombrePieces, viewModel::onNombrePiecesChange, keyboard = numberKeyboard, capitalize = false)
                Text("État général du bâtiment", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                ChoiceGroup(
                    options = listOf(
                        "BON" to "Bon (aucun travaux majeurs)",
                        "MOYEN" to "Moyen (travaux de réhabilitation légers requis)",
                        "VETUSTE" to "Vétuste / à réhabiliter entièrement",
                    ),
                    selected = uiState.etatBatiment,
                    onSelect = viewModel::onEtatBatimentChange,
                )
                FormField("Autres observations sur l'état du bâtiment", uiState.etatBatimentObservations, viewModel::onEtatBatimentObservationsChange, singleLine = false)
            }

            SectionCard("V. Commodité et accès techniques") {
                Text("Accès routier et transport", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                ChoiceGroup(
                    options = listOf(
                        "VEHICULE_TOUTE_ANNEE" to "Accessible en véhicule toute l'année",
                        "DIFFICILE_SAISON_PLUIES" to "Accessible difficilement en saison des pluies",
                        "PIED_MOTO_UNIQUEMENT" to "Accessible uniquement à pied ou moto",
                    ),
                    selected = uiState.accesRoutier,
                    onSelect = viewModel::onAccesRoutierChange,
                )
                Text("Alimentation en énergie / eau", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                YesNoRow("Raccordé au réseau public (SNEL)", uiState.raccordeSnel, viewModel::onRaccordeSnelChange)
                if (uiState.raccordeSnel == true) {
                    Text("Stabilité", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                    ChoiceGroup(
                        options = listOf("BONNE" to "Bonne", "FAIBLE" to "Faible", "INEXISTANT" to "Inexistant"),
                        selected = uiState.stabiliteSnel,
                        onSelect = viewModel::onStabiliteSnelChange,
                    )
                }
                YesNoRow("Présence d'un groupe électrogène ou solaire alternatif", uiState.groupeElectrogeneSolaire, viewModel::onGroupeElectrogeneSolaireChange)
                YesNoRow("Non électrifié", uiState.nonElectrifie, viewModel::onNonElectrifieChange)
                YesNoRow("Regideso (eau)", uiState.regideso, viewModel::onRegidesoChange)
                YesNoRow("Sanitaires", uiState.sanitaires, viewModel::onSanitairesChange)
                Text("Connectivité télécom / internet", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                ChoiceGroup(
                    options = listOf("EXCELLENTE" to "Excellente", "MOYENNE" to "Moyenne", "INEXISTANTE" to "Inexistante"),
                    selected = uiState.connectiviteInternet,
                    onSelect = viewModel::onConnectiviteInternetChange,
                )
                FormField("Réseaux disponibles", uiState.reseauxDisponibles, viewModel::onReseauxDisponiblesChange, capitalize = false)
                YesNoRow("Sécurité des lieux : clôturé et sécurisé", uiState.clotureSecurise, viewModel::onClotureSecuriseChange)
                YesNoRow("Proximité d'un poste de police/militaire", uiState.proximitePolice, viewModel::onProximitePoliceChange)
                if (uiState.proximitePolice == true) {
                    FormField("Distance (mètres)", uiState.distancePoliceMetres, viewModel::onDistancePoliceMetresChange, keyboard = numberKeyboard, capitalize = false)
                }
            }

            SectionCard("VI. Évaluation et recommandations de l'équipe de terrain") {
                Text("Avis global de l'équipe", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
                ChoiceGroup(
                    options = listOf(
                        "FAVORABLE" to "Favorable (site recommandé sans réserve)",
                        "FAVORABLE_SOUS_RESERVE" to "Favorable sous réserve de travaux d'aménagement",
                        "DEFAVORABLE" to "Défavorable",
                    ),
                    selected = uiState.avisGlobal,
                    onSelect = viewModel::onAvisGlobalChange,
                )
                if (uiState.avisGlobal == "FAVORABLE_SOUS_RESERVE") {
                    FormField("Travaux d'aménagement nécessaires", uiState.travauxAmenagement, viewModel::onTravauxAmenagementChange, singleLine = false)
                }
                if (uiState.avisGlobal == "DEFAVORABLE") {
                    FormField("Motif", uiState.motifDefavorable, viewModel::onMotifDefavorableChange, singleLine = false)
                }
                FormField("Observations particulières", uiState.observationsParticulieres, viewModel::onObservationsParticulieresChange, singleLine = false)
                FormField("Nom et signature de l'enquêteur", uiState.nomSignatureEnqueteur, viewModel::onNomSignatureEnqueteurChange)
                OutlinedTextField(
                    // TextFieldValue avec sélection forcée en fin de texte : le formatage auto (ajout
                    // des "-") réécrit la chaîne à chaque frappe, et sans forcer la sélection, Compose
                    // replace parfois le curseur n'importe où dans le texte reformaté au lieu de le
                    // laisser là où l'utilisateur tape (cf. CaptureScreen, même correctif).
                    value = TextFieldValue(uiState.dateEvaluation, TextRange(uiState.dateEvaluation.length)),
                    onValueChange = { viewModel.onDateEvaluationChange(it.text) },
                    label = { Text("Date de l'évaluation (AAAA-MM-JJ)") },
                    singleLine = true,
                    keyboardOptions = numberKeyboard,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
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
            label = { Text("Province/Région") },
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
            label = { Text("Ville/Territoire") },
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
            label = { Text("Ville/Territoire") }, singleLine = true,
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
            label = { Text("Commune/Secteur/Chefferie") }, singleLine = true,
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
            label = { Text("Commune/Secteur/Chefferie") },
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
            label = { Text("Commune/Secteur/Chefferie") }, singleLine = true,
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
private fun FormField(
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
private fun ChoiceGroup(options: List<Pair<String, String>>, selected: String?, onSelect: (String?) -> Unit) {
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
private fun YesNoRow(label: String, value: Boolean?, onChange: (Boolean?) -> Unit) {
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
