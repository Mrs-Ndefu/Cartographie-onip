package com.onip.cartoonip.ui.profile

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onip.cartoonip.ui.theme.OnipBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.uploadPhoto(context, it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ProfileAvatar(photoDataUrl = uiState.agent?.photoDataUrl, initial = uiState.agent?.fullName?.firstOrNull())
                    Text(
                        uiState.agent?.fullName ?: "…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    uiState.agent?.username?.let {
                        Text("@$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        enabled = !uiState.photoUploading,
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Text(if (uiState.photoUploading) "Envoi…" else "Changer la photo")
                    }
                    uiState.photoError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Changer le mot de passe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OnipBlue)

                        OutlinedTextField(
                            value = uiState.currentPassword,
                            onValueChange = viewModel::onCurrentPasswordChange,
                            label = { Text("Mot de passe actuel") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.newPassword,
                            onValueChange = viewModel::onNewPasswordChange,
                            label = { Text("Nouveau mot de passe") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = { Text("Confirmer le nouveau mot de passe") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        uiState.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        uiState.passwordSuccess?.let { Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall) }

                        Button(
                            onClick = viewModel::changePassword,
                            enabled = !uiState.passwordLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (uiState.passwordLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Mettre à jour le mot de passe")
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Déconnexion", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(photoDataUrl: String?, initial: Char?) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = photoDataUrl) {
        value = photoDataUrl?.let { dataUrl ->
            runCatching {
                val base64 = dataUrl.substringAfter(",")
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo de profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (initial != null) {
            Text(initial.uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(48.dp))
        }
    }
}
