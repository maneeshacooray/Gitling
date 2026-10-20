package com.manichord.mgit.repolist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.database.RepoContract
import me.sheimi.sgit.database.RepoDbManager
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.preference.PreferenceHelper

class RepoListViewModel(application: Application) : AndroidViewModel(application), RepoDbManager.RepoDbObserver {

    private val _repoList = MutableStateFlow<List<Repo>>(emptyList())
    val repoList: StateFlow<List<Repo>> = _repoList.asStateFlow()

    private val _showStorageMigrationNotice = MutableStateFlow(false)
    val showStorageMigrationNotice: StateFlow<Boolean> = _showStorageMigrationNotice.asStateFlow()

    init {
        // Must run before refreshRepoList() so the first load reflects any converted paths, and
        // can't run any earlier than this (e.g. in MGitApplication.onCreate()) -- RepoDbManager
        // needs BasicFunctions.getActiveActivity(), which isn't set until an Activity's onCreate
        // actually starts running, well after Application.onCreate() returns.
        if (Repo.migrateAwayFromCustomRoot(getApplication())) {
            Profile.setPendingStorageMigrationNotice(getApplication(), true)
        }
        // Covers users who already had "Make repos visible to other apps" on before the .nomedia
        // marker existed, so they get it without having to toggle the setting.
        val sharedMedia = PreferenceHelper(getApplication()).useSharedMediaStorage()
        val app = getApplication<Application>()
        Thread {
            Repo.syncNoMediaMarker(sharedMedia)
            // Fixes repos moved between storage roots by older versions (see
            // Repo.ignoreFileModeAndSymlinks)
            if (!Profile.getFileModeMigrationDone(app)) {
                Repo.ignoreFileModeInAllRepos(app)
                Profile.setFileModeMigrationDone(app)
            }
        }.start()
        RepoDbManager.registerDbObserver(RepoContract.RepoEntry.TABLE_NAME, this)
        refreshRepoList()
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

    override fun nofityChanged() {
        refreshRepoList()
    }

    override fun onCleared() {
        super.onCleared()
        RepoDbManager.unregisterDbObserver(RepoContract.RepoEntry.TABLE_NAME, this)
    }
}
