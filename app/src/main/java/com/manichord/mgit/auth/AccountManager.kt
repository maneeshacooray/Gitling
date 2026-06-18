package com.manichord.mgit.auth

import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import me.sheimi.android.utils.SecurePrefsHelper
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class AccountManager(private val securePrefsHelper: SecurePrefsHelper) {

    private val KEY_ACCOUNTS = "global_accounts"

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
                        type = AccountType.valueOf(jsonObj.getString("type"))
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
    }

    fun deleteAccount(accountId: Long) {
        val accounts = getAccounts().filter { it.id != accountId }
        saveAccounts(accounts)
    }

    /** Best-effort match of a git remote URL to a connected account, by host -> AccountType. */
    fun findAccountForRemoteUrl(remoteUrl: String?): Account? {
        if (remoteUrl.isNullOrBlank()) return null
        val host = try {
            java.net.URI(remoteUrl).host
        } catch (e: Exception) {
            null
        } ?: return null

        val type = when {
            host.endsWith("github.com") -> AccountType.GITHUB
            host.endsWith("gitlab.com") -> AccountType.GITLAB
            host.endsWith("bitbucket.org") -> AccountType.BITBUCKET
            else -> return null
        }
        return getAccounts().firstOrNull { it.type == type }
    }

    fun updateAccount(updatedAccount: Account) {
        val accounts = getAccounts().map {
            if (it.id == updatedAccount.id) updatedAccount else it
        }
        saveAccounts(accounts)
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
            jsonArray.put(jsonObj)
        }
        securePrefsHelper.set(KEY_ACCOUNTS, jsonArray.toString())
    }
}
