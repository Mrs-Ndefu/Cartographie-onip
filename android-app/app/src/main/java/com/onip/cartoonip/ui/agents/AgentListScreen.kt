package com.onip.cartoonip.ui.agents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.onip.cartoonip.data.model.AgentDto
import com.onip.cartoonip.data.model.AgentRole
import com.onip.cartoonip.ui.common.AppScaffold
import com.onip.cartoonip.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(navController: NavHostController, viewModel: AgentListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var resetPasswordTarget by remember { mutableStateOf<AgentDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearActionError()
        }
    }

    AppScaffold(navController = navController, currentRoute = Routes.AGENTS, title = "Agents") { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.agents, key = { it.id }) { agent ->
                        AgentRow(
                            agent = agent,
                            isSelf = agent.username == viewModel.currentUsername,
                            onToggleActive = { viewModel.setActive(agent, !agent.active) },
                            onChangeRole = { role -> viewModel.changeRole(agent, role) },
                            onResetPassword = { resetPasswordTarget = agent },
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Créer un agent")
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (showCreateDialog) {
        CreateAgentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { username, password, fullName, role ->
                viewModel.createAgent(username, password, fullName, role) { success ->
                    if (success) showCreateDialog = false
                }
            },
        )
    }

    resetPasswordTarget?.let { agent ->
        ResetPasswordDialog(
            agent = agent,
            onDismiss = { resetPasswordTarget = null },
            onConfirm = { newPassword ->
                viewModel.resetPassword(agent, newPassword) { success ->
                    if (success) resetPasswordTarget = null
                }
            },
        )
    }
}

@Composable
private fun AgentRow(
    agent: AgentDto,
    isSelf: Boolean,
    onToggleActive: () -> Unit,
    onChangeRole: (AgentRole) -> Unit,
    onResetPassword: () -> Unit,
) {
    var roleMenuExpanded by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(agent.fullName, style = MaterialTheme.typography.titleMedium)
                    Text("@${agent.username}" + if (isSelf) " (vous)" else "", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = agent.active, onCheckedChange = { onToggleActive() }, enabled = !isSelf)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    TextButton(onClick = { roleMenuExpanded = true }) {
                        Text(if (agent.role == AgentRole.ADMIN) "Rôle : Admin" else "Rôle : Agent")
                    }
                    DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                        AgentRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(if (role == AgentRole.ADMIN) "Admin" else "Agent") },
                                onClick = {
                                    roleMenuExpanded = false
                                    onChangeRole(role)
                                },
                                enabled = !(isSelf && role != AgentRole.ADMIN),
                            )
                        }
                    }
                }
                TextButton(onClick = onResetPassword) { Text("Réinitialiser mot de passe") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAgentDialog(
    onDismiss: () -> Unit,
    onCreate: (username: String, password: String, fullName: String, role: AgentRole) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(AgentRole.AGENT) }
    var roleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel agent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nom complet") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Identifiant") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe (min. 6 caractères)") }, singleLine = true)

                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = if (role == AgentRole.ADMIN) "Admin" else "Agent",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rôle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        AgentRole.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(if (entry == AgentRole.ADMIN) "Admin" else "Agent") },
                                onClick = { role = entry; roleExpanded = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(username.trim(), password, fullName.trim(), role) },
                enabled = username.isNotBlank() && password.length >= 6 && fullName.isNotBlank(),
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun ResetPasswordDialog(agent: AgentDto, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réinitialiser le mot de passe de ${agent.username}") },
        text = {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Nouveau mot de passe (min. 6 caractères)") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newPassword) }, enabled = newPassword.length >= 6) { Text("Confirmer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
