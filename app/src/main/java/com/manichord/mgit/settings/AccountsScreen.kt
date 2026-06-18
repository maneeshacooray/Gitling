package com.manichord.mgit.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import me.sheimi.sgit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val accounts by viewModel.accounts.observeAsState(emptyList())
    val authInProgress by viewModel.githubAuthInProgress.observeAsState(false)
    val authMessage by viewModel.githubAuthMessage.observeAsState(null)
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Managed Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add account manually")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (authInProgress || authMessage != null) {
                item {
                    GitHubAuthStatusBanner(
                        inProgress = authInProgress,
                        message = authMessage,
                        onDismiss = { viewModel.dismissGitHubAuthMessage() }
                    )
                }
            }
            if (accounts.isEmpty()) {
                item {
                    EmptyAccountsState(
                        onLoginWithGitHub = { viewModel.launchGitHubAuth() },
                        onAddManually = { showAddDialog = true }
                    )
                }
            } else {
                item {
                    ListItem(
                        headlineContent = { Text("Connect with GitHub") },
                        supportingContent = { Text("Authenticate via browser OAuth") },
                        leadingContent = {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { viewModel.launchGitHubAuth() }
                    )
                    HorizontalDivider()
                }
                items(accounts, key = { it.id }) { account ->
                    AccountItem(
                        account = account,
                        onDeleteClick = { accountToDelete = account }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove Account") },
            text = {
                Text("Remove \"${account.name}\"? This will not revoke the token on ${account.type.displayName}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(account.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(R.string.label_cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { account ->
                viewModel.addAccount(account)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GitHubAuthStatusBanner(
    inProgress: Boolean,
    message: String?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = message ?: "Waiting for you to approve in the browser…",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!inProgress && message != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsState(
    onLoginWithGitHub: () -> Unit,
    onAddManually: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "No accounts yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Connect GitHub via OAuth for one-tap sign-in, or manually add any Git host using a Personal Access Token.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onLoginWithGitHub,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Connect with GitHub")
        }
        OutlinedButton(
            onClick = onAddManually,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add manually (PAT / Password)")
        }
    }
}

@Composable
fun AccountItem(account: Account, onDeleteClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(account.name) },
        supportingContent = { Text("${account.username} · ${account.type.displayName}") },
        leadingContent = {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove account",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.GITHUB) }
    var expanded by remember { mutableStateOf(false) }
    var tokenVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account label (e.g. Work GitHub)") },
                    singleLine = true,
                    isError = showErrors && name.isBlank(),
                    supportingText = if (showErrors && name.isBlank()) {
                        { Text("Required") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    isError = showErrors && username.isBlank(),
                    supportingText = if (showErrors && username.isBlank()) {
                        { Text("Required") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Personal Access Token / Password") },
                    singleLine = true,
                    isError = showErrors && token.isBlank(),
                    supportingText = if (showErrors && token.isBlank()) {
                        { Text("Required") }
                    } else null,
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (tokenVisible) "Hide token" else "Show token"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == AccountType.GITHUB || type == AccountType.GITLAB || type == AccountType.BITBUCKET) {
                    val tokenUrl = when (type) {
                        AccountType.GITHUB -> "https://github.com/settings/tokens/new?scopes=repo,user"
                        AccountType.GITLAB -> "https://gitlab.com/-/user_settings/personal_access_tokens"
                        else -> "https://bitbucket.org/account/settings/app-passwords/new"
                    }
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tokenUrl)))
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Text("Get a token on ${type.displayName} →")
                    }
                }

                Box {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account type") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AccountType.entries.forEach { accountType ->
                            DropdownMenuItem(
                                text = { Text(accountType.displayName) },
                                onClick = {
                                    type = accountType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || username.isBlank() || token.isBlank()) {
                        showErrors = true
                    } else {
                        onConfirm(Account(name = name, username = username, token = token, type = type))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        }
    )
}
