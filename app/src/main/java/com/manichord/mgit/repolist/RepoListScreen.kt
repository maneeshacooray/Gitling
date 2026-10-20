package com.manichord.mgit.repolist

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepoListScreen(
    repoList: List<Repo>,
    isGitHubConnected: Boolean,
    githubBannerDismissed: Boolean,
    onDismissGitHubBanner: () -> Unit,
    onRepoClick: (Repo) -> Unit,
    onRepoLongClick: (Repo) -> Unit,
    onCloneClick: () -> Unit,
    onInitLocalClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onConnectGitHubClick: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var addMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    BackHandler(enabled = addMenuExpanded) { addMenuExpanded = false }
    val searchState = rememberTextFieldState()
    val searchQuery = searchState.text.toString()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    val allTags: List<String> = remember(repoList) {
        repoList.flatMap { repo: me.sheimi.sgit.database.models.Repo ->
            repo.labels.filterIsInstance<String>()
        }.distinct().sorted()
    }

    // If the selected tag was removed, fall back to "All" without mutating state during composition
    val effectiveTag: String? = if (selectedTag != null && allTags.contains(selectedTag)) selectedTag else null

    val displayedRepoList = remember(repoList, searchQuery, selectedTag) {
        repoList
            .filter { effectiveTag == null || it.hasLabel(effectiveTag) }
            .filter { searchQuery.isBlank() || it.matchesSearch(searchQuery) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        val focusRequester = searchFocusRequester
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        TextField(
                            state = searchState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search repositories") },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchState.clearText()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchState.clearText() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                // Expressive flexible bar: large title with the repo count, collapsing to one
                // line as the list scrolls
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    subtitle = {
                        if (repoList.isNotEmpty()) {
                            Text(pluralStringResource(R.plurals.repo_count, repoList.size, repoList.size))
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        floatingActionButton = {
            AddRepositoryMenu(
                expanded = addMenuExpanded,
                onExpandedChange = { addMenuExpanded = it },
                onCloneClick = onCloneClick,
                onInitLocalClick = onInitLocalClick,
                onImportClick = onImportClick
            )
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
                    onDismissClick = onDismissGitHubBanner
                )
            }
            if (allTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = effectiveTag == null,
                            onClick = { selectedTag = null },
                            label = { Text("All") }
                        )
                    }
                    items(allTags, key = { it }) { tag ->
                        FilterChip(
                            selected = effectiveTag == tag,
                            onClick = { selectedTag = if (effectiveTag == tag) null else tag },
                            label = { Text(tag) }
                        )
                    }
                }
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
                        items(displayedRepoList, key = { it.getID() }) { repo ->
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
        // Dims the list while the add menu is open; a tap outside closes it
        AnimatedVisibility(visible = addMenuExpanded, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(R.string.label_close_menu)
                    ) { addMenuExpanded = false }
            )
        }
    }
}

/** The + button: opens a menu with every way to add a repository, so Import doesn't hide at
 * the bottom of the clone sheet. Each item closes the menu before acting. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddRepositoryMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCloneClick: () -> Unit,
    onInitLocalClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val toggleLabel = stringResource(if (expanded) R.string.label_close_menu else R.string.label_add_repository)
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            // Primary both closed and open, like the + button before; the default starts out
            // in the much lighter primary container color
            val primary = MaterialTheme.colorScheme.primary
            val onPrimary = MaterialTheme.colorScheme.onPrimary
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                modifier = Modifier.semantics { contentDescription = toggleLabel },
                containerColor = ToggleFloatingActionButtonDefaults.containerColor(primary, primary)
            ) {
                Icon(
                    imageVector = if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null,
                    modifier = with(ToggleFloatingActionButtonDefaults) {
                        Modifier.animateIcon(
                            checkedProgress = { checkedProgress },
                            color = ToggleFloatingActionButtonDefaults.iconColor(onPrimary, onPrimary)
                        )
                    }
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = { onExpandedChange(false); onCloneClick() },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            text = { Text(stringResource(R.string.label_menu_clone)) }
        )
        FloatingActionButtonMenuItem(
            onClick = { onExpandedChange(false); onInitLocalClick() },
            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
            text = { Text(stringResource(R.string.label_menu_new_local)) }
        )
        FloatingActionButtonMenuItem(
            onClick = { onExpandedChange(false); onImportClick() },
            icon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
            text = { Text(stringResource(R.string.label_import_repo)) }
        )
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
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismissClick) { Text("Not now") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(shapes = ButtonDefaults.shapes(), onClick = onConnectClick) { Text("Connect") }
            }
        }
    }
}
