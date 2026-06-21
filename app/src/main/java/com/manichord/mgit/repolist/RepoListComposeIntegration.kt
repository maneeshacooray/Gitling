package com.manichord.mgit.repolist

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
        val updateAvailable by viewModel.updateAvailable.collectAsState()
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

        val context = LocalContext.current
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
                showCloneSheet = true
            },
            onSearchClick = {
                // TODO: Implement search
            },
            onSettingsClick = {
                (activity as MainActivity).openUserSettings()
            },
            onConnectGitHubClick = {
                (activity as MainActivity).openUserSettings(initialScreen = "accounts")
            },
            updateAvailableVersion = updateAvailable?.versionName,
            onViewReleaseClick = {
                updateAvailable?.let { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.releaseUrl))) }
            },
            onDismissUpdateClick = { viewModel.dismissUpdateAvailable() }
        )

        if (showPermissionDialog) {
            PermissionRequiredDialog(
                onConfirm = {
                    viewModel.setShowPermissionDialog(false)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                    activity.startActivity(intent)
                },
                onDismiss = {
                    viewModel.setShowPermissionDialog(false)
                    activity.finish()
                }
            )
        }

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
                onDismissRequest = { repoOptionsTarget = null },
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
    }
}

@Composable
fun PermissionRequiredDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_access_all_files_title)) },
        text = { Text(stringResource(R.string.dialog_access_all_files_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.label_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.label_cancel))
            }
        }
    )
}

@Composable
private fun RepoOptionsDialog(
    repoName: String,
    onDismissRequest: () -> Unit,
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
