package org.keepassxc.android

import android.util.Log
import org.keepassxc.android.models.GroupNode
import org.keepassxc.android.models.EntryHistoryItem

interface YubiKeyCallback {
    fun onChallenge(challenge: ByteArray): ByteArray?
}

object NativeCore {
    private const val TAG = "NativeCore"

    init {
        try {
            System.loadLibrary("keepassxc-android-core")
            Log.i(TAG, "KeePassXC Native Core loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load KeePassXC Native Core", e)
        }
    }

    /**
     * Sets the global callback for YubiKey challenge-response.
     */
    external fun setYubiKeyCallback(callback: YubiKeyCallback?)

    /**
     * Test function to verify JNI connection.
     */
    external fun stringFromJNI(): String

    /**
     * Unlocks a database and returns a native pointer to the Database object.
     * Returns 0 if unlocking failed.
     */
    external fun openDatabase(filePath: String, password: String, useYubiKey: Boolean): Long

    /**
     * Unlocks a database from a byte array.
     */
    external fun openDatabaseFromBytes(data: ByteArray, password: String, keyfileBytes: ByteArray?, useYubiKey: Boolean): Long

    /**
     * Creates a new database.
     */
    external fun createDatabase(password: String, keyfileBytes: ByteArray?, useYubiKey: Boolean): Long

    /**
     * Serializes the database to a byte array for saving.
     */
    external fun saveDatabaseAsBytes(dbPtr: Long): ByteArray?

    /**
     * Creates a new Keyfile (KeePass 2 XML Format) returning the bytes.
     */
    external fun createKeyfileAsBytes(): ByteArray

    /**
     * Closes the database and frees the native memory.
     */
    external fun closeDatabase(dbPtr: Long)

    /**
     * Returns the name of the root group.
     */
    external fun getRootGroupName(dbPtr: Long): String

    /**
     * Returns all entries in the database (flattened for now).
     */
    external fun getEntries(dbPtr: Long): List<org.keepassxc.android.models.Entry>

    /**
     * Returns full details for a specific entry by its UUID hex string.
     */
    external fun getEntryDetails(dbPtr: Long, uuidHex: String): org.keepassxc.android.models.EntryDetails

    /**
     * Returns the current TOTP string for an entry.
     */
    external fun getTotp(dbPtr: Long, entryUuidHex: String): String?

    /**
     * Adds a new entry to a specific group and returns its UUID hex string.
     */
    external fun addEntry(dbPtr: Long, groupUuidHex: String, title: String?, username: String?, password: String?, url: String?, notes: String?): String

    /**
     * Updates an existing entry by its UUID hex string.
     */
    external fun updateEntry(dbPtr: Long, uuidHex: String, title: String?, username: String?, password: String?, url: String?, notes: String?): Boolean

    /**
     * Returns all groups in the database as a flattened list of GroupNodes with depth info.
     */
    external fun getGroups(dbPtr: Long): List<org.keepassxc.android.models.GroupNode>

    /**
     * Returns entries for a specific group (not recursive).
     */
    external fun getEntriesInGroup(dbPtr: Long, groupUuidHex: String): List<org.keepassxc.android.models.Entry>

    /**
     * Creates a new group.
     */
    external fun addGroup(dbPtr: Long, parentUuidHex: String, name: String): String

    /**
     * Renames an existing group.
     */
    external fun renameGroup(dbPtr: Long, groupUuidHex: String, newName: String): Boolean

    /**
     * Deletes a group and its content (or moves to Recycle Bin).
     */
    external fun deleteGroup(dbPtr: Long, groupUuidHex: String): Boolean

    /**
     * Moves an entry to a different group.
     */
    external fun moveEntry(dbPtr: Long, entryUuidHex: String, groupUuidHex: String): Boolean

    /**
     * Moves a group to a different parent group.
     */
    external fun moveGroup(dbPtr: Long, groupUuidHex: String, parentUuidHex: String): Boolean

    /**
     * Returns the entry history items.
     */
    external fun getEntryHistory(dbPtr: Long, entryUuidHex: String): List<org.keepassxc.android.models.EntryHistoryItem>

    /**
     * Restores a history state of an entry.
     */
    external fun restoreEntryHistory(dbPtr: Long, entryUuidHex: String, historyUuidHex: String): Boolean

    /**
     * Empties the database recycle bin.
     */
    external fun emptyRecycleBin(dbPtr: Long): Boolean
}
