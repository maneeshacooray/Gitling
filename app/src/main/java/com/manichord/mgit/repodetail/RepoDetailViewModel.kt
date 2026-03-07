package com.manichord.mgit.repodetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
}
