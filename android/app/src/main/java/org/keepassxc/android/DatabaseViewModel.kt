package org.keepassxc.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log
import org.keepassxc.android.models.Entry
import org.keepassxc.android.models.EntryDetails
import org.keepassxc.android.models.GroupNode
import org.keepassxc.android.models.EntryHistoryItem

class DatabaseViewModel : ViewModel() {
    private var dbPtr: Long = 0
    
    var isUnlocked by mutableStateOf(false)
        private set
        
    var rootGroupName by mutableStateOf("")
        private set

    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set

    var groups by mutableStateOf<List<GroupNode>>(emptyList())
        private set

    var currentGroupUuid by mutableStateOf<String?>(null)
        private set

    var selectedEntry by mutableStateOf<EntryDetails?>(null)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    var currentUri: android.net.Uri? = null
        private set

    fun unlock(filePath: String, password: String, useYubiKey: Boolean) {
        if (isUnlocked) {
            close()
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val ptr = NativeCore.openDatabase(filePath, password, useYubiKey)
            handleUnlockResult(ptr)
        }
    }

    fun unlockFromUri(contentResolver: android.content.ContentResolver, uri: android.net.Uri, password: String, keyfileUri: android.net.Uri?, useYubiKey: Boolean) {
        if (isUnlocked) {
            close()
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var keyfileBytes: ByteArray? = null
                if (keyfileUri != null) {
                    contentResolver.openInputStream(keyfileUri)?.use {
                        keyfileBytes = it.readBytes()
                    }
                }
                
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val ptr = NativeCore.openDatabaseFromBytes(bytes, password, keyfileBytes, useYubiKey)
                    if (ptr != 0L) {
                        currentUri = uri
                    }
                    handleUnlockResult(ptr)
                } ?: run {
                    lastError = "Could not open file."
                }
            } catch (e: Exception) {
                lastError = "Error reading file: ${e.message}"
                Log.e("DatabaseViewModel", "Error reading URI", e)
            }
        }
    }

    fun createDatabase(contentResolver: android.content.ContentResolver, uri: android.net.Uri, password: String, keyfileUri: android.net.Uri?, useYubiKey: Boolean) {
        if (isUnlocked) {
            close()
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var keyfileBytes: ByteArray? = null
                if (keyfileUri != null) {
                    contentResolver.openInputStream(keyfileUri)?.use {
                        keyfileBytes = it.readBytes()
                    }
                }
                val ptr = NativeCore.createDatabase(password, keyfileBytes, useYubiKey)
                if (ptr != 0L) {
                    currentUri = uri
                    handleUnlockResult(ptr)
                    saveDatabase(contentResolver)
                } else {
                    lastError = "Could not create database."
                }
            } catch (e: Exception) {
                lastError = "Error creating database: ${e.message}"
                Log.e("DatabaseViewModel", "Error creating DB", e)
            }
        }
    }

    private fun handleUnlockResult(ptr: Long) {
        if (ptr != 0L) {
            dbPtr = ptr
            isUnlocked = true
            rootGroupName = NativeCore.getRootGroupName(dbPtr)
            refreshGroups()
            refreshEntries()
            lastError = null
            Log.i("DatabaseViewModel", "Database unlocked: $rootGroupName")
        } else {
            isUnlocked = false
            lastError = "Failed to unlock database. Check password."
            Log.e("DatabaseViewModel", "Failed to unlock database")
        }
    }

    fun selectGroup(uuid: String?) {
        currentGroupUuid = uuid
        refreshEntries()
    }

    fun selectEntry(uuid: String?) {
        if (uuid == null) {
            selectedEntry = null
        } else if (dbPtr != 0L) {
            selectedEntry = NativeCore.getEntryDetails(dbPtr, uuid)
        }
    }

    fun getTotp(uuid: String): String? {
        if (dbPtr != 0L) {
            return NativeCore.getTotp(dbPtr, uuid)
        }
        return null
    }

    fun addEntry(title: String, username: String, password: String, url: String, notes: String): String? {
        if (dbPtr != 0L) {
            val groupUuid = currentGroupUuid ?: groups.firstOrNull { it.depth == 0 }?.uuid ?: ""
            val uuid = NativeCore.addEntry(dbPtr, groupUuid, title, username, password, url, notes)
            if (uuid.isNotEmpty()) {
                refreshEntries()
                return uuid
            }
        }
        return null
    }

    fun updateEntry(uuid: String, title: String, username: String, password: String, url: String, notes: String): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.updateEntry(dbPtr, uuid, title, username, password, url, notes)
            if (success) {
                refreshEntries()
                if (selectedEntry?.uuid == uuid) {
                    selectEntry(uuid)
                }
                return true
            }
        }
        return false
    }

    fun addGroup(parentUuid: String?, name: String): String? {
        if (dbPtr != 0L) {
            val pUuid = parentUuid ?: currentGroupUuid ?: groups.firstOrNull { it.depth == 0 }?.uuid ?: ""
            val uuid = NativeCore.addGroup(dbPtr, pUuid, name)
            if (uuid.isNotEmpty()) {
                refreshGroups()
                return uuid
            }
        }
        return null
    }

    fun renameGroup(groupUuid: String, newName: String): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.renameGroup(dbPtr, groupUuid, newName)
            if (success) {
                refreshGroups()
                return true
            }
        }
        return false
    }

    fun deleteGroup(groupUuid: String): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.deleteGroup(dbPtr, groupUuid)
            if (success) {
                if (currentGroupUuid == groupUuid) {
                    currentGroupUuid = null
                }
                refreshGroups()
                refreshEntries()
                return true
            }
        }
        return false
    }

    fun moveEntry(entryUuid: String, targetGroupUuid: String): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.moveEntry(dbPtr, entryUuid, targetGroupUuid)
            if (success) {
                refreshEntries()
                return true
            }
        }
        return false
    }

    fun moveGroup(groupUuid: String, targetParentUuid: String): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.moveGroup(dbPtr, groupUuid, targetParentUuid)
            if (success) {
                refreshGroups()
                return true
            }
        }
        return false
    }

    fun getEntryHistory(uuid: String): List<org.keepassxc.android.models.EntryHistoryItem> {
        if (dbPtr != 0L) {
            return NativeCore.getEntryHistory(dbPtr, uuid)
        }
        return emptyList()
    }

    fun restoreEntryHistory(entryUuid: String, historyUuid: String): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.restoreEntryHistory(dbPtr, entryUuid, historyUuid)
            if (success) {
                refreshEntries()
                if (selectedEntry?.uuid == entryUuid) selectEntry(entryUuid)
                return true
            }
        }
        return false
    }

    fun emptyRecycleBin(): Boolean {
        if (dbPtr != 0L) {
            val success = NativeCore.emptyRecycleBin(dbPtr)
            if (success) {
                refreshGroups()
                refreshEntries()
                return true
            }
        }
        return false
    }

    fun saveDatabase(contentResolver: android.content.ContentResolver) {
        if (dbPtr == 0L || currentUri == null) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bytes = NativeCore.saveDatabaseAsBytes(dbPtr)
            if (bytes != null) {
                try {
                    contentResolver.openOutputStream(currentUri!!, "wt")?.use { outputStream ->
                        outputStream.write(bytes)
                        Log.i("DatabaseViewModel", "Database saved to URI successfully.")
                    }
                } catch (e: Exception) {
                    Log.e("DatabaseViewModel", "Failed to save database to URI", e)
                }
            } else {
                Log.e("DatabaseViewModel", "saveDatabaseAsBytes returned null")
            }
        }
    }

    fun createKeyfile(contentResolver: android.content.ContentResolver, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bytes = NativeCore.createKeyfileAsBytes()
                contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(bytes)
                    Log.i("DatabaseViewModel", "Keyfile created successfully.")
                }
            } catch (e: Exception) {
                Log.e("DatabaseViewModel", "Failed to create keyfile", e)
            }
        }
    }

    fun refreshGroups() {
        if (dbPtr != 0L) {
            groups = NativeCore.getGroups(dbPtr)
        }
    }

    fun refreshEntries() {
        if (dbPtr != 0L) {
            entries = if (currentGroupUuid == null) {
                NativeCore.getEntries(dbPtr) // Recursive for root search etc.
            } else {
                NativeCore.getEntriesInGroup(dbPtr, currentGroupUuid!!)
            }
        }
    }

    fun close() {
        if (dbPtr != 0L) {
            NativeCore.closeDatabase(dbPtr)
            dbPtr = 0
            isUnlocked = false
            rootGroupName = ""
            entries = emptyList<Entry>()
            groups = emptyList<GroupNode>()
            currentGroupUuid = null
            selectedEntry = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        close()
    }
}
