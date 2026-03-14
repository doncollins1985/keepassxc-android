package org.keepassxc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeePassXCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UnlockScreen()
                }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    entry: EntryDetails,
    onSave: (String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    var title by remember(entry.uuid) { mutableStateOf(entry.title) }
    var username by remember(entry.uuid) { mutableStateOf(entry.username) }
    var password by remember(entry.uuid) { mutableStateOf(entry.password) }
    var url by remember(entry.uuid) { mutableStateOf(entry.url) }
    var notes by remember(entry.uuid) { mutableStateOf(entry.notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(title, username, password, url, notes) }, enabled = title.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Group") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Group name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = groupNameInput.isNotBlank() && onCreateGroup(groupNameInput.trim())
                        localMessage = if (ok) "Group created." else "Failed to create group."
                        showCreateGroupDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameGroupDialog) {
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = false },
            title = { Text("Rename Group") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = groupNameInput.isNotBlank() && onRenameSelectedGroup(groupNameInput.trim())
                        localMessage = if (ok) "Group renamed." else "Failed to rename group."
                        showRenameGroupDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Delete the selected group and move it to recycle bin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = onDeleteSelectedGroup()
                        localMessage = if (ok) "Group deleted." else "Failed to delete group."
                        showDeleteGroupDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    entry: EntryDetails,
    onSave: (String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    var title by remember(entry.uuid) { mutableStateOf(entry.title) }
    var username by remember(entry.uuid) { mutableStateOf(entry.username) }
    var password by remember(entry.uuid) { mutableStateOf(entry.password) }
    var url by remember(entry.uuid) { mutableStateOf(entry.url) }
    var notes by remember(entry.uuid) { mutableStateOf(entry.notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(title, username, password, url, notes) }, enabled = title.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Group") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Group name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = groupNameInput.isNotBlank() && onCreateGroup(groupNameInput.trim())
                        localMessage = if (ok) "Group created." else "Failed to create group."
                        showCreateGroupDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameGroupDialog) {
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = false },
            title = { Text("Rename Group") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = groupNameInput.isNotBlank() && onRenameSelectedGroup(groupNameInput.trim())
                        localMessage = if (ok) "Group renamed." else "Failed to rename group."
                        showRenameGroupDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Delete the selected group and move it to recycle bin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = onDeleteSelectedGroup()
                        localMessage = if (ok) "Group deleted." else "Failed to delete group."
                        showDeleteGroupDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    entry: EntryDetails,
    onSave: (String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    var title by remember(entry.uuid) { mutableStateOf(entry.title) }
    var username by remember(entry.uuid) { mutableStateOf(entry.username) }
    var password by remember(entry.uuid) { mutableStateOf(entry.password) }
    var url by remember(entry.uuid) { mutableStateOf(entry.url) }
    var notes by remember(entry.uuid) { mutableStateOf(entry.notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(title, username, password, url, notes) }, enabled = title.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Group") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Group name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = groupNameInput.isNotBlank() && onCreateGroup(groupNameInput.trim())
                        localMessage = if (ok) "Group created." else "Failed to create group."
                        showCreateGroupDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameGroupDialog) {
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = false },
            title = { Text("Rename Group") },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = groupNameInput.isNotBlank() && onRenameSelectedGroup(groupNameInput.trim())
                        localMessage = if (ok) "Group renamed." else "Failed to rename group."
                        showRenameGroupDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Group") },
            text = { Text("Delete the selected group and move it to recycle bin?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = onDeleteSelectedGroup()
                        localMessage = if (ok) "Group deleted." else "Failed to delete group."
                        showDeleteGroupDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) { Text("Cancel") }
            }
        )
    }
}
            }
            if (localMessage != null) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localMessage!!,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { localMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            if (localMessage != null) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localMessage!!,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { localMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            if (localMessage != null) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localMessage!!,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { localMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeePassXCTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}

@Composable
fun AutoLockObserver(
    enabled: Boolean,
    lockTimeoutSeconds: Int,
    onLock: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundTimestampMs by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(lifecycleOwner, enabled, lockTimeoutSeconds) {
        val observer = LifecycleEventObserver { _, event ->
            if (!enabled) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundTimestampMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val start = backgroundTimestampMs
                    if (start != null) {
                        val elapsedMs = System.currentTimeMillis() - start
                        if (elapsedMs >= lockTimeoutSeconds.coerceAtLeast(0) * 1000L) {
                            onLock()
                        }
                    }
                    backgroundTimestampMs = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun copyToClipboardWithTimeout(
    context: Context,
    label: String,
    value: String,
    timeoutSeconds: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, value))
    if (timeoutSeconds <= 0) {
        manager.setPrimaryClip(ClipData.newPlainText("", ""))
        return
    }
    coroutineScope.launch {
        delay(timeoutSeconds * 1000L)
        val current = manager.primaryClip
        val currentText = current?.getItemAt(0)?.coerceToText(context)?.toString()
        if (currentText == value) {
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}

@Composable
fun AutoLockObserver(
    enabled: Boolean,
    lockTimeoutSeconds: Int,
    onLock: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundTimestampMs by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(lifecycleOwner, enabled, lockTimeoutSeconds) {
        val observer = LifecycleEventObserver { _, event ->
            if (!enabled) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundTimestampMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val start = backgroundTimestampMs
                    if (start != null) {
                        val elapsedMs = System.currentTimeMillis() - start
                        if (elapsedMs >= lockTimeoutSeconds.coerceAtLeast(0) * 1000L) {
                            onLock()
                        }
                    }
                    backgroundTimestampMs = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun copyToClipboardWithTimeout(
    context: Context,
    label: String,
    value: String,
    timeoutSeconds: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, value))
    if (timeoutSeconds <= 0) {
        manager.setPrimaryClip(ClipData.newPlainText("", ""))
        return
    }
    coroutineScope.launch {
        delay(timeoutSeconds * 1000L)
        val current = manager.primaryClip
        val currentText = current?.getItemAt(0)?.coerceToText(context)?.toString()
        if (currentText == value) {
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}

@Composable
fun AutoLockObserver(
    enabled: Boolean,
    lockTimeoutSeconds: Int,
    onLock: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundTimestampMs by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(lifecycleOwner, enabled, lockTimeoutSeconds) {
        val observer = LifecycleEventObserver { _, event ->
            if (!enabled) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundTimestampMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    val start = backgroundTimestampMs
                    if (start != null) {
                        val elapsedMs = System.currentTimeMillis() - start
                        if (elapsedMs >= lockTimeoutSeconds.coerceAtLeast(0) * 1000L) {
                            onLock()
                        }
                    }
                    backgroundTimestampMs = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun copyToClipboardWithTimeout(
    context: Context,
    label: String,
    value: String,
    timeoutSeconds: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, value))
    if (timeoutSeconds <= 0) {
        manager.setPrimaryClip(ClipData.newPlainText("", ""))
        return
    }
    coroutineScope.launch {
        delay(timeoutSeconds * 1000L)
        val current = manager.primaryClip
        val currentText = current?.getItemAt(0)?.coerceToText(context)?.toString()
        if (currentText == value) {
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen() {
    var password by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("No file selected") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "KeePassXC Android",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { /* TODO: File picker */ }) {
            Text(text = "Select Database")
        }
        
        Text(text = filePath, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* TODO: Call JNI to unlock */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
    }
}
