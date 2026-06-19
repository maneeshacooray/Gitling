package com.manichord.mgit.repolist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.manichord.mgit.clone.CloneViewModel
import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import com.manichord.mgit.models.GitHubRepo
import com.manichord.mgit.util.resolvePrimaryVolumePath
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import me.sheimi.sgit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneView(
    viewModel: CloneViewModel,
    onCloneClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sync remoteUrl from VM to local state
    var remoteUrl by remember(viewModel.remoteUrl) { mutableStateOf(viewModel.remoteUrl) }
    val localRepoName by viewModel.localRepoName.observeAsState("")
    val initLocal by viewModel.initLocal.observeAsState(false)
    val cloneRecursively by remember { mutableStateOf(viewModel.cloneRecursively) }

    val remoteUrlError by viewModel.remoteUrlError.observeAsState()
    val localRepoNameError by viewModel.localRepoNameError.observeAsState()

    val cloneLocation by viewModel.cloneLocation.observeAsState("")
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = resolvePrimaryVolumePath(it)
            if (path != null) {
                viewModel.cloneLocation.value = path
            } else {
                Toast.makeText(
                    context,
                    "That location isn't supported -- please pick a folder on internal storage.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp) // Extra padding for bottom sheet
    ) {
        Text(
            text = if (initLocal) stringResource(id = R.string.dialog_clone_neutral_label) else stringResource(id = R.string.title_clone_repo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!initLocal) {
            val accounts by viewModel.accounts.observeAsState(emptyList())
            val selectedAccount by viewModel.selectedAccount.observeAsState()
            var expanded by remember { mutableStateOf(false) }

            if (accounts.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Select Account (Optional)",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Git Account") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Manual Credentials)") },
                            onClick = {
                                viewModel.selectedAccount.value = null
                                expanded = false
                            }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${account.username})") },
                                onClick = {
                                    viewModel.selectAccount(account)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // GitHub Repo Browser
                if (selectedAccount?.type == AccountType.GITHUB) {
                    Spacer(modifier = Modifier.height(16.dp))
                    GitHubRepoBrowser(
                        viewModel = viewModel,
                        onRepoSelected = { repo ->
                            viewModel.remoteUrl = repo.cloneUrl
                            // Auto-set local name if it's currently empty or just the old repo name
                            if (viewModel.localRepoName.value.isNullOrBlank()) {
                                viewModel.localRepoName.value = repo.name
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = remoteUrl,
                onValueChange = {
                    remoteUrl = it
                    viewModel.remoteUrl = it  // sync to VM
                },
                label = { Text(stringResource(id = R.string.label_remote_url)) },
                placeholder = { Text("https://github.com/user/repo.git") },
                modifier = Modifier.fillMaxWidth(),
                isError = !remoteUrlError.isNullOrEmpty(),
                supportingText = {
                    if (!remoteUrlError.isNullOrEmpty()) {
                        Text(text = remoteUrlError!!)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = localRepoName,
            onValueChange = { viewModel.localRepoName.value = it },
            label = { Text(stringResource(id = R.string.dialog_clone_local_path_hint)) },
            modifier = Modifier.fillMaxWidth(),
            isError = !localRepoNameError.isNullOrEmpty(),
            supportingText = {
                if (!localRepoNameError.isNullOrEmpty()) {
                    Text(text = localRepoNameError!!)
                } else {
                    Text(text = "Cloning to: $cloneLocation/$localRepoName")
                }
            },
            trailingIcon = {
                IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Change Parent Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Options
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = initLocal,
                        onCheckedChange = { viewModel.initLocal.value = it }
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_clone_neutral_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (!initLocal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = cloneRecursively,
                            onCheckedChange = { viewModel.cloneRecursively = it }
                        )
                        Text(
                            text = stringResource(id = R.string.dialog_clone_recursive),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(stringResource(id = R.string.label_cancel))
            }
            Button(
                onClick = onCloneClick,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(
                    if (initLocal) stringResource(id = R.string.label_init)
                    else stringResource(id = R.string.label_clone)
                )
            }
        }
    }
}

@Composable
fun GitHubRepoBrowser(
    viewModel: CloneViewModel,
    onRepoSelected: (GitHubRepo) -> Unit
) {
    val repos by viewModel.githubRepos.collectAsState()
    val isLoading by viewModel.isLoadingRepos.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
        Text(
            "Your GitHub Repositories",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search your repos...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val filteredRepos = repos.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        (it.description?.contains(searchQuery, ignoreCase = true) ?: false)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredRepos) { repo ->
                    GitHubRepoItem(repo = repo, onClick = { onRepoSelected(repo) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun GitHubRepoItem(repo: GitHubRepo, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(repo.name, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                if (!repo.description.isNullOrBlank()) {
                    Text(repo.description, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(" ${repo.stars}", style = MaterialTheme.typography.bodySmall)
                    repo.language?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        leadingContent = { Icon(Icons.Default.Code, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
