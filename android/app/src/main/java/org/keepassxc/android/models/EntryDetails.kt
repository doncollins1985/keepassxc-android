package org.keepassxc.android.models

data class EntryDetails(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val uuid: String,
    val totp: String? = null
)
