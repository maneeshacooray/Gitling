package com.manichord.mgit.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.manichord.mgit.auth.DeviceFlowStatus
import me.sheimi.sgit.MGitApplication
import me.sheimi.sgit.R
import me.sheimi.sgit.preference.PreferenceHelper

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsHelper: PreferenceHelper = (application as MGitApplication).prefenceHelper!!

    private val _repoRoot = MutableLiveData(prefsHelper.repoRoot?.absolutePath ?: "")
    val repoRoot: LiveData<String> = _repoRoot

    private val _useEnglish = MutableLiveData(prefsHelper.useEnglish())
    val useEnglish: LiveData<Boolean> = _useEnglish

    private val _gitUserName = MutableLiveData(prefsHelper.getUserName() ?: "")
    val gitUserName: LiveData<String> = _gitUserName

    private val _gitUserEmail = MutableLiveData(prefsHelper.getUserEmail() ?: "")
    val gitUserEmail: LiveData<String> = _gitUserEmail

    private val _useGravatar = MutableLiveData(prefsHelper.useGravatar())
    val useGravatar: LiveData<Boolean> = _useGravatar

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

    fun setRepoRoot(path: String) {
        prefsHelper.setRepoRoot(path)
        _repoRoot.value = path
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
}
