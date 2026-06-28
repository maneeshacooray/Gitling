package com.manichord.mgit.repolist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.manichord.mgit.ui.components.EmptyStateView
import com.manichord.mgit.ui.components.RepoCard
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo

private fun Repo.matchesSearch(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return listOfNotNull(getDiaplayName(), remoteURL, lastCommitter, lastCommitMsg)
        .any { it.contains(q, ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    repoList: List<Repo>,
    isGitHubConnected: Boolean,
    githubBannerDismissed: Boolean,
    onDismissGitHubBanner: () -> Unit,
    onRepoClick: (Repo) -> Unit,
    onRepoLongClick: (Repo) -> Unit,
    onCloneClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onConnectGitHubClick: () -> Unit,
    updateAvailableVersion: String? = null,
    onViewReleaseClick: () -> Unit = {},
    onDismissUpdateClick: () -> Unit = {}
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    val displayedRepoList = remember(repoList, searchQuery) {
        if (searchQuery.isBlank()) repoList else repoList.filter { it.matchesSearch(searchQuery) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        val focusRequester = searchFocusRequester
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search repositories") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    } else {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
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
            if (updateAvailableVersion != null) {
                UpdateAvailableBanner(
                    versionName = updateAvailableVersion,
                    onViewReleaseClick = onViewReleaseClick,
                    onDismissClick = onDismissUpdateClick
                )
            }
            if (!isGitHubConnected && !githubBannerDismissed && repoList.isNotEmpty()) {
                ConnectGitHubBanner(
                    onConnectClick = onConnectGitHubClick,
                    onDismissClick = onDismissGitHubBanner
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (repoList.isEmpty()) {
                    EmptyStateView(
                        isGitHubConnected = isGitHubConnected,
                        onActionClick = onCloneClick,
                        onConnectGitHubClick = onConnectGitHubClick
                    )
                } else if (displayedRepoList.isEmpty()) {
                    NoSearchResultsView(query = searchQuery)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp) // Space for FAB
                    ) {
                        items(displayedRepoList) { repo ->
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
private fun NoSearchResultsView(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No repositories match \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun UpdateAvailableBanner(
    versionName: String,
    onViewReleaseClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp, 16.dp, 0.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Update available",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Version $versionName is available on GitHub",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissClick) { Text("Not now") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onViewReleaseClick) { Text("View release") }
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
