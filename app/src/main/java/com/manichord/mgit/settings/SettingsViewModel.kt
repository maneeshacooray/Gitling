package com.manichord.mgit.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.manichord.mgit.auth.DeviceFlowStatus
import com.manichord.mgit.ui.theme.FontOption
import com.manichord.mgit.update.UpdateCheckResult
import com.manichord.mgit.update.UpdateChecker
import me.sheimi.android.utils.Profile
import me.sheimi.sgit.BuildConfig
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.database.models.Repo
import me.sheimi.sgit.preference.PreferenceHelper

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsHelper: PreferenceHelper = (application as MGitApplication).prefenceHelper!!

    // Informational display of the current default root -- there's no custom-location picker
    // (see CloneView/RepoListScreen), but it does change when useSharedMediaStorage is toggled.
    private val _repoRoot = MutableLiveData(Repo.getDefaultRepoRootDir().absolutePath)
    val repoRoot: LiveData<String> = _repoRoot

    private val _useSharedMediaStorage = MutableLiveData(prefsHelper.useSharedMediaStorage())
    val useSharedMediaStorage: LiveData<Boolean> = _useSharedMediaStorage

    private val _movingRepoStorage = MutableLiveData(false)
    val movingRepoStorage: LiveData<Boolean> = _movingRepoStorage

    /** Moves every bare-named repo's directory to the other default root, then flips the
     * preference -- both roots are always accessible to Gitling regardless of the current
     * setting (see Repo.getDefaultRepoRootDir()), so this is a real move, not a "some repos
     * become unreachable" migration. Runs off the main thread since it's real disk I/O
     * proportional to repo size/count. */
    fun setUseSharedMediaStorage(use: Boolean) {
        val app = getApplication<MGitApplication>()
        val oldRoot = Repo.getDefaultRepoRootDir()
        _movingRepoStorage.value = true
        Thread {
            prefsHelper.setUseSharedMediaStorage(use)
            val newRoot = Repo.getDefaultRepoRootDir()
            Repo.moveReposBetweenDefaultRoots(app, oldRoot, newRoot)
            _movingRepoStorage.postValue(false)
            _useSharedMediaStorage.postValue(use)
            _repoRoot.postValue(newRoot.absolutePath)
        }.start()
    }

    private val _useEnglish = MutableLiveData(prefsHelper.useEnglish())
    val useEnglish: LiveData<Boolean> = _useEnglish

    private val _gitUserName = MutableLiveData(prefsHelper.getUserName() ?: "")
    val gitUserName: LiveData<String> = _gitUserName

    private val _gitUserEmail = MutableLiveData(prefsHelper.getUserEmail() ?: "")
    val gitUserEmail: LiveData<String> = _gitUserEmail

    private val _useGravatar = MutableLiveData(prefsHelper.useGravatar())
    val useGravatar: LiveData<Boolean> = _useGravatar

    private val _useDynamicColor = MutableLiveData(prefsHelper.useDynamicColor())
    val useDynamicColor: LiveData<Boolean> = _useDynamicColor

    private val _fontOption = MutableLiveData(FontOption.fromId(prefsHelper.getAppFont()))
    val fontOption: LiveData<FontOption> = _fontOption

    private val accountManager = (application as MGitApplication).accountManager!!
    private val githubAuthManager = (application as MGitApplication).githubAuthManager!!

    private val _accounts = MutableLiveData(accountManager.getAccounts())
    val accounts: LiveData<List<com.manichord.mgit.models.Account>> = _accounts

    private val _githubAuthInProgress = MutableLiveData(false)
    val githubAuthInProgress: LiveData<Boolean> = _githubAuthInProgress

    private val _githubAuthMessage = MutableLiveData<String?>(null)
    val githubAuthMessage: LiveData<String?> = _githubAuthMessage

    fun refreshAccounts() {
        _accounts.value = accountManager.getAccounts()
    }

    fun launchGitHubAuth() {
        _githubAuthInProgress.value = true
        _githubAuthMessage.value = null
        githubAuthManager.startDeviceFlow { status ->
            when (status) {
                is DeviceFlowStatus.WaitingForUser -> {
                    _githubAuthMessage.value = if (status.codePreFilled) {
                        "Approve the sign-in in your browser to finish connecting."
                    } else {
                        "Enter code ${status.userCode} at ${status.verificationUri} to finish connecting."
                    }
                }
                is DeviceFlowStatus.Success -> {
                    _githubAuthInProgress.value = false
                    _githubAuthMessage.value = "Connected as ${status.account.username}."
                    refreshAccounts()
                }
                is DeviceFlowStatus.Failed -> {
                    _githubAuthInProgress.value = false
                    _githubAuthMessage.value = status.message
                }
            }
        }
    }

    fun dismissGitHubAuthMessage() {
        _githubAuthMessage.value = null
    }

    fun addAccount(account: com.manichord.mgit.models.Account) {
        accountManager.addAccount(account)
        _accounts.value = accountManager.getAccounts()
    }

    fun deleteAccount(accountId: Long) {
        accountManager.deleteAccount(accountId)
        _accounts.value = accountManager.getAccounts()
    }

    fun updateAccount(account: com.manichord.mgit.models.Account) {
        accountManager.updateAccount(account)
        _accounts.value = accountManager.getAccounts()
    }

    fun setUseEnglish(use: Boolean) {
        prefsHelper.setUseEnglish(use)
        _useEnglish.value = use
    }

    fun setGitUserName(name: String) {
        prefsHelper.setUserName(name)
        _gitUserName.value = name
    }

    fun setGitUserEmail(email: String) {
        prefsHelper.setUserEmail(email)
        _gitUserEmail.value = email
    }

    fun setUseGravatar(use: Boolean) {
        prefsHelper.setUseGravatar(use)
        _useGravatar.value = use
    }

    fun setUseDynamicColor(use: Boolean) {
        prefsHelper.setUseDynamicColor(use)
        _useDynamicColor.value = use
    }

    fun setFontOption(option: FontOption) {
        prefsHelper.setAppFont(option.id)
        _fontOption.value = option
    }

    private val updateChecker = UpdateChecker()

    private val _checkingForUpdate = MutableLiveData(false)
    val checkingForUpdate: LiveData<Boolean> = _checkingForUpdate

    private val _updateCheckResult = MutableLiveData<UpdateCheckResult?>(null)
    val updateCheckResult: LiveData<UpdateCheckResult?> = _updateCheckResult

    /** Manual check, triggered from the Settings "Check for Updates" row -- bypasses the 24h
     * cooldown RepoListViewModel applies for its own passive check, since the user explicitly
     * asked for this one. Still updates the same shared last-checked timestamp, so the passive
     * check on the repo list won't immediately redo the same work. */
    fun checkForUpdate() {
        _checkingForUpdate.value = true
        updateChecker.checkForUpdate(BuildConfig.VERSION_NAME) { result ->
            Profile.setLastUpdateCheckTime(getApplication(), System.currentTimeMillis())
            _checkingForUpdate.value = false
            _updateCheckResult.value = result
        }
    }
}
