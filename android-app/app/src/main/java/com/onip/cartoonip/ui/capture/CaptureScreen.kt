package com.onip.cartoonip.ui.capture

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.ui.navigation.Routes
import com.onip.cartoonip.ui.theme.OnipBlue

private val fieldKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
private val dateKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(onNavigate: (String) -> Unit, editId: String? = null, viewModel: CaptureViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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
                        value = uiState.chefDateNaissance, onValueChange = viewModel::onChefDateNaissanceChange,
                        label = { Text("Date de naissance (JJ/MM/AAAA)") }, singleLine = true, keyboardOptions = dateKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    SexeSelector(value = uiState.chefSexe, onChange = viewModel::onChefSexeChange)
                }
            }

            item {
                StepCard(number = 3, title = "Adresse complète") {
                    OutlinedTextField(
                        value = uiState.ville, onValueChange = viewModel::onVilleChange,
                        label = { Text("Ville") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = uiState.commune, onValueChange = viewModel::onCommuneChange,
                        label = { Text("Commune*") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                    OutlinedTextField(
                        value = uiState.immeuble, onValueChange = viewModel::onImmeubleChange,
                        label = { Text("Immeuble / repère") }, singleLine = true, keyboardOptions = fieldKeyboardOptions,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            item {
                MembersSection(
                    members = uiState.members,
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
                            "Recopiez ce code sur la fiche papier avant de continuer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Renseignez le chef de ménage et l'adresse pour générer le code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = viewModel::generateCode,
                        enabled = uiState.canGenerateCode,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text(if (uiState.generatedCode != null) "Régénérer le code" else "Générer le code")
                    }
                }
            }

            item {
                StepCard(number = 5, title = "Photo de la fiche complétée") {
                    uiState.photoError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        "Facultatif, mais recommandé : la photo sert de référence complète de la fiche papier.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (uiState.photoUri != null) {
                        LocalPhotoPreview(path = uiState.photoUri.toString())
                    }
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(viewModel.photoFileUri()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.photoUri != null) "Reprendre la photo" else "Prendre la photo")
                    }
                }
            }

            item {
                uiState.saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (!uiState.canSubmit) {
                    Text(
                        "Le GPS est obligatoire pour enregistrer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        viewModel.submit {
                            if (uiState.isEditing) onNavigate(Routes.JOURNAL) else viewModel.resetForm()
                        }
                    },
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        "Membres du ménage (facultatif)" + if (members.isNotEmpty()) " · ${members.size}" else "",
                        style = MaterialTheme.typography.titleMedium,
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
                                value = member.dateNaissance, onValueChange = { onDateNaissanceChange(member.key, it) },
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

@Composable
private fun LocalPhotoPreview(path: String) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = runCatching {
            context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    bitmapState.value?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo de la fiche",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)).padding(bottom = 8.dp),
        )
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
