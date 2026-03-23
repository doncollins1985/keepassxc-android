package org.keepassxc.android.models

data class GroupNode(
    val name: String,
    val uuid: String,
    val depth: Int,
    val fullPath: String
)