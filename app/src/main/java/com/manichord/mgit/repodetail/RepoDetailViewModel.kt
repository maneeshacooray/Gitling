package com.manichord.mgit.repodetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sheimi.sgit.database.models.Repo

class RepoDetailViewModel : ViewModel() {

    private val _repo = MutableLiveData<Repo>()
    val repo: LiveData<Repo> = _repo

    private val _selectedTab = MutableLiveData(0)
    val selectedTab: LiveData<Int> = _selectedTab

    private val _isDrawerOpen = MutableLiveData(false)
    val isDrawerOpen: LiveData<Boolean> = _isDrawerOpen

    fun setRepo(repo: Repo) {
        _repo.value = repo
        _consoleEntries.value = consoleEntriesByRepo[repo.id].orEmpty()
        _consoleRunning.value = repo.id in consoleRunningRepos
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
