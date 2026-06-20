package com.manichord.mgit.repodetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    viewModel: RepoDetailViewModel,
    onBackClick: () -> Unit,
    onBranchClick: () -> Unit,
    onOperationClick: (index: Int) -> Unit,
    filesContent: @Composable () -> Unit,
    commitsContent: @Composable () -> Unit,
    statusContent: @Composable () -> Unit
) {
    val repo by viewModel.repo.observeAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.observeAsState(false)
    val progressState by viewModel.progressState.observeAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.isClosed) viewModel.setDrawerOpen(false)
        else viewModel.setDrawerOpen(true)
    }

    val tabs = listOf(
        TabItem(stringResource(R.string.tab_files_label), Icons.Default.FolderCopy),
        TabItem(stringResource(R.string.tab_commits_label), Icons.Default.History),
        TabItem(stringResource(R.string.tab_status_label), Icons.Default.Assessment)
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.action_toggle_drawer),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Divider()
                // Repository Operations List
                RepoOperationList(onOperationClick = {
                    onOperationClick(it)
                    scope.launch { drawerState.close() }
                })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = repo?.diaplayName ?: "Loading...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            repo?.branchName?.let { branch ->
                                SuggestionChip(
                                    onClick = onBranchClick,
                                    label = { Text(branch) },
                                    icon = { Icon(Icons.Default.AccountTree, null, Modifier.size(16.dp)) }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setDrawerOpen(true) }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, contentDescription = null) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { index ->
                        when (index) {
                            0 -> filesContent()
                            1 -> commitsContent()
                            2 -> statusContent()
                        }
                    }

                    // Progress Overlay
                    if (progressState?.visible == true) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = progressState!!.message,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = progressState!!.progress / 100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(progressState!!.leftHint)
                                    Text(progressState!!.rightHint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoOperationList(onOperationClick: (index: Int) -> Unit) {
    // These names/icons match me.sheimi.sgit.activities.delegate.RepoOperationDelegate's
    // mActions order -- index is what gets passed back to onOperationClick.
    val operations = listOf(
        "New Branch" to Icons.Default.AccountTree,
        "Pull" to Icons.Default.CloudDownload,
        "Push" to Icons.Default.CloudUpload,
        "Add All" to Icons.Default.PlaylistAddCheck,
        "Commit" to Icons.Default.Save,
        "Reset" to Icons.Default.RestartAlt,
        "Merge" to Icons.Default.CallMerge,
        "Fetch" to Icons.Default.Sync,
        "Rebase" to Icons.Default.Timeline,
        "Cherry Pick" to Icons.Default.ContentCopy,
        "Diff" to Icons.Default.Difference,
        "New File" to Icons.Default.NoteAdd,
        "New Directory" to Icons.Default.CreateNewFolder,
        "Add Remote" to Icons.Default.AddLink,
        "Remove Remote" to Icons.Default.LinkOff,
        "Delete" to Icons.Default.Delete,
        "Raw Config" to Icons.Default.Code,
        "Options" to Icons.Default.Settings
    )

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        operations.forEachIndexed { index, (op, icon) ->
            NavigationDrawerItem(
                label = { Text(op) },
                icon = { Icon(icon, contentDescription = null) },
                selected = false,
                onClick = { onOperationClick(index) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)
