package com.manichord.mgit.branchchooser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.sheimi.sgit.database.models.Repo
import kotlinx.coroutines.launch

class BranchChooserViewModel : ViewModel() {

    private val _repo = MutableLiveData<Repo>()
    val repo: LiveData<Repo> = _repo

    private val _branches = MutableLiveData<List<BranchItem>>(emptyList())
    val branches: LiveData<List<BranchItem>> = _branches

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _filter = MutableLiveData(BranchFilter.ALL)
    val filter: LiveData<BranchFilter> = _filter

    enum class BranchFilter { ALL, LOCAL, REMOTE, TAGS }

    data class BranchItem(
        val name: String,
        val displayName: String,
        val type: Int // Repo.COMMIT_TYPE_*
    )

    fun setRepo(repo: Repo) {
        _repo.value = repo
        refreshList()
    }

    fun setFilter(filter: BranchFilter) {
        _filter.value = filter
        refreshList()
    }

    fun refreshList() {
        val repo = _repo.value ?: return
        _isLoading.value = true

        viewModelScope.launch {
            val allBranches = repo.branches ?: emptyArray()
            val allTags = repo.tags ?: emptyArray()

            val items = mutableListOf<BranchItem>()

            val currentFilter = _filter.value ?: BranchFilter.ALL

            if (currentFilter == BranchFilter.ALL || currentFilter == BranchFilter.LOCAL || currentFilter == BranchFilter.REMOTE) {
                allBranches.forEach { name ->
                    val type = Repo.getCommitType(name)
                    if (currentFilter == BranchFilter.ALL ||
                        (currentFilter == BranchFilter.LOCAL && type == Repo.COMMIT_TYPE_HEAD) ||
                        (currentFilter == BranchFilter.REMOTE && type == Repo.COMMIT_TYPE_REMOTE)) {
                        items.add(BranchItem(name, Repo.getCommitDisplayName(name), type))
                    }
                }
            }

            if (currentFilter == BranchFilter.ALL || currentFilter == BranchFilter.TAGS) {
                allTags.forEach { name ->
                    items.add(BranchItem(name, Repo.getCommitDisplayName(name), Repo.COMMIT_TYPE_TAG))
                }
            }

            _branches.postValue(items)
            _isLoading.value = false
        }
    }
}
