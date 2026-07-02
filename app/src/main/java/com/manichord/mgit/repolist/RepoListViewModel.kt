package com.manichord.mgit.repolist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manichord.mgit.update.UpdateCheckResult
import com.manichord.mgit.update.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.BuildConfig
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo

class RepoListViewModel(application: Application) : AndroidViewModel(application), RepoDbManager.RepoDbObserver {

    companion object {
        // GitHub's API rate limit for unauthenticated requests is generous (60/hr per IP) but
        // there's no reason to check more than once a day -- new releases don't ship that often.
        private const val UPDATE_CHECK_COOLDOWN_MILLIS = 24L * 60 * 60 * 1000
    }

    private val _repoList = MutableStateFlow<List<Repo>>(emptyList())
    val repoList: StateFlow<List<Repo>> = _repoList.asStateFlow()

    private val _updateAvailable = MutableStateFlow<UpdateCheckResult.UpdateAvailable?>(null)
    val updateAvailable: StateFlow<UpdateCheckResult.UpdateAvailable?> = _updateAvailable.asStateFlow()

    private val _showStorageMigrationNotice = MutableStateFlow(false)
    val showStorageMigrationNotice: StateFlow<Boolean> = _showStorageMigrationNotice.asStateFlow()

    private val updateChecker = UpdateChecker()

    init {
        // Must run before refreshRepoList() so the first load reflects any converted paths, and
        // can't run any earlier than this (e.g. in MGitApplication.onCreate()) -- RepoDbManager
        // needs BasicFunctions.getActiveActivity(), which isn't set until an Activity's onCreate
        // actually starts running, well after Application.onCreate() returns.
        if (Repo.migrateAwayFromCustomRoot(getApplication())) {
            Profile.setPendingStorageMigrationNotice(getApplication(), true)
        }
        RepoDbManager.registerDbObserver(RepoContract.RepoEntry.TABLE_NAME, this)
        refreshRepoList()
        maybeCheckForUpdate()
        _showStorageMigrationNotice.value = Profile.getPendingStorageMigrationNotice(getApplication())
    }

    fun dismissStorageMigrationNotice() {
        Profile.setPendingStorageMigrationNotice(getApplication(), false)
        _showStorageMigrationNotice.value = false
    }

    fun refreshRepoList() {
        viewModelScope.launch {
            val cursor = RepoDbManager.queryAllRepo()
            val repos = Repo.getRepoList(getApplication(), cursor)
            repos.sortWith(compareByDescending<Repo> { it.isPinned() }.thenByDescending { it.getID() })
            cursor.close()
            _repoList.value = repos
        }
    }

    private fun maybeCheckForUpdate() {
        val context = getApplication<Application>()
        val sinceLastCheck = System.currentTimeMillis() - Profile.getLastUpdateCheckTime(context)
        if (sinceLastCheck < UPDATE_CHECK_COOLDOWN_MILLIS) return

        updateChecker.checkForUpdate(BuildConfig.VERSION_NAME) { result ->
            Profile.setLastUpdateCheckTime(context, System.currentTimeMillis())
            if (result is UpdateCheckResult.UpdateAvailable &&
                result.versionName != Profile.getDismissedUpdateVersion(context)
            ) {
                _updateAvailable.value = result
            }
        }
    }

    fun dismissUpdateAvailable() {
        _updateAvailable.value?.let { Profile.setDismissedUpdateVersion(getApplication(), it.versionName) }
        _updateAvailable.value = null
    }

    override fun nofityChanged() {
        refreshRepoList()
    }

    override fun onCleared() {
        super.onCleared()
        RepoDbManager.unregisterDbObserver(RepoContract.RepoEntry.TABLE_NAME, this)
    }
}
