@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.manichord.mgit.repodetail

import androidx.compose.foundation.background
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
    toolbarActions: RepoToolbarActions? = null,
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
    val syncCounts by viewModel.syncCounts.observeAsState()
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
    // The view model's selected tab and the pager follow each other: operations switch tabs
    // through the view model (Diff opens Commits), and swiping keeps it current so the next
    // switch is always a change it can see
    val selectedTab by viewModel.selectedTab.observeAsState(0)
    LaunchedEffect(selectedTab) {
        if (selectedTab != pagerState.currentPage) pagerState.animateScrollToPage(selectedTab)
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setSelectedTab(pagerState.currentPage)
    }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchState = rememberTextFieldState()
    val searchQuery = searchState.text.toString()
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler {
        when {
            // The operations drawer first: Back used to leave the repo with the drawer still open
            isDrawerOpen -> viewModel.setDrawerOpen(false)
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navigationIcon: @Composable () -> Unit = {
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
    }
    val topBarActions: @Composable RowScope.() -> Unit = {
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
                RepoOperationList(
                    repoName = repo?.diaplayName,
                    branch = repo?.branchName,
                    onOperationClick = {
                        onOperationClick(it)
                        scope.launch { drawerState.close() }
                    }
                )
            }
            }
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            // Hidden on the Console tab, whose command input sits at the bottom, and while searching
            bottomBar = {
                if (toolbarActions != null && pagerState.currentPage != TAB_CONSOLE && !isSearchActive) {
                    RepoToolbar(toolbarActions, syncCounts)
                }
            },
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
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
                        },
                        navigationIcon = navigationIcon,
                        actions = topBarActions
                    )
                } else {
                    // Expressive flexible bar: the repo name large with the branch underneath,
                    // collapsing as the tab content scrolls. The tabs draw their content directly
                    // in this composition (see HeadlessFragment), so their scrolling reaches it
                    MediumFlexibleTopAppBar(
                        title = {
                            Text(
                                text = repo?.diaplayName ?: "Loading...",
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        subtitle = {
                            repo?.branchName?.let { branch ->
                                BranchSubtitle(branch = branch, onClick = onBranchClick)
                            }
                        },
                        navigationIcon = navigationIcon,
                        actions = topBarActions,
                        // Shorter than the default expanded height: the default leaves a tall
                        // gap under the icon row, which never collapses in a repo whose file
                        // list fits on screen
                        expandedHeight = 120.dp,
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                // Icons beside the labels when every label fits next to its icon in a quarter of
                // the width; text only otherwise (narrow phones, longer translations), so no
                // label gets cut off and the tabs stay alike
                val tabLabelStyle = MaterialTheme.typography.titleSmall
                val textMeasurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val tabWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() } / tabs.size
                val showTabIcons = tabs.all { tab ->
                    val labelWidth = with(density) { textMeasurer.measure(tab.title, tabLabelStyle).size.width.toDp() }
                    labelWidth + 18.dp + 6.dp + 8.dp <= tabWidth
                }
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, tab ->
                        // Icon beside the label rather than above it: a 48dp row instead of 72dp.
                        // Built by hand because LeadingIconTab's padding cuts labels off on a phone
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(horizontal = 4.dp)
                            ) {
                                if (showTabIcons) {
                                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    tab.title,
                                    style = tabLabelStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                                // Some git operations don't report a total; show the
                                // indeterminate wave then instead of a bar stuck at 0
                                if (progressState!!.progress > 0) {
                                    LinearWavyProgressIndicator(
                                        progress = { progressState!!.progress / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
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

// Indexes into RepoOperationDelegate's mActions: what onOperationClick passes back
private const val OP_NEW_BRANCH = 0
private const val OP_PULL = 1
private const val OP_PUSH = 2
private const val OP_ADD_ALL = 3
private const val OP_COMMIT = 4
private const val OP_RESET = 5
private const val OP_MERGE = 6
private const val OP_FETCH = 7
private const val OP_REBASE = 8
private const val OP_CHERRY_PICK = 9
private const val OP_DIFF = 10
private const val OP_NEW_FILE = 11
private const val OP_NEW_DIR = 12
private const val OP_ADD_REMOTE = 13
private const val OP_REMOVE_REMOTE = 14
private const val OP_DELETE = 15
private const val OP_RAW_CONFIG = 16
private const val OP_OPTIONS = 17
private const val OP_SUBMODULE_UPDATE = 18

/** The drawer's operations by what they're for, in the same rounded groups as Settings. */
private val operationGroups = listOf(
    R.string.drawer_group_sync to listOf(OP_FETCH, OP_PULL, OP_PUSH, OP_SUBMODULE_UPDATE),
    R.string.drawer_group_changes to listOf(OP_ADD_ALL, OP_COMMIT, OP_DIFF, OP_RESET),
    R.string.drawer_group_branches to listOf(OP_NEW_BRANCH, OP_MERGE, OP_REBASE, OP_CHERRY_PICK),
    R.string.drawer_group_files to listOf(OP_NEW_FILE, OP_NEW_DIR),
    R.string.drawer_group_remotes to listOf(OP_ADD_REMOTE, OP_REMOVE_REMOTE),
    R.string.drawer_group_repository to listOf(OP_RAW_CONFIG, OP_OPTIONS)
)

private fun operationIcon(op: Int): ImageVector = when (op) {
    OP_NEW_BRANCH -> Icons.Default.AccountTree
    OP_PULL -> Icons.Default.CloudDownload
    OP_PUSH -> Icons.Default.CloudUpload
    OP_ADD_ALL -> Icons.Default.PlaylistAddCheck
    OP_COMMIT -> Icons.Default.Commit
    OP_RESET -> Icons.Default.RestartAlt
    OP_MERGE -> Icons.Default.CallMerge
    OP_FETCH -> Icons.Default.Sync
    OP_REBASE -> Icons.Default.Timeline
    OP_CHERRY_PICK -> Icons.Default.ContentCopy
    OP_DIFF -> Icons.Default.Difference
    OP_NEW_FILE -> Icons.Default.NoteAdd
    OP_NEW_DIR -> Icons.Default.CreateNewFolder
    OP_ADD_REMOTE -> Icons.Default.AddLink
    OP_REMOVE_REMOTE -> Icons.Default.LinkOff
    OP_DELETE -> Icons.Default.Delete
    OP_RAW_CONFIG -> Icons.Default.Code
    OP_OPTIONS -> Icons.Default.Settings
    else -> Icons.Default.Layers // OP_SUBMODULE_UPDATE
}

/**
 * The operations drawer: the repo and its branch on top, then every operation grouped by what
 * it's for. Delete sits apart at the bottom, in the error colors, so it isn't hit by mistake.
 */
@Composable
fun RepoOperationList(repoName: String?, branch: String?, onOperationClick: (index: Int) -> Unit) {
    // Translated names, in mActions order; the last two entries come from their own strings
    val names = stringArrayResource(R.array.repo_operation_names)
    val submoduleLabel = stringResource(R.string.menu_submodule_update)
    val deleteLabel = stringResource(R.string.menu_delete_repo)
    fun label(op: Int) = when (op) {
        OP_SUBMODULE_UPDATE -> submoduleLabel
        OP_DELETE -> deleteLabel
        else -> names.getOrElse(op) { "" }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                repoName.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!branch.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        branch.removePrefix("refs/heads/"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        operationGroups.forEach { (title, ops) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OperationGroup(MaterialTheme.colorScheme.surfaceContainerHigh) {
                    ops.forEachIndexed { i, op ->
                        if (i > 0) {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            )
                        }
                        OperationRow(label(op), operationIcon(op), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurface) {
                            onOperationClick(op)
                        }
                    }
                }
            }
        }
        OperationGroup(MaterialTheme.colorScheme.errorContainer) {
            OperationRow(
                label(OP_DELETE),
                operationIcon(OP_DELETE),
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.onErrorContainer
            ) { onOperationClick(OP_DELETE) }
        }
    }
}

@Composable
private fun OperationGroup(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun OperationRow(label: String, icon: ImageVector, iconTint: Color, textColor: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}

data class TabItem(val title: String, val icon: ImageVector)

/** The current branch under the repo name; tapping it opens the branch chooser, like the chip
 * it replaces. */
@Composable
private fun BranchSubtitle(branch: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = stringResource(R.string.label_switch_branch), onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = branch.removePrefix("refs/heads/"), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
