package com.manichord.mgit.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
