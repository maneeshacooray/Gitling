package com.manichord.mgit.repodetail

import kotlinx.coroutines.Job
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.BranchTrackingStatus
import timber.log.Timber

class RepoDetailViewModel : ViewModel() {

    private val _repo = MutableLiveData<Repo>()
    val repo: LiveData<Repo> = _repo

    private val _selectedTab = MutableLiveData(0)
    val selectedTab: LiveData<Int> = _selectedTab

    private val _isDrawerOpen = MutableLiveData(false)
    val isDrawerOpen: LiveData<Boolean> = _isDrawerOpen

    fun setRepo(repo: Repo) {
        if (_repo.value?.id != repo.id) _syncCounts.value = null
        _repo.value = repo
        _consoleEntries.value = consoleEntriesByRepo[repo.id].orEmpty()
        _consoleRunning.value = repo.id in consoleRunningRepos
    }

    /**
     * What the toolbar's buttons have waiting: commits on the remote branch not pulled yet
     * ([behind]), local commits not pushed yet ([ahead]), and changed files ([changes]).
     * [behind] and [ahead] compare with the remote branch as of the last fetch or pull (like
     * `git status`), and are null when the branch has no remote branch to compare with.
     */
    data class SyncCounts(val behind: Int?, val ahead: Int?, val changes: Int)

    private val _syncCounts = MutableLiveData<SyncCounts?>(null)
    val syncCounts: LiveData<SyncCounts?> = _syncCounts

    // Counting walks the whole working tree (git status): about 2 s for a 20,000-file repo. All
    // of the bookkeeping below is only touched on the main thread.
    private var countJob: Job? = null
    private var recountAfter: Repo? = null
    private var lastCountedRepoId = -1
    private var lastCountedAt = 0L
    private var countStartedAt = 0L
    private var countsStale = false

    /**
     * Recounts [syncCounts] off the main thread. Call after anything that can change them (git
     * operations, file edits). With [onlyIfStale] (coming back to the screen), a count of the
     * same repo from the last minute is kept, unless [markSyncCountsStale] was called since:
     * switching apps rarely changes anything, and recounting a big repo on every return is
     * wasted disk work. Never runs two counts at once: a request during a count runs once it's
     * done, so the result is still current.
     */
    @JvmOverloads
    fun refreshSyncCounts(repo: Repo, onlyIfStale: Boolean = false) {
        if (onlyIfStale && !countsStale && repo.id == lastCountedRepoId &&
            SystemClock.elapsedRealtime() - lastCountedAt < RECOUNT_AFTER_MS
        ) return
        if (countJob?.isActive == true) {
            // A request right after the count started is already covered by it (e.g. the
            // screen's refresh and the finished task both asking after the same operation);
            // a later one may follow a change the running count has already walked past
            if (SystemClock.elapsedRealtime() - countStartedAt > ALREADY_COVERED_MS) recountAfter = repo
            return
        }
        countsStale = false
        countStartedAt = SystemClock.elapsedRealtime()
        val repoId = repo.id
        countJob = viewModelScope.launch {
            val counts = withContext(Dispatchers.IO) { countSync(repo) }
            lastCountedRepoId = repoId
            lastCountedAt = SystemClock.elapsedRealtime()
            // Only if the user is still on the repo that was counted
            if (_repo.value?.id == repoId) _syncCounts.value = counts
            recountAfter?.let {
                recountAfter = null
                refreshSyncCounts(it)
            }
        }
    }

    /** Something may have changed files without a git operation (e.g. a file opened for
     * editing): the next return to the screen recounts even within the minute. */
    fun markSyncCountsStale() {
        countsStale = true
    }

    private fun countSync(repo: Repo): SyncCounts? = try {
        // Its own Git instance, not repo.git: that one's Repository is shared with the tasks
        // and screens running at the same time, and reading its config from here while they use
        // it sometimes came back without the branch's upstream
        Git.open(repo.dir).use { git ->
            val repository = git.repository
            val tracking = repository.branch?.let { BranchTrackingStatus.of(repository, it) }
            val status = git.status().call()
            SyncCounts(
                behind = tracking?.behindCount,
                ahead = tracking?.aheadCount,
                changes = (status.uncommittedChanges + status.untracked).size
            )
        }
    } catch (e: Exception) {
        Timber.w(e, "Couldn't count sync state")
        null
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setDrawerOpen(open: Boolean) {
        _isDrawerOpen.value = open
    }

    // Progress state for pull/push operations
    data class ProgressState(
        val message: String,
        val leftHint: String,
        val rightHint: String,
        val progress: Int,
        val visible: Boolean = false
    )

    private val _progressState = MutableLiveData(ProgressState("", "", "", 0, false))
    val progressState: LiveData<ProgressState> = _progressState

    fun updateProgress(message: String, leftHint: String, rightHint: String, progress: Int) {
        _progressState.value = ProgressState(message, leftHint, rightHint, progress, true)
    }

    fun hideProgress() {
        _progressState.value = _progressState.value?.copy(visible = false)
    }

    // Console state. Kept per repo: this ViewModel is activity-scoped (MainActivity) and shared
    // by every repo opened, so a single list showed one repo's console output in another's.
    // consoleEntries/consoleRunning always reflect the current repo.
    data class ConsoleEntry(val command: String, val output: String)

    private val consoleEntriesByRepo = mutableMapOf<Int, List<ConsoleEntry>>()
    private val consoleRunningRepos = mutableSetOf<Int>()

    private val _consoleEntries = MutableLiveData<List<ConsoleEntry>>(emptyList())
    val consoleEntries: LiveData<List<ConsoleEntry>> = _consoleEntries

    private val _consoleRunning = MutableLiveData(false)
    val consoleRunning: LiveData<Boolean> = _consoleRunning

    fun runConsoleCommand(repo: Repo, command: String) {
        val repoId = repo.id
        if (repoId in consoleRunningRepos) return
        consoleRunningRepos += repoId
        if (_repo.value?.id == repoId) _consoleRunning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val output = GitCommandEngine.execute(repo, command)
            withContext(Dispatchers.Main) {
                // The user may have switched repos while the command ran: file the output under
                // the repo that ran it, and only show it if that repo is still the current one
                val entries = consoleEntriesByRepo[repoId].orEmpty() + ConsoleEntry(command, output)
                consoleEntriesByRepo[repoId] = entries
                consoleRunningRepos -= repoId
                if (_repo.value?.id == repoId) {
                    _consoleEntries.value = entries
                    _consoleRunning.value = false
                }
            }
        }
    }

    fun clearConsole() {
        _repo.value?.let { consoleEntriesByRepo.remove(it.id) }
        _consoleEntries.value = emptyList()
    }
}

/** How long a count stays good enough to skip recounting when coming back to the screen. */
private const val RECOUNT_AFTER_MS = 60_000L

/** How soon after a count starts a new request still counts as asking for the same state. */
private const val ALREADY_COVERED_MS = 500L
