package com.manichord.mgit

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.manichord.mgit.branchchooser.BranchChooserScreen
import com.manichord.mgit.branchchooser.BranchChooserViewModel
import com.manichord.mgit.clone.CloneViewModel
import com.manichord.mgit.diff.CommitDiffScreen
import com.manichord.mgit.dialogs.MessageDialog
import com.manichord.mgit.explorer.FileExplorerScreen
import com.manichord.mgit.repodetail.RepoDetailScreen
import com.manichord.mgit.repodetail.RepoDetailViewModel
import com.manichord.mgit.repolist.RepoListComposeContent
import com.manichord.mgit.repolist.RepoListViewModel
import com.manichord.mgit.settings.AccountsScreen
import com.manichord.mgit.settings.SettingsScreen
import com.manichord.mgit.settings.SettingsViewModel
import com.manichord.mgit.transport.MGitHttpConnectionFactory
import com.manichord.mgit.ui.components.FragmentHost
import com.manichord.mgit.ui.theme.AppTheme
import com.manichord.mgit.ui.theme.FontOption
import me.sheimi.android.activities.SheimiFragmentActivity
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.activities.CommitDiffActivity
import me.sheimi.sgit.activities.RepoDetailActivity
import me.sheimi.sgit.activities.explorer.ExploreFileActivity
import me.sheimi.sgit.activities.explorer.PrivateKeyManageActivity
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.dialogs.RenameBranchDialog
import me.sheimi.sgit.fragments.CommitsFragment
import me.sheimi.sgit.fragments.FilesFragment
import me.sheimi.sgit.fragments.StatusFragment
import me.sheimi.sgit.repo.tasks.repo.CheckoutTask
import me.sheimi.sgit.repo.tasks.SheimiAsyncTask
import me.sheimi.sgit.repo.tasks.repo.CloneTask
import me.sheimi.sgit.ssh.PrivateKeyUtils
import me.sheimi.android.utils.Profile
import timber.log.Timber
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Single host Activity for the app, replacing the former per-screen Activity model one screen
 * at a time -- see the single-activity + Navigation Compose rewrite scope. Phase 1 hosts only
 * the "repoList" route (formerly RepoListActivity); every other screen is still its own
 * Activity, started via Intent from within this NavHost's composables exactly as before.
 */
class MainActivity : SheimiFragmentActivity() {

    override fun getThemeResource(): Int {
        return if (Profile.getTheme(this) == 1) {
            R.style.DarkAppTheme_NoActionBar
        } else {
            R.style.AppTheme_NoActionBar
        }
    }

    private lateinit var cloneViewModel: CloneViewModel
    private lateinit var viewModel: RepoListViewModel
    private lateinit var navController: NavHostController

    /** Set just before navigating to "repoDetail"; read by that route's setup. Survives
     * recomposition (it's a plain field, not Compose state) and, since onSaveInstanceState below
     * persists it, an Activity recreate too -- needed because NavController's own back-stack
     * state *does* survive recreation (via its internal rememberSaveable), so without this, a
     * relaunch (e.g. a font-scale change, which isn't in this Activity's configChanges) would
     * land back on "repoDetail" with this null, bailing out to a blank screen instead of
     * properly restoring the repo that was actually showing. */
    private var pendingRepoForDetail: Repo? = null

    /** The live repoDetail screen's state/dispatch object, if that route is currently showing.
     * Fragments hosted inside it (Files/Commits/Status) reach this via
     * `(rawActivity as MainActivity).currentRepoDetailHost` instead of casting rawActivity
     * itself, since RepoDetailActivity is no longer an Activity type. */
    var currentRepoDetailHost: RepoDetailActivity? = null

    /** Set just before navigating to "branchChooser"; mirrors pendingRepoForDetail above. */
    private var pendingRepoForBranchChooser: Repo? = null

    /** The live branchChooser screen's ViewModel, if that route is currently showing -- read by
     * RenameBranchDialog (`(requireActivity() as MainActivity).currentBranchChooserViewModel`)
     * since BranchChooserActivity no longer exists as a type to cast to. */
    var currentBranchChooserViewModel: BranchChooserViewModel? = null

    /** Set just before navigating to "commitDiff"; mirrors pendingRepoForDetail above. */
    private var pendingCommitDiffArgs: CommitDiffArgs? = null

    private data class CommitDiffArgs(
        val oldCommit: String?,
        val newCommit: String,
        val showDescription: Boolean,
        val repo: Repo?
    ) : java.io.Serializable

    /** The live commitDiff screen's state/dispatch object, if that route is currently showing --
     * read by saveDiffLauncher's callback below since CommitDiffActivity no longer is a
     * ComponentActivity able to own that launcher itself. */
    private var currentCommitDiffHost: CommitDiffActivity? = null

    private val saveDiffLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val host = currentCommitDiffHost ?: return@registerForActivityResult
        if (result.resultCode == RESULT_OK && result.data != null) {
            val diffUri = result.data!!.data
            if (diffUri != null) {
                try {
                    contentResolver.openOutputStream(diffUri)?.use { host.saveDiff(it) }
                } catch (e: IOException) {
                    showToastMessage(R.string.alert_file_creation_failure)
                }
            }
        }
    }

    fun launchSaveDiff(intent: Intent) {
        saveDiffLauncher.launch(intent)
    }

    /** Set just before navigating to "exploreFile"; invoked with the picked file's path, then
     * cleared, by that route's host before popping back. */
    private var pendingExploreFileOnPicked: ((String) -> Unit)? = null

    /** The live privateKeyManage screen's state/dispatch object, if that route is currently
     * showing -- read by EditKeyPasswordDialog/RenameKeyDialog/PrivateKeyGenerate
     * (`(requireActivity() as MainActivity).currentPrivateKeyManageHost`) since
     * PrivateKeyManageActivity no longer is an Activity type to cast to. */
    var currentPrivateKeyManageHost: PrivateKeyManageActivity? = null

    private lateinit var settingsViewModel: SettingsViewModel

    /** Set just before navigating to "userSettings"; mirrors pendingRepoForDetail above. */
    private var pendingUserSettingsInitialScreen: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore navigation state lost to an Activity recreate (see pendingRepoForDetail's doc
        // comment) before the first setContent below composes anything -- must happen here, not
        // later, so the very first composition of e.g. "repoDetail" already sees a non-null repo
        // rather than bailing out once and only picking up the restored value on a second pass.
        if (savedInstanceState != null) {
            pendingRepoForDetail = savedInstanceState.getSerializable(KEY_PENDING_REPO_FOR_DETAIL) as? Repo
            pendingRepoForBranchChooser = savedInstanceState.getSerializable(KEY_PENDING_REPO_FOR_BRANCH_CHOOSER) as? Repo
            pendingCommitDiffArgs = savedInstanceState.getSerializable(KEY_PENDING_COMMIT_DIFF_ARGS) as? CommitDiffArgs
            pendingUserSettingsInitialScreen = savedInstanceState.getString(KEY_PENDING_USER_SETTINGS_INITIAL_SCREEN)
        }

        viewModel = ViewModelProvider(this)[RepoListViewModel::class.java]
        cloneViewModel = ViewModelProvider(this)[CloneViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        PrivateKeyUtils.migratePrivateKeys()
        initUpdatedSSL()

        setContent {
            navController = rememberNavController()

            // Picks up a repo staged by openRepoDetailSafely() (handleIntent/
            // handleWidgetOpenRepoIntent in onCreate, called before this composition was
            // guaranteed to exist) and navigates now that navController actually does. Runs
            // once per Activity instance; skipped if we're already showing repoDetail (e.g.
            // NavController's own back stack restored that route after a config-change
            // recreate) to avoid pushing a redundant duplicate entry.
            LaunchedEffect(Unit) {
                if (pendingRepoForDetail != null && navController.currentDestination?.route != "repoDetail") {
                    navController.navigate("repoDetail")
                }
            }

            NavHost(navController = navController, startDestination = "repoList") {
                composable("repoList") {
                    RepoListComposeContent(
                        activity = this@MainActivity,
                        cloneViewModel = cloneViewModel,
                        viewModel = viewModel,
                        onCloneClick = { cloneRepo() },
                        onCancelCloneViewClick = { hideCloneView() }
                    )
                }
                composable("repoDetail") {
                    val repo = pendingRepoForDetail
                    if (repo == null) {
                        // Got here without a pending repo (e.g. process death) -- bail to the
                        // list rather than crash.
                        navController.popBackStack()
                    } else {
                        val repoDetailViewModel = ViewModelProvider(this@MainActivity)[RepoDetailViewModel::class.java]
                        val host = remember(repo) {
                            RepoDetailActivity(this@MainActivity, repo, repoDetailViewModel).also {
                                currentRepoDetailHost = it
                            }
                        }
                        val filesFragment = remember(host) { FilesFragment.newInstance(host.repo) }
                        val commitsFragment = remember(host) { CommitsFragment.newInstance(host.repo, null) }
                        val statusFragment = remember(host) { StatusFragment.newInstance(host.repo) }

                        DisposableEffect(host) {
                            onDispose {
                                if (currentRepoDetailHost === host) {
                                    currentRepoDetailHost = null
                                }
                            }
                        }

                        AppTheme {
                            RepoDetailScreen(
                                viewModel = host.viewModel,
                                onBackClick = { navController.popBackStack() },
                                onBranchClick = { openBranchChooser(host.repo) },
                                onOperationClick = { index ->
                                    host.getRepoDelegate().executeAction(index)
                                },
                                filesContent = { FragmentHost(supportFragmentManager, filesFragment) },
                                commitsContent = { FragmentHost(supportFragmentManager, commitsFragment) },
                                statusContent = { FragmentHost(supportFragmentManager, statusFragment) },
                                onFilesSearchQueryChange = { query -> host.searchFiles(query) },
                                onCommitsSearchQueryChange = { query -> host.searchCommits(query) }
                            )
                        }
                    }
                }
                composable("branchChooser") {
                    val repo = pendingRepoForBranchChooser
                    if (repo == null) {
                        navController.popBackStack()
                    } else {
                        val branchChooserViewModel = ViewModelProvider(this@MainActivity)[BranchChooserViewModel::class.java]
                        remember(repo) {
                            branchChooserViewModel.setRepo(repo)
                            currentBranchChooserViewModel = branchChooserViewModel
                        }

                        DisposableEffect(branchChooserViewModel) {
                            onDispose {
                                if (currentBranchChooserViewModel === branchChooserViewModel) {
                                    currentBranchChooserViewModel = null
                                }
                            }
                        }

                        AppTheme {
                            BranchChooserScreen(
                                viewModel = branchChooserViewModel,
                                onBackClick = { navController.popBackStack() },
                                onBranchClick = { commitName -> checkoutBranch(repo, commitName) },
                                onRenameClick = { commitName -> showRenameBranchDialog(repo, commitName) },
                                onDeleteClick = { commitName -> showDeleteBranchDialog(repo, commitName) }
                            )
                        }
                    }
                }
                composable("commitDiff") {
                    val args = pendingCommitDiffArgs
                    if (args == null) {
                        navController.popBackStack()
                    } else {
                        val host = remember(args) {
                            CommitDiffActivity(
                                this@MainActivity, args.oldCommit, args.newCommit, args.showDescription, args.repo
                            ).also { currentCommitDiffHost = it }
                        }

                        DisposableEffect(host) {
                            onDispose {
                                if (currentCommitDiffHost === host) {
                                    currentCommitDiffHost = null
                                }
                            }
                        }

                        AppTheme {
                            CommitDiffScreen(
                                title = host.screenTitle,
                                isLoading = host.isLoading,
                                onBackClick = { navController.popBackStack() },
                                onShareClick = { host.shareDiff() },
                                onSaveClick = { host.launchSaveDiff() },
                                onWebViewCreated = { host.setupWebView(it) }
                            )
                        }
                    }
                }
                composable("exploreFile") {
                    val onPicked = pendingExploreFileOnPicked
                    if (onPicked == null) {
                        navController.popBackStack()
                    } else {
                        val host = remember(onPicked) {
                            ExploreFileActivity(this@MainActivity) { path ->
                                onPicked(path)
                                navController.popBackStack()
                            }
                        }

                        // Cleared on dispose rather than inline in the click callback above --
                        // clearing it there let a stale recomposition (mid pop-transition) read
                        // pendingExploreFileOnPicked as null and call popBackStack() a second
                        // time, which corrupted NavHost's back stack state badly enough to blank
                        // the whole screen with no exception logged anywhere.
                        DisposableEffect(host) {
                            onDispose { pendingExploreFileOnPicked = null }
                        }

                        BackHandler {
                            if (!host.goUpOrFalse()) navController.popBackStack()
                        }

                        AppTheme {
                            Box {
                                FileExplorerScreen(
                                    title = host.screenTitle,
                                    currentPath = host.currentDir.path,
                                    files = host.filesState,
                                    showUpRow = !host.isAtRoot,
                                    onUpClick = { host.goUpOrFalse() },
                                    onBackClick = { navController.popBackStack() },
                                    onItemClick = { host.onFileClick(it) },
                                    onItemLongClick = { host.onFileLongClick(it) },
                                    selectedFile = host.selectedFile
                                )
                                host.Overlay()
                            }
                        }
                    }
                }
                composable("privateKeyManage") {
                    val host = remember {
                        PrivateKeyManageActivity(this@MainActivity).also { currentPrivateKeyManageHost = it }
                    }

                    DisposableEffect(host) {
                        onDispose {
                            if (currentPrivateKeyManageHost === host) {
                                currentPrivateKeyManageHost = null
                            }
                        }
                    }

                    BackHandler {
                        if (!host.goUpOrFalse()) navController.popBackStack()
                    }

                    AppTheme {
                        Box {
                            FileExplorerScreen(
                                title = host.screenTitle,
                                currentPath = host.currentDir.path,
                                files = host.filesState,
                                showUpRow = !host.isAtRoot,
                                onUpClick = { host.goUpOrFalse() },
                                onBackClick = { navController.popBackStack() },
                                onItemClick = { host.onFileClick(it) },
                                onItemLongClick = { host.onFileLongClick(it) },
                                selectedFile = host.selectedFile,
                                actions = { host.TopBarActions() }
                            )
                            host.Overlay()
                        }
                    }
                }
                composable("userSettings") {
                    var currentScreen by remember { mutableStateOf(pendingUserSettingsInitialScreen ?: "settings") }
                    val useDynamicColor by settingsViewModel.useDynamicColor.observeAsState(false)
                    val fontOption by settingsViewModel.fontOption.observeAsState(FontOption.DEFAULT)

                    AppTheme(useDynamicColor = useDynamicColor, fontOption = fontOption) {
                        Crossfade(targetState = currentScreen) { screen ->
                            when (screen) {
                                "settings" -> SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { navController.popBackStack() },
                                    onManageAccountsClick = { currentScreen = "accounts" },
                                    onManageSshKeysClick = { openPrivateKeyManage() },
                                    onFeedbackClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.feedback_url)))
                                        startActivity(intent)
                                    },
                                    onViewReleaseClick = { url ->
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    },
                                    onAddWidgetClick = { requestPinRepoListWidget() }
                                )
                                "accounts" -> AccountsScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { currentScreen = "settings" }
                                )
                            }
                        }
                    }
                }
            }
        }

        handleIntent(intent)
        handleWidgetOpenRepoIntent(intent)
    }

    fun openRepoDetail(repo: Repo) {
        pendingRepoForDetail = repo
        navController.navigate("repoDetail")
    }

    /** Like openRepoDetail(), but safe to call from onCreate's tail (handleIntent/
     * handleWidgetOpenRepoIntent below) where setContent's first composition -- and thus
     * navController -- isn't guaranteed to have run yet (confirmed by a production crash:
     * UninitializedPropertyAccessException on navController from a cold-started widget tap).
     * Falls back to just staging pendingRepoForDetail; the NavHost's own LaunchedEffect picks
     * it up and navigates once the composition is actually ready. */
    private fun openRepoDetailSafely(repo: Repo) {
        if (::navController.isInitialized) {
            openRepoDetail(repo)
        } else {
            pendingRepoForDetail = repo
        }
    }

    private fun openBranchChooser(repo: Repo) {
        pendingRepoForBranchChooser = repo
        navController.navigate("branchChooser")
    }

    fun openCommitDiff(oldCommit: String?, newCommit: String, showDescription: Boolean, repo: Repo?) {
        pendingCommitDiffArgs = CommitDiffArgs(oldCommit, newCommit, showDescription, repo)
        navController.navigate("commitDiff")
    }

    fun openExploreFile(onPicked: (String) -> Unit) {
        pendingExploreFileOnPicked = onPicked
        navController.navigate("exploreFile")
    }

    fun openPrivateKeyManage() {
        navController.navigate("privateKeyManage")
    }

    fun openUserSettings(initialScreen: String = "settings") {
        pendingUserSettingsInitialScreen = initialScreen
        navController.navigate("userSettings")
    }

    private fun requestPinRepoListWidget() {
        val appWidgetManager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, com.manichord.mgit.widget.RepoListWidgetReceiver::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        } else {
            showToastMessage(R.string.widget_pin_not_supported)
        }
    }

    private fun checkoutBranch(repo: Repo, commitName: String) {
        val checkoutTask = CheckoutTask(repo, commitName, null, object : SheimiAsyncTask.AsyncTaskPostCallback {
            override fun onPostExecute(isSuccess: Boolean?) {
                currentRepoDetailHost?.reset(commitName)
                navController.popBackStack()
            }
        })
        checkoutTask.executeTask()
    }

    private fun showRenameBranchDialog(repo: Repo, commitName: String) {
        val pathArg = Bundle()
        pathArg.putString(RenameBranchDialog.FROM_COMMIT, commitName)
        pathArg.putSerializable(Repo.TAG, repo)
        val rbd = RenameBranchDialog()
        rbd.arguments = pathArg
        rbd.show(supportFragmentManager, "rename-dialog")
    }

    private fun showDeleteBranchDialog(repo: Repo, commitName: String) {
        MessageDialog.show(
            findViewById(android.R.id.content),
            getString(R.string.dialog_branch_delete) + " " + commitName,
            getString(R.string.dialog_branch_delete_msg),
            getString(R.string.label_delete),
            getString(R.string.label_cancel),
            DialogInterface.OnClickListener { _, _ -> deleteBranch(repo, commitName) },
            DialogInterface.OnClickListener { _, _ -> }
        )
    }

    private fun deleteBranch(repo: Repo, commitName: String) {
        val commitType = Repo.getCommitType(commitName)
        try {
            when (commitType) {
                Repo.COMMIT_TYPE_HEAD -> {
                    repo.git?.branchDelete()?.setBranchNames(commitName)?.setForce(true)?.call()
                }
                Repo.COMMIT_TYPE_TAG -> {
                    repo.git?.tagDelete()?.setTags(commitName)?.call()
                }
            }
            currentBranchChooserViewModel?.refreshList()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this, getString(R.string.cannot_delete_branch, commitName), android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        handleWidgetOpenRepoIntent(intent)
    }

    private fun handleWidgetOpenRepoIntent(intent: Intent?) {
        val repoId = intent?.getIntExtra(EXTRA_OPEN_REPO_ID, -1) ?: -1
        if (repoId == -1) return
        // RepoDbManager.getRepoById returns null (not an empty cursor) when nothing matches --
        // a real possibility here specifically, since the widget's displayed repo list can be
        // stale relative to the current DB (e.g. the user deleted that repo in-app since the
        // widget last refreshed). Repo.getRepoList()/Repo.getRepoById() both assume a non-null
        // cursor and NPE otherwise, so check first rather than feeding it through.
        val cursor = RepoDbManager.getRepoById(repoId.toLong()) ?: return
        val repo = Repo.getRepoList(this, cursor).firstOrNull()
        if (repo != null) {
            openRepoDetailSafely(repo)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        try {
            val remoteRepoUrl = URL(uri.scheme, uri.host, uri.port ?: -1, uri.path)
            val remoteUrl = remoteRepoUrl.toString()
            var repoName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1)
            val repoUrlBuilder = StringBuilder(remoteUrl)

            if (!remoteUrl.lowercase().endsWith(getString(R.string.git_extension))) {
                repoUrlBuilder.append(getString(R.string.git_extension))
            } else {
                repoName = repoName.substring(0, repoName.lastIndexOf('.'))
            }

            val repositoriesWithSameRemote = Repo.getRepoList(this, RepoDbManager.searchRepo(remoteUrl))

            if (repositoriesWithSameRemote.isNotEmpty()) {
                showToastMessage(R.string.repository_already_present)
                openRepoDetailSafely(repositoriesWithSameRemote[0])
            } else if (Repo.getDir((applicationContext as MGitApplication).prefenceHelper, repoName).exists()) {
                cloneViewModel.remoteUrl = repoUrlBuilder.toString()
                showCloneView()
            } else {
                val cloningStatus = getString(R.string.cloning)
                val mRepo = Repo.createRepo(repoName, repoUrlBuilder.toString(), cloningStatus)
                CloneTask(mRepo, true, cloningStatus, null).executeTask()
            }
        } catch (e: MalformedURLException) {
            showToastMessage(R.string.invalid_url)
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts in case the user completed GitHub OAuth in the browser. (CloneViewModel
        // doesn't need an equivalent call here -- it observes AccountManager.accountsChanged
        // directly, which fires the moment an account is actually saved rather than depending on
        // Activity resume timing.)
        settingsViewModel.refreshAccounts()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // See pendingRepoForDetail's doc comment -- restored in onCreate above.
        outState.putSerializable(KEY_PENDING_REPO_FOR_DETAIL, pendingRepoForDetail)
        outState.putSerializable(KEY_PENDING_REPO_FOR_BRANCH_CHOOSER, pendingRepoForBranchChooser)
        outState.putSerializable(KEY_PENDING_COMMIT_DIFF_ARGS, pendingCommitDiffArgs)
        outState.putString(KEY_PENDING_USER_SETTINGS_INITIAL_SCREEN, pendingUserSettingsInitialScreen)
    }

    companion object {
        private const val KEY_PENDING_REPO_FOR_DETAIL = "pending_repo_for_detail"
        private const val KEY_PENDING_REPO_FOR_BRANCH_CHOOSER = "pending_repo_for_branch_chooser"
        private const val KEY_PENDING_COMMIT_DIFF_ARGS = "pending_commit_diff_args"
        private const val KEY_PENDING_USER_SETTINGS_INITIAL_SCREEN = "pending_user_settings_initial_screen"

        /** Set by the home-screen widget's "open repo" tap action (see widget/RepoWidgetActions.kt)
         * to route straight to that repo's detail screen instead of the repo list. */
        const val EXTRA_OPEN_REPO_ID = "com.manichord.mgit.OPEN_REPO_ID"
    }

    private fun initUpdatedSSL() {
        MGitHttpConnectionFactory.install()
        Timber.i("Installed custom HTTPS factory")
    }

    private fun cloneRepo() {
        if (cloneViewModel.validate()) {
            hideCloneView()
            cloneViewModel.cloneRepo()
        }
    }

    private fun showCloneView() {
        cloneViewModel.show(true)
    }

    private fun hideCloneView() {
        cloneViewModel.show(false)
    }
}
