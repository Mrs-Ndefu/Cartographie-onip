package com.onip.cartoonip.ui.capture

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.data.DRC_PROVINCES
import com.onip.cartoonip.data.DRC_VILLES
import com.onip.cartoonip.data.communesForVille
import com.onip.cartoonip.data.villesForProvince
import com.onip.cartoonip.data.model.MAX_HOUSEHOLD_PHOTOS
import com.onip.cartoonip.ui.navigation.Routes
import com.onip.cartoonip.ui.theme.OnipBlue

private val fieldKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
private val dateKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(onNavigate: (String) -> Unit, editId: String? = null, viewModel: CaptureViewModel = viewModel()) {
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

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success -> viewModel.onPhotoCaptured(success) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {},
                    actions = {
                        val agentPhoto by AppContainer.agentPhoto.collectAsState()
                        TextButton(onClick = { onNavigate(Routes.OVERVIEW) }) { Text("Aperçu") }
                        TextButton(onClick = { onNavigate(Routes.JOURNAL) }) { Text("Journal") }
                        IconButton(onClick = { onNavigate(Routes.PROFILE) }) {
                            TopBarAvatar(photoDataUrl = agentPhoto)
                        }
                    },
                )
                Text(
                    if (uiState.isEditing) "Modifier le ménage" else "Nouveau ménage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnipBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 8.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                StepCard(number = 1, title = "Coordonnées GPS*") {
                    if (uiState.hasLocation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                "  Lat ${"%.5f".format(uiState.latitude)} · Lon ${"%.5f".format(uiState.longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    uiState.gpsError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                        },
                        enabled = !uiState.gpsLoading,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        if (uiState.gpsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text(if (uiState.hasLocation) "Relever à nouveau" else "Localiser")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.declaredMembersText,
                            onValueChange = viewModel::onDeclaredMembersChange,
                            label = { Text("Nombre de membres du ménage (chef compris)*") },
                            supportingText = { Text("1 = le chef vit seul ; 5 = le chef + jusqu'à 4 membres à ajouter.") },
                            isError = uiState.declaredMembersText.isNotBlank() && uiState.declaredMembers == null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                StepCard(number = 2, title = "Chef de ménage") {
                    OutlinedTextField(
                        value = uiState.chefNom, onValueChange = viewModel::onChefNomChange,
                        label = { Text("Nom*") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = uiState.chefPostnom, onValueChange = viewModel::onChefPostnomChange,
                        label = { Text("Postnom") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = uiState.chefPrenom, onValueChange = viewModel::onChefPrenomChange,
                        label = { Text("Prénom") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        // TextFieldValue avec sélection forcée en fin de texte : le formatage auto (ajout
                        // des "/") réécrit la chaîne à chaque frappe, et sans forcer la sélection, Compose
                        // replace parfois le curseur n'importe où dans le texte reformaté au lieu de le
                        // laisser là où l'utilisateur tape.
                        value = TextFieldValue(uiState.chefDateNaissance, TextRange(uiState.chefDateNaissance.length)),
                        onValueChange = { viewModel.onChefDateNaissanceChange(it.text) },
                        label = { Text("Date de naissance (JJ/MM/AAAA)") }, singleLine = true, keyboardOptions = dateKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    SexeSelector(value = uiState.chefSexe, onChange = viewModel::onChefSexeChange)
                }
            }

            item {
                StepCard(number = 3, title = "Adresse complète") {
                    VilleCommuneFields(
                        province = uiState.province,
                        ville = uiState.ville,
                        commune = uiState.commune,
                        onProvinceChange = viewModel::onProvinceChange,
                        onVilleChange = viewModel::onVilleChange,
                        onCommuneChange = viewModel::onCommuneChange,
                    )
                    OutlinedTextField(
                        value = uiState.quartier, onValueChange = viewModel::onQuartierChange,
                        label = { Text("Quartier*") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.rue, onValueChange = viewModel::onRueChange,
                            label = { Text("Rue") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                            modifier = Modifier.weight(2f),
                        )
                        OutlinedTextField(
                            value = uiState.numero, onValueChange = viewModel::onNumeroChange,
                            label = { Text("N°") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = uiState.immeuble, onValueChange = viewModel::onImmeubleChange,
                            label = { Text("Immeuble ou appartement") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                            modifier = Modifier.weight(2f),
                        )
                        OutlinedTextField(
                            value = uiState.etage, onValueChange = viewModel::onEtageChange,
                            label = { Text("Étage") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                MembersSection(
                    members = uiState.members,
                    declaredMembers = uiState.declaredMembers,
                    limitMessage = uiState.memberLimitMessage,
                    expanded = uiState.membersExpanded,
                    onToggle = viewModel::toggleMembersExpanded,
                    onAdd = viewModel::addMember,
                    onRemove = viewModel::removeMember,
                    onNomChange = viewModel::onMemberNomChange,
                    onPostnomChange = viewModel::onMemberPostnomChange,
                    onPrenomChange = viewModel::onMemberPrenomChange,
                    onDateNaissanceChange = viewModel::onMemberDateNaissanceChange,
                    onSexeChange = viewModel::onMemberSexeChange,
                    onRelationChange = viewModel::onMemberRelationChange,
                )
            }

            item {
                StepCard(number = 4, title = "Code du ménage") {
                    if (uiState.generatedCode != null) {
                        Text(
                            uiState.generatedCode!!,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        Text(
                            "Recopiez ce code sur la fiche papier avant de continuer. Il ne peut plus être régénéré.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Le code est généré automatiquement à partir du GPS, du chef de ménage et de l'adresse (commune et quartier) dès qu'ils sont renseignés.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                StepCard(number = 5, title = "Photos de la fiche complétée") {
                    uiState.photoError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        "Facultatif, mais recommandé : jusqu'à ${MAX_HOUSEHOLD_PHOTOS} photos servent de référence complète de la fiche papier.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (uiState.photoPaths.isNotEmpty()) {
                        PhotoGallery(paths = uiState.photoPaths, onRemove = viewModel::removePhoto)
                    }
                    // Un seul bouton, réutilisé pour chaque prise successive (pas un bouton par
                    // emplacement de la galerie) — masqué une fois les 4 emplacements remplis.
                    if (uiState.canAddPhoto) {
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(viewModel.photoFileUri()) },
                            modifier = Modifier.fillMaxWidth().padding(top = if (uiState.photoPaths.isNotEmpty()) 8.dp else 0.dp),
                        ) {
                            Text("Prendre une photo")
                        }
                    } else {
                        Text(
                            "Galerie complète (${MAX_HOUSEHOLD_PHOTOS}/${MAX_HOUSEHOLD_PHOTOS}) — retirez une photo pour en reprendre une autre.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            item {
                uiState.saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (!uiState.canSubmit && uiState.missingForSubmit.isNotEmpty()) {
                    Text(
                        "Pour enregistrer, renseignez : ${uiState.missingForSubmit.joinToString(", ")}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = { showSaveConfirm = true },
                    enabled = uiState.canSubmit && !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (uiState.isEditing) "Mettre à jour le ménage" else "Enregistrer le ménage")
                    }
                }
            }
        }
    }

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("Voulez-vous enregistrer ce ménage ?") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveConfirm = false
                    viewModel.submit {
                        Toast.makeText(context, "Ménage enregistré avec succès", Toast.LENGTH_SHORT).show()
                        if (uiState.isEditing) onNavigate(Routes.JOURNAL) else viewModel.resetForm()
                    }
                }) { Text("Oui") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun SexeSelector(value: String?, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        FilterChip(selected = value == "M", onClick = { onChange("M") }, label = { Text("Masculin") })
        FilterChip(selected = value == "F", onClick = { onChange("F") }, label = { Text("Féminin") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VilleCommuneFields(
    province: String,
    ville: String,
    commune: String,
    onProvinceChange: (String) -> Unit,
    onVilleChange: (String) -> Unit,
    onCommuneChange: (String) -> Unit,
) {
    val allVilleNames = remember { DRC_VILLES.map { it.name } }
    val villeNames = remember(province) { villesForProvince(province).map { it.name } }
    var villeOther by remember { mutableStateOf(ville.isNotBlank() && allVilleNames.none { it.equals(ville, ignoreCase = true) }) }
    var provinceExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = provinceExpanded, onExpandedChange = { provinceExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = DRC_PROVINCES.firstOrNull { it.equals(province, ignoreCase = true) } ?: province,
            onValueChange = {},
            readOnly = true,
            label = { Text("Province") },
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

    val communeOptions = if (villeOther) emptyList() else communesForVille(ville)
    var communeOther by remember(ville) {
        mutableStateOf(commune.isNotBlank() && communeOptions.none { it.equals(commune, ignoreCase = true) })
    }
    var villeExpanded by remember { mutableStateOf(false) }
    var communeExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = villeExpanded, onExpandedChange = { villeExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = if (villeOther) "Autre (saisie libre)" else ville,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ville") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = villeExpanded, onDismissRequest = { villeExpanded = false }) {
            villeNames.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    villeOther = false
                    onVilleChange(name)
                    onCommuneChange("")
                    villeExpanded = false
                })
            }
            DropdownMenuItem(text = { Text("Autre (saisie libre)") }, onClick = {
                villeOther = true
                onVilleChange("")
                onCommuneChange("")
                villeExpanded = false
            })
        }
    }
    if (villeOther) {
        OutlinedTextField(
            value = ville, onValueChange = { onVilleChange(it.uppercase()) },
            label = { Text("Ville / territoire") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }

    ExposedDropdownMenuBox(
        expanded = communeExpanded, onExpandedChange = { communeExpanded = it },
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = if (communeOther) "Autre (saisie libre)" else commune,
            onValueChange = {},
            readOnly = true,
            label = { Text("Commune*") },
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
            label = { Text("Commune") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelationDropdown(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.ifBlank { "Lien avec le chef" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Lien avec le chef") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RELATION_OPTIONS.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false })
            }
        }
    }
}

@Composable
private fun MembersSection(
    members: List<MemberInput>,
    declaredMembers: Int?,
    limitMessage: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onNomChange: (String, String) -> Unit,
    onPostnomChange: (String, String) -> Unit,
    onPrenomChange: (String, String) -> Unit,
    onDateNaissanceChange: (String, String) -> Unit,
    onSexeChange: (String, String) -> Unit,
    onRelationChange: (String, String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = if (expanded) 10.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Membres du ménage (facultatif)", style = MaterialTheme.typography.titleMedium)
                    // Fiches ajoutées / membres possibles d'après le nombre saisi (chef non compté).
                    Text(
                        "Membres ajoutés : ${members.size} / ${declaredMembers?.minus(1) ?: "?"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnipBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Réduire" else "Afficher",
                    )
                }
            }

            if (expanded) {
                members.forEach { member ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 14.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = member.nom, onValueChange = { onNomChange(member.key, it) },
                                label = { Text("Nom") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            )
                            OutlinedTextField(
                                value = member.postnom, onValueChange = { onPostnomChange(member.key, it) },
                                label = { Text("Postnom") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            )
                            OutlinedTextField(
                                value = member.prenom, onValueChange = { onPrenomChange(member.key, it) },
                                label = { Text("Prénom") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            )
                            OutlinedTextField(
                                value = TextFieldValue(member.dateNaissance, TextRange(member.dateNaissance.length)),
                                onValueChange = { onDateNaissanceChange(member.key, it.text) },
                                label = { Text("Date de naissance (JJ/MM/AAAA)") }, singleLine = true, keyboardOptions = dateKeyboardOptions,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            )
                            SexeSelector(value = member.sexe, onChange = { onSexeChange(member.key, it) })
                            RelationDropdown(value = member.relation, onChange = { onRelationChange(member.key, it) })
                        }
                        IconButton(onClick = { onRemove(member.key) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Retirer ce membre")
                        }
                    }
                }
                limitMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Ajouter un membre", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun TopBarAvatar(photoDataUrl: String?) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = photoDataUrl) {
        value = photoDataUrl?.let { dataUrl ->
            runCatching {
                val bytes = Base64.decode(dataUrl.substringAfter(","), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Profil",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(32.dp).clip(CircleShape),
        )
    } else {
        Icon(Icons.Filled.Person, contentDescription = "Profil")
    }
}

// Galerie des photos déjà prises (jusqu'à MAX_HOUSEHOLD_PHOTOS), 2 par ligne, chacune avec son
// propre bouton de suppression — la prise elle-même reste sur un unique bouton (cf. StepCard 5).
@Composable
private fun PhotoGallery(paths: List<String>, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        paths.chunked(2).forEach { rowPaths ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowPaths.forEach { path ->
                    PhotoThumbnail(path = path, onRemove = { onRemove(path) }, modifier = Modifier.weight(1f))
                }
                if (rowPaths.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(path: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }
    Box(modifier = modifier) {
        bitmapState.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo de la fiche",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Retirer cette photo", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun StepCard(number: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
            }
            content()
        }
    }
}
