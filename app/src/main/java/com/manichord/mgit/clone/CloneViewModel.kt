package com.manichord.mgit.clone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.repo.tasks.repo.CloneTask
import me.sheimi.sgit.repo.tasks.repo.InitLocalTask
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CloneViewModel(application: Application) : AndroidViewModel(application) {

    var remoteUrl : String = ""
        set(value) {
            field = value
            localRepoName.value = stripGitExtension(stripUrlFromRepo(remoteUrl))
        }

    val localRepoName : MutableLiveData<String> = MutableLiveData()
    val cloneLocation : MutableLiveData<String> = MutableLiveData()
    var cloneRecursively : Boolean = false
    val initLocal : MutableLiveData<Boolean> = MutableLiveData()

    var remoteUrlError : MutableLiveData<String?> = MutableLiveData()
    var localRepoNameError : MutableLiveData<String?> = MutableLiveData()

    val visible : MutableLiveData<Boolean> = MutableLiveData()

    private val accountManager = (application as MGitApplication).accountManager!!
    private val githubAuthManager = (application as MGitApplication).githubAuthManager!!

    val accounts: MutableLiveData<List<com.manichord.mgit.models.Account>> = MutableLiveData(emptyList())
    val selectedAccount: MutableLiveData<com.manichord.mgit.models.Account?> = MutableLiveData(null)

    private val _githubRepos = kotlinx.coroutines.flow.MutableStateFlow<List<com.manichord.mgit.models.GitHubRepo>>(emptyList())
    val githubRepos = _githubRepos.asStateFlow()

    private val _isLoadingRepos = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoadingRepos = _isLoadingRepos.asStateFlow()

    // observeForever since this ViewModel isn't a LifecycleOwner -- removed in onCleared().
    // Without this, a GitHub account connected via the OAuth Device Flow (completed on a
    // background polling thread, see GitHubAuthManager) wouldn't show up here until something
    // else happened to call refreshAccounts(), which an Activity-lifecycle-timed refresh can
    // easily miss if the background poll lands just after the user switches back to the app.
    private val accountsChangedObserver = androidx.lifecycle.Observer<Unit> { refreshAccounts() }

    init {
        visible.value = false
        initLocal.value = false
        refreshCloneLocation()
        refreshAccounts()
        accountManager.accountsChanged.observeForever(accountsChangedObserver)
    }

    /** Display-only -- repos always clone into the current default root (no folder picker; see
     * CloneView), so this just shows the user where that currently is. Must be re-read every
     * time the clone sheet opens, not just once at ViewModel construction (this ViewModel
     * outlives any single sheet open/close, and the default root can change via the "make repos
     * visible to other apps" Settings toggle without the app restarting in between). */
    fun refreshCloneLocation() {
        cloneLocation.value = Repo.getDefaultRepoRootDir().absolutePath
    }

    override fun onCleared() {
        super.onCleared()
        accountManager.accountsChanged.removeObserver(accountsChangedObserver)
    }

    fun selectAccount(account: com.manichord.mgit.models.Account?) {
        selectedAccount.value = account
        if (account?.type == com.manichord.mgit.models.AccountType.GITHUB) {
            fetchGitHubRepos(account.token)
        } else {
            _githubRepos.value = emptyList()
        }
    }

    private fun fetchGitHubRepos(token: String) {
        _isLoadingRepos.value = true
        githubAuthManager.fetchRepos(token) { reposJson ->
            val repos = reposJson.map { json ->
                com.manichord.mgit.models.GitHubRepo(
                    name = json.getString("name"),
                    fullName = json.getString("full_name"),
                    description = json.optString("description", null),
                    cloneUrl = json.getString("clone_url"),
                    stars = json.optInt("stargazers_count", 0),
                    language = json.optString("language", null)
                )
            }
            _githubRepos.value = repos
            _isLoadingRepos.value = false
        }
    }

    fun refreshAccounts() {
        accounts.value = accountManager.getAccounts()
    }

    fun show(show : Boolean) {
        if (show) {
            refreshAccounts()
            refreshCloneLocation()
        }
        visible.value = show
    }


    fun cloneRepo() {
        if (initLocal.value ?: false) {
            Timber.d("INIT LOCAL %s", localRepoName.value)
            initLocalRepo()
        } else {
            Timber.d("CLONE REPO %s %s [%b]", localRepoName.value, remoteUrl, cloneRecursively)
            val repoName = localRepoName.value ?: ""
            val repo = Repo.createRepo(repoName, remoteUrl, "")

            selectedAccount.value?.let { account ->
                repo.username = account.username
                repo.password = account.token
                repo.saveCredentials()
            }

            val task = CloneTask(repo, cloneRecursively, "", null)
            task.executeTask()
            remoteUrl = ""
            show(false)
        }
    }

    fun validate() : Boolean {
        val localName = localRepoName.value ?: ""
        return if (initLocal.value ?: false) {
            validateLocalName(localName)
        } else validateRemoteUrl(remoteUrl) && validateLocalName(localName)
    }

    fun initLocalRepo() {
        val repo = Repo.createRepo(localRepoName.value, "local repository", "")
        val task = InitLocalTask(repo)
        task.executeTask()
    }

    private fun stripUrlFromRepo(remoteUrl: String): String {
        val lastSlash = remoteUrl.lastIndexOf("/")
        return if (lastSlash != -1) {
            remoteUrl.substring(lastSlash + 1)
        } else remoteUrl

    }

    private fun stripGitExtension(remoteUrl: String): String {
        val extension = remoteUrl.indexOf(".git")
        return if (extension != -1) {
            remoteUrl.substring(0, extension)
        } else remoteUrl

    }


    private fun validateRemoteUrl(remoteUrl: String): Boolean {
        remoteUrlError.value = null
        if (remoteUrl.isBlank()) {
            remoteUrlError.value = getApplication<MGitApplication>().getString(R.string.alert_remoteurl_required)
            return false
        }
        return true
    }

    private fun validateLocalName(localName: String): Boolean {
        localRepoNameError.value = null
        if (localName.isBlank()) {
           localRepoNameError.value = getApplication<MGitApplication>().getString((R.string.alert_localpath_required))
           return false
        }
        if (localName.contains("/")) {
            localRepoNameError.value = getApplication<MGitApplication>().getString((R.string.alert_localpath_format))
            return false
        }

        val prefsHelper = (getApplication<MGitApplication>()).prefenceHelper
        val file = Repo.getDir(prefsHelper, localName)
        if (file.exists()) {
            localRepoNameError.value = getApplication<MGitApplication>().getString((R.string.alert_localpath_repo_exists))
            return false
        }
        return true
    }
}
