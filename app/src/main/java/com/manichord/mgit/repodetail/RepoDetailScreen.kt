package com.manichord.mgit.repodetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.manichord.mgit.ui.components.onUserTextChange

private const val TAB_FILES = 0
private const val TAB_COMMITS = 1
private const val TAB_STATUS = 2
private const val TAB_CONSOLE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    viewModel: RepoDetailViewModel,
    onBackClick: () -> Unit,
    onNavigateUp: () -> Boolean,
    onBranchClick: () -> Unit,
    onOperationClick: (index: Int) -> Unit,
    filesContent: @Composable () -> Unit,
    commitsContent: @Composable () -> Unit,
    statusContent: @Composable () -> Unit,
    consoleContent: @Composable () -> Unit = {},
    onFilesSearchQueryChange: (String) -> Unit = {},
    onCommitsSearchQueryChange: (String) -> Unit = {}
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
        TabItem(stringResource(R.string.tab_status_label), Icons.Default.Assessment),
        TabItem(stringResource(R.string.tab_console_label), Icons.Outlined.Terminal)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var isSearchActive by remember { mutableStateOf(false) }
    val searchState = rememberTextFieldState()
    val searchQuery = searchState.text.toString()
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler {
        when {
            isSearchActive -> {
                isSearchActive = false
                searchState.clearText()
                onFilesSearchQueryChange("")
                onCommitsSearchQueryChange("")
            }
            pagerState.currentPage == TAB_FILES && onNavigateUp() -> Unit
            else -> onBackClick()
        }
    }

    // Each tab's search is independent -- switching tabs while searching would otherwise leave
    // a stale query applied to whichever tab the user navigated away from, so just exit search.
    LaunchedEffect(pagerState.currentPage) {
        if (isSearchActive) {
            isSearchActive = false
            searchState.clearText()
            onFilesSearchQueryChange("")
            onCommitsSearchQueryChange("")
        }
    }

    // ModalNavigationDrawer always opens from the layout-start edge, with no built-in option to
    // open from the end -- but the menu button that opens it lives in the TopAppBar's `actions`,
    // which Compose always renders at the layout-end (issue #64: button and drawer were on
    // opposite sides). Flipping LocalLayoutDirection for just the drawer container, then
    // flipping back to the real direction for both the drawer's own content and the screen
    // behind it, makes the drawer's start edge become the visual end edge instead -- matching
    // wherever the button actually renders. This must invert the *current* direction rather than
    // hardcode Rtl, so it still opens on the correct side (the left) for an RTL locale (e.g.
    // Arabic, see values-ar/), where the button itself renders on the left too.
    val layoutDirection = LocalLayoutDirection.current
    val drawerLayoutDirection = if (layoutDirection == LayoutDirection.Ltr) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides drawerLayoutDirection) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                            TextField(
                                state = searchState,
                                inputTransformation = onUserTextChange { query ->
                                    when (pagerState.currentPage) {
                                        TAB_FILES -> onFilesSearchQueryChange(query)
                                        TAB_COMMITS -> onCommitsSearchQueryChange(query)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                                placeholder = {
                                    Text(
                                        if (pagerState.currentPage == TAB_FILES) "Search files"
                                        else "Search commits"
                                    )
                                },
                                lineLimits = TextFieldLineLimits.SingleLine,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            Column {
                                Text(
                                    text = repo?.diaplayName ?: "Loading...",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                repo?.branchName?.let { branch ->
                                    SuggestionChip(
                                        onClick = onBranchClick,
                                        label = {
                                            Text(
                                                text = branch,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        icon = { Icon(Icons.Default.AccountTree, null, Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchState.clearText()
                                when (pagerState.currentPage) {
                                    TAB_FILES -> onFilesSearchQueryChange("")
                                    TAB_COMMITS -> onCommitsSearchQueryChange("")
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                            }
                        } else {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchState.clearText()
                                    when (pagerState.currentPage) {
                                        TAB_FILES -> onFilesSearchQueryChange("")
                                        TAB_COMMITS -> onCommitsSearchQueryChange("")
                                    }
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        } else {
                            if (pagerState.currentPage == TAB_FILES || pagerState.currentPage == TAB_COMMITS) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                            if (pagerState.currentPage == TAB_CONSOLE) {
                                IconButton(onClick = { viewModel.clearConsole() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear console")
                                }
                            }
                            IconButton(onClick = { viewModel.setDrawerOpen(true) }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
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
                            3 -> consoleContent()
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
        "Options" to Icons.Default.Settings,
        "Submodule Update" to Icons.Default.AccountTree
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
