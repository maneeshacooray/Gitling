package com.manichord.mgit.repolist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manichord.mgit.ui.components.EmptyStateView
import com.manichord.mgit.ui.components.RepoCard
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    repoList: List<Repo>,
    isGitHubConnected: Boolean,
    onRepoClick: (Repo) -> Unit,
    onRepoLongClick: (Repo) -> Unit,
    onCloneClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onConnectGitHubClick: () -> Unit
) {
    var githubBannerDismissed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCloneClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add repository")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isGitHubConnected && !githubBannerDismissed && repoList.isNotEmpty()) {
                ConnectGitHubBanner(
                    onConnectClick = onConnectGitHubClick,
                    onDismissClick = { githubBannerDismissed = true }
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (repoList.isEmpty()) {
                    EmptyStateView(
                        isGitHubConnected = isGitHubConnected,
                        onActionClick = onCloneClick,
                        onConnectGitHubClick = onConnectGitHubClick
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp) // Space for FAB
                    ) {
                        items(repoList) { repo ->
                            RepoCard(
                                repo = repo,
                                onClick = { onRepoClick(repo) },
                                onLongClick = { onRepoLongClick(repo) },
                                onCancelClick = { onRepoLongClick(repo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectGitHubBanner(
    onConnectClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp, 16.dp, 0.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Connect your GitHub account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Skip typing credentials for GitHub remotes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissClick) { Text("Not now") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConnectClick) { Text("Connect") }
            }
        }
    }
}
