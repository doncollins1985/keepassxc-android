package org.keepassxc.android

import android.util.Log

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
     * Adds a new entry and returns its UUID hex string.
     */
    external fun addEntry(dbPtr: Long, title: String?, username: String?, password: String?, url: String?, notes: String?): String

    /**
     * Updates an existing entry by its UUID hex string.
     */
    external fun updateEntry(dbPtr: Long, uuidHex: String, title: String?, username: String?, password: String?, url: String?, notes: String?): Boolean
}
