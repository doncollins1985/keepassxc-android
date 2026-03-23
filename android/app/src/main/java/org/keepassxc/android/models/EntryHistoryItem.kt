package org.keepassxc.android.models

data class EntryHistoryItem(
    val index: Int,
    val title: String,
    val username: String,
    val url: String,
    val uuid: String,
    val modifiedAtMs: Long
)