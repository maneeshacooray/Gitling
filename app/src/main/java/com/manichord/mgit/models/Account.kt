package com.manichord.mgit.models

enum class AccountType(val displayName: String) {
    GITHUB("GitHub"),
    GITLAB("GitLab"),
    BITBUCKET("Bitbucket"),
    CUSTOM("Custom")
}

data class Account(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val username: String,
    val token: String,
    val type: AccountType = AccountType.GITHUB,
    val baseUrl: String? = null
)
