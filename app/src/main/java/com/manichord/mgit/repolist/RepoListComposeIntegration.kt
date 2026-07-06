package com.manichord.mgit.repolist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.manichord.mgit.MainActivity
import com.manichord.mgit.clone.CloneViewModel
import com.manichord.mgit.models.AccountType
import com.manichord.mgit.ui.theme.AppTheme
import com.manichord.mgit.whatsnew.WhatsNewContent
import com.manichord.mgit.whatsnew.WhatsNewDialog
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.android.utils.Profile
import androidx.compose.runtime.saveable.rememberSaveable
import me.sheimi.sgit.BuildConfig
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.models.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListComposeContent(
    activity: SheimiFragmentActivity,
    cloneViewModel: CloneViewModel,
    viewModel: RepoListViewModel,
    onCloneClick: () -> Unit,
    onCancelCloneViewClick: () -> Unit
) {
    AppTheme {
        val repoListSnapshot by viewModel.repoList.collectAsState()
        val showStorageMigrationNotice by viewModel.showStorageMigrationNotice.collectAsState()
        var showCloneSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState()

        val accountManager = (activity.application as MGitApplication).accountManager
        var isGitHubConnected by remember {
            mutableStateOf(accountManager?.getAccounts()?.any { it.type == AccountType.GITHUB } == true)
        }
        // Re-check on every recomposition trigger from resume isn't wired here, so refresh
        // whenever the repo list itself refreshes (good enough proxy for "activity resumed").
        LaunchedEffect(repoListSnapshot) {
            isGitHubConnected = accountManager?.getAccounts()?.any { it.type == AccountType.GITHUB } == true
        }

        var repoOptionsTarget by remember { mutableStateOf<Repo?>(null) }
        var renameTarget by remember { mutableStateOf<Repo?>(null) }
        var deleteTarget by remember { mutableStateOf<Repo?>(null) }
        var tagsTarget by remember { mutableStateOf<Repo?>(null) }

        val allTags: List<String> = remember(repoListSnapshot) {
            repoListSnapshot.flatMap { repo: Repo ->
                repo.labels.filterIsInstance<String>()
            }.distinct().sorted()
        }

        val context = LocalContext.current
        var githubBannerDismissed by rememberSaveable {
            mutableStateOf(Profile.getGitHubBannerDismissed(context))
        }

        var whatsNewEntries by remember {
            mutableStateOf(run {
                val lastSeen = Profile.getLastSeenVersionCode(context)
                when {
                    // Fresh install -- nothing to announce, just start tracking from here.
                    lastSeen < 0 -> {
                        Profile.setLastSeenVersionCode(context, BuildConfig.VERSION_CODE)
                        emptyList()
                    }
                    lastSeen < BuildConfig.VERSION_CODE ->
                        WhatsNewContent.entries.filter { it.versionCode in (lastSeen + 1)..BuildConfig.VERSION_CODE }
                    else -> emptyList()
                }
            })
        }

        RepoListScreen(
            repoList = repoListSnapshot,
            isGitHubConnected = isGitHubConnected,
            githubBannerDismissed = githubBannerDismissed,
            onDismissGitHubBanner = {
                githubBannerDismissed = true
                Profile.setGitHubBannerDismissed(context, true)
            },
            onRepoClick = { repo ->
                (activity as com.manichord.mgit.MainActivity).openRepoDetail(repo)
            },
            onRepoLongClick = { repo ->
                if (repo.repoStatus == RepoContract.REPO_STATUS_NULL) {
                    repoOptionsTarget = repo
                } else {
                    // Cancel the operational repo
                    repo.deleteRepo()
                    repo.cancelTask()
                }
            },
            onCloneClick = {
                cloneViewModel.refreshCloneLocation()
                showCloneSheet = true
            },
            onSettingsClick = {
                (activity as MainActivity).openUserSettings()
            },
            onConnectGitHubClick = {
                (activity as MainActivity).openUserSettings(initialScreen = "accounts")
            },
        )

        if (showCloneSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCloneSheet = false
                    onCancelCloneViewClick()
                },
                sheetState = sheetState
            ) {
                CloneView(
                    viewModel = cloneViewModel,
                    onCloneClick = {
                        showCloneSheet = false
                        onCloneClick()
                    },
                    onCancelClick = {
                        showCloneSheet = false
                        onCancelCloneViewClick()
                    }
                )
            }
        }

        repoOptionsTarget?.let { repo ->
            RepoOptionsDialog(
                repoName = (repo.getDiaplayName() ?: ""),
                isPinned = repo.isPinned,
                onDismissRequest = { repoOptionsTarget = null },
                onPinClick = {
                    repoOptionsTarget = null
                    repo.togglePinned()
                },
                onTagsClick = {
                    repoOptionsTarget = null
                    tagsTarget = repo
                },
                onRenameClick = {
                    repoOptionsTarget = null
                    renameTarget = repo
                },
                onDeleteClick = {
                    repoOptionsTarget = null
                    deleteTarget = repo
                }
            )
        }

        tagsTarget?.let { repo ->
            TagEditorDialog(
                repo = repo,
                allTags = allTags,
                onDismissRequest = { tagsTarget = null },
                onConfirm = { newTags ->
                    tagsTarget = null
                    repo.setLabels(newTags)
                }
            )
        }

        renameTarget?.let { repo ->
            RenameRepoDialog(
                initialName = (repo.getDiaplayName() ?: ""),
                onDismissRequest = { renameTarget = null },
                onConfirm = { newName ->
                    renameTarget = null
                    if (!repo.renameRepo(newName)) {
                        activity.showToastMessage(R.string.error_rename_repo_fail)
                    }
                }
            )
        }

        deleteTarget?.let { repo ->
            DeleteRepoDialog(
                repoName = (repo.getDiaplayName() ?: ""),
                onDismissRequest = { deleteTarget = null },
                onConfirm = {
                    deleteTarget = null
                    repo.deleteRepo()
                    repo.cancelTask()
                }
            )
        }

        if (whatsNewEntries.isNotEmpty()) {
            WhatsNewDialog(
                entries = whatsNewEntries,
                onDismiss = {
                    Profile.setLastSeenVersionCode(context, BuildConfig.VERSION_CODE)
                    whatsNewEntries = emptyList()
                }
            )
        }

        if (showStorageMigrationNotice) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissStorageMigrationNotice() },
                title = { Text("Storage access changed") },
                text = {
                    Text(
                        "Gitling no longer requests broad storage access, for privacy and " +
                            "to comply with F-Droid's review guidelines. Any repos you'd " +
                            "stored outside Gitling's own folder are no longer reachable -- " +
                            "you'll need to re-clone them. Sorry for the inconvenience."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissStorageMigrationNotice() }) {
                        Text(stringResource(R.string.label_ok))
                    }
                }
            )
        }
    }
}

@Composable
private fun RepoOptionsDialog(
    repoName: String,
    isPinned: Boolean,
    onDismissRequest: () -> Unit,
    onPinClick: () -> Unit,
    onTagsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(repoName) },
        text = {
            val transparentListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
            Column {
                ListItem(
                    headlineContent = { Text(if (isPinned) "Unpin" else "Pin") },
                    leadingContent = { Icon(Icons.Default.PushPin, contentDescription = null) },
                    colors = transparentListItemColors,
                    modifier = Modifier.clickable(onClick = onPinClick)
                )
                ListItem(
                    headlineContent = { Text("Tags") },
                    leadingContent = { Icon(Icons.Default.Label, contentDescription = null) },
                    colors = transparentListItemColors,
                    modifier = Modifier.clickable(onClick = onTagsClick)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.label_rename)) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    colors = transparentListItemColors,
                    modifier = Modifier.clickable(onClick = onRenameClick)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.label_delete)) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    colors = transparentListItemColors,
                    modifier = Modifier.clickable(onClick = onDeleteClick)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}

@Composable
private fun TagEditorDialog(
    repo: Repo,
    allTags: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf<Set<String>>(HashSet<String>(repo.labels)) }
    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Tags") },
        text = {
            val displayedTags = remember(allTags, selected) {
                (allTags + selected.toList()).distinct().sorted()
            }
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                displayedTags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (selected.contains(tag)) selected - tag else selected + tag
                            }
                    ) {
                        Checkbox(
                            checked = selected.contains(tag),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + tag else selected - tag
                            }
                        )
                        Text(tag, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        placeholder = { Text("New tag") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val tag = newTagText.trim().lowercase()
                            if (tag.isNotEmpty()) {
                                selected = selected + tag
                                newTagText = ""
                            }
                        },
                        enabled = newTagText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}

@Composable
private fun RenameRepoDialog(
    initialName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.dialog_rename_repo_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.dialog_rename_repo_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.label_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}

@Composable
private fun DeleteRepoDialog(
    repoName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.dialog_delete_repo_title)) },
        text = { Text(stringResource(R.string.dialog_delete_repo_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.label_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.label_cancel)) }
        }
    )
}
