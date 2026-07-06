package com.manichord.mgit.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import me.sheimi.android.utils.SecurePrefsHelper
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class AccountManager(private val securePrefsHelper: SecurePrefsHelper) {

    private val KEY_ACCOUNTS = "global_accounts"

    /** Fires whenever accounts are added/updated/deleted, so every ViewModel holding its own
     * cached copy of getAccounts() (SettingsViewModel, CloneViewModel) can refresh in response,
     * regardless of Activity lifecycle timing. This matters specifically for the GitHub OAuth
     * Device Flow: completeSignIn() below runs on GitHubAuthManager's background polling thread,
     * which can land *after* the user has already switched back to the app (and any
     * onResume-triggered refresh has already run and found nothing yet) -- postValue (safe from
     * any thread) is the only reliable way to notify observers at the moment the account is
     * actually saved. */
    private val _accountsChanged = MutableLiveData<Unit>()
    val accountsChanged: LiveData<Unit> = _accountsChanged

    fun getAccounts(): List<Account> {
        val jsonString = securePrefsHelper.get(KEY_ACCOUNTS) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val accounts = mutableListOf<Account>()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                accounts.add(
                    Account(
                        id = jsonObj.getLong("id"),
                        name = jsonObj.getString("name"),
                        username = jsonObj.getString("username"),
                        token = jsonObj.getString("token"),
                        type = AccountType.valueOf(jsonObj.getString("type")),
                        baseUrl = jsonObj.optString("baseUrl").takeIf { it.isNotBlank() }
                    )
                )
            }
            accounts
        } catch (e: Exception) {
            Timber.e(e, "Error parsing accounts")
            emptyList()
        }
    }

    fun addAccount(account: Account) {
        val accounts = getAccounts().toMutableList()
        accounts.add(account)
        saveAccounts(accounts)
        _accountsChanged.postValue(Unit)
    }

    fun deleteAccount(accountId: Long) {
        val accounts = getAccounts().filter { it.id != accountId }
        saveAccounts(accounts)
        _accountsChanged.postValue(Unit)
    }

    /** Best-effort match of a git remote URL to a connected account, by host -> AccountType. */
    fun findAccountForRemoteUrl(remoteUrl: String?): Account? {
        if (remoteUrl.isNullOrBlank()) return null
        val host = try {
            java.net.URI(remoteUrl).host
        } catch (e: Exception) {
            null
        } ?: return null

        val accounts = getAccounts()
        val knownType = when {
            host.endsWith("github.com") -> AccountType.GITHUB
            host.endsWith("gitlab.com") -> AccountType.GITLAB
            host.endsWith("bitbucket.org") -> AccountType.BITBUCKET
            else -> null
        }
        if (knownType != null) {
            return accounts.firstOrNull { it.type == knownType }
        }
        // For custom/self-hosted instances (e.g. Forgejo, Gitea), match by comparing the
        // remote's host against the host stored in the account's baseUrl.
        return accounts.firstOrNull { account ->
            if (account.baseUrl.isNullOrBlank()) return@firstOrNull false
            val accountHost = try { java.net.URI(account.baseUrl).host } catch (e: Exception) { null }
            accountHost != null && host.equals(accountHost, ignoreCase = true)
        }
    }

    fun updateAccount(updatedAccount: Account) {
        val accounts = getAccounts().map {
            if (it.id == updatedAccount.id) updatedAccount else it
        }
        saveAccounts(accounts)
        _accountsChanged.postValue(Unit)
    }

    private fun saveAccounts(accounts: List<Account>) {
        val jsonArray = JSONArray()
        accounts.forEach { account ->
            val jsonObj = JSONObject()
            jsonObj.put("id", account.id)
            jsonObj.put("name", account.name)
            jsonObj.put("username", account.username)
            jsonObj.put("token", account.token)
            jsonObj.put("type", account.type.name)
            if (!account.baseUrl.isNullOrBlank()) jsonObj.put("baseUrl", account.baseUrl)
            jsonArray.put(jsonObj)
        }
        securePrefsHelper.set(KEY_ACCOUNTS, jsonArray.toString())
    }
}
