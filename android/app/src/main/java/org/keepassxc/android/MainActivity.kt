package org.keepassxc.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import org.keepassxc.android.models.Entry
import org.keepassxc.android.models.EntryDetails
import org.keepassxc.android.models.GroupNode

import com.yubico.yubikit.android.YubiKitManager
import com.yubico.yubikit.android.transport.usb.UsbConfiguration
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration
import com.yubico.yubikit.core.smartcard.SmartCardConnection
import com.yubico.yubikit.yubiotp.YubiOtpSession

import org.keepassxc.android.biometrics.BiometricHelper
import android.util.Base64
import javax.crypto.Cipher

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val viewModel: DatabaseViewModel by viewModels()
    private var yubiKitManager: YubiKitManager? = null
    var isWaitingForYubiKey = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        yubiKitManager = YubiKitManager(this)

        NativeCore.setYubiKeyCallback(object : YubiKeyCallback {
            override fun onChallenge(challenge: ByteArray): ByteArray? {
                var result: ByteArray? = null
                val latch = java.util.concurrent.CountDownLatch(1)

                runOnUiThread {
                    isWaitingForYubiKey.value = true
                    
                    val onDevice = { device: com.yubico.yubikit.core.YubiKeyDevice ->
                        com.yubico.yubikit.yubiotp.YubiOtpSession.create(device) { sessionResult ->
                            Thread {
                                try {
                                    android.util.Log.d("YubiKey", "Session created successfully")
                                    val session = sessionResult.value
                                    result = session.calculateHmacSha1(com.yubico.yubikit.yubiotp.Slot.TWO, challenge, null)
                                    android.util.Log.d("YubiKey", "Challenge calculated successfully")
                                } catch (e: Exception) {
                                    android.util.Log.e("YubiKey", "Error calculating HMAC", e)
                                    e.printStackTrace()
                                } finally {
                                    latch.countDown()
                                }
                            }.start()
                        }
                    }

                    val usbConfig = UsbConfiguration()
                        .handlePermissions(true)
                        .setDeviceFilter(com.yubico.yubikit.android.transport.usb.KeePassXCDeviceFilter())

                    yubiKitManager?.startUsbDiscovery(usbConfig, onDevice)
                    yubiKitManager?.startNfcDiscovery(NfcConfiguration(), this@MainActivity, onDevice)
                }

                latch.await(30, java.util.concurrent.TimeUnit.SECONDS)

                runOnUiThread {
                    isWaitingForYubiKey.value = false
                    yubiKitManager?.stopUsbDiscovery()
                    yubiKitManager?.stopNfcDiscovery(this@MainActivity)
                }
                return result
            }
        })

        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            var currentScreen by remember { mutableStateOf(Screen.UNLOCK) }
            val waitingForYubiKey by isWaitingForYubiKey

            LaunchedEffect(viewModel.isUnlocked) {
                if (viewModel.isUnlocked) {
                    currentScreen = Screen.DATABASE
                } else if (currentScreen != Screen.SETTINGS) {
                    currentScreen = Screen.UNLOCK
                }
            }

            KeePassXCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.UNLOCK -> {
                            UnlockScreen(
                                lastError = viewModel.lastError,
                                onUnlock = { uri, password, keyfileUri, useYubiKey ->
                                    viewModel.unlockFromUri(contentResolver, uri, password, keyfileUri, useYubiKey)
                                },
                                onCreateDatabase = { uri, password, keyfileUri, useYubiKey ->
                                    viewModel.createDatabase(contentResolver, uri, password, keyfileUri, useYubiKey)
                                },
                                onCreateKeyfile = { uri ->
                                    viewModel.createKeyfile(contentResolver, uri)
                                },
                                onOpenSettings = { currentScreen = Screen.SETTINGS },
                                settingsManager = settingsManager
                            )
                        }
                        Screen.DATABASE -> {
                            val selectedEntry = viewModel.selectedEntry
                            if (selectedEntry != null) {
                                EntryDetailView(
                                    entry = selectedEntry,
                                    onBack = { viewModel.selectEntry(null) },
                                    onEdit = { currentScreen = Screen.ADD_EDIT_ENTRY },
                                    getTotp = { viewModel.getTotp(selectedEntry.uuid) },
                                    getHistory = { viewModel.getEntryHistory(selectedEntry.uuid) },
                                    onRestoreHistory = { historyUuid -> viewModel.restoreEntryHistory(selectedEntry.uuid, historyUuid); viewModel.saveDatabase(contentResolver) }
                                )
                            } else {
                                DatabaseView(
                                    currentGroupUuid = viewModel.currentGroupUuid,
                                    groups = viewModel.groups,
                                    entries = viewModel.entries,
                                    onEntryClick = { entry -> viewModel.selectEntry(entry.uuid) },
                                    onGroupClick = { group -> viewModel.selectGroup(group.uuid) },
                                    onClose = { viewModel.close() },
                                    onOpenSettings = { currentScreen = Screen.SETTINGS },
                                    onAddEntry = { 
                                        viewModel.selectEntry(null)
                                        currentScreen = Screen.ADD_EDIT_ENTRY 
                                    },
                                    onAddGroup = { parentUuid, name -> viewModel.addGroup(parentUuid, name); viewModel.saveDatabase(contentResolver) },
                                    onRenameGroup = { uuid, name -> viewModel.renameGroup(uuid, name); viewModel.saveDatabase(contentResolver) },
                                    onDeleteGroup = { uuid -> viewModel.deleteGroup(uuid); viewModel.saveDatabase(contentResolver) },
                                    onEmptyRecycleBin = { viewModel.emptyRecycleBin(); viewModel.saveDatabase(contentResolver) }
                                )
                            }
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                settingsManager = settingsManager,
                                onBack = { 
                                    currentScreen = if (viewModel.isUnlocked) Screen.DATABASE else Screen.UNLOCK 
                                }
                            )
                        }
                        Screen.ADD_EDIT_ENTRY -> {
                            AddEditEntryScreen(
                                entry = viewModel.selectedEntry,
                                onSave = { title, username, password, url, notes ->
                                    if (viewModel.selectedEntry == null) {
                                        viewModel.addEntry(title, username, password, url, notes)
                                    } else {
                                        viewModel.updateEntry(viewModel.selectedEntry!!.uuid, title, username, password, url, notes)
                                    }
                                    viewModel.saveDatabase(contentResolver)
                                    currentScreen = Screen.DATABASE
                                },
                                onCancel = { currentScreen = Screen.DATABASE }
                            )
                        }
                        Screen.ENTRY -> {}
                    }

                    if (waitingForYubiKey) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Hardware Key Required") },
                            text = { Text("Please insert or tap your YubiKey to unlock the database.") },
                            confirmButton = { CircularProgressIndicator() }
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    UNLOCK, DATABASE, ENTRY, SETTINGS, ADD_EDIT_ENTRY
}

val KeePassXCGreen = Color(0xFF61A142)
val KeePassXCDarkGrey = Color(0xFF1E1E1E)
val KeePassXCSurface = Color(0xFF2C313B)

@Composable
fun KeePassXCTheme(content: @Composable () -> Unit) {
    val customColorScheme = darkColorScheme(
        primary = KeePassXCGreen,
        onPrimary = Color.White,
        background = KeePassXCDarkGrey,
        onBackground = Color.White,
        surface = KeePassXCSurface,
        onSurface = Color.White,
        primaryContainer = KeePassXCSurface,
        onPrimaryContainer = KeePassXCGreen
    )

    MaterialTheme(
        colorScheme = customColorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    lastError: String?, 
    onUnlock: (Uri, String, Uri?, Boolean) -> Unit, 
    onCreateDatabase: (Uri, String, Uri?, Boolean) -> Unit,
    onCreateKeyfile: (Uri) -> Unit,
    onOpenSettings: () -> Unit,
    settingsManager: SettingsManager
) {
    val coroutineScope = rememberCoroutineScope()
    val lastUriString by settingsManager.lastDatabaseUri.collectAsState(initial = null as String?)
    
    var password by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedKeyfileUri by remember { mutableStateOf<Uri?>(null) }
    var useYubiKey by remember { mutableStateOf(false) }
    var isCreateMode by remember { mutableStateOf(false) }
    
    LaunchedEffect(lastUriString) {
        if (lastUriString != null && selectedUri == null) {
            selectedUri = Uri.parse(lastUriString)
        }
    }
    
    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper() }
    val encryptedPassword by settingsManager.encryptedPassword.collectAsState(initial = null as String?)
    val encryptedPasswordIv by settingsManager.encryptedPasswordIv.collectAsState(initial = null as String?)
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> 
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                selectedUri = uri
                coroutineScope.launch { settingsManager.setLastDatabaseUri(uri.toString()) }
            }
        }
    )

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri -> 
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                selectedUri = uri
                coroutineScope.launch { settingsManager.setLastDatabaseUri(uri.toString()) }
            }
        }
    )

    val createKeyfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri -> 
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                selectedKeyfileUri = uri
                onCreateKeyfile(uri)
            }
        }
    )

    val keyfilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> 
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedKeyfileUri = uri
            }
        }
    )

    val biometricUnlockEnabled by settingsManager.biometricUnlockEnabled.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KeePassXC") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_configure), contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(R.mipmap.ic_launcher), contentDescription = "KeePassXC Logo", modifier = Modifier.size(96.dp).padding(bottom = 16.dp))
            Text(text = if (isCreateMode) "Create Database" else "Unlock Database", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = { if (isCreateMode) createFileLauncher.launch("Passwords.kdbx") else filePickerLauncher.launch(arrayOf("*/*")) }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_object_unlocked), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(if (isCreateMode) "New Database File" else "Selected Database", style = MaterialTheme.typography.labelMedium)
                        Text(text = selectedUri?.path?.substringAfterLast('/') ?: "No file selected", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(12.dp).clickable { keyfilePickerLauncher.launch(arrayOf("*/*")) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_object_unlocked), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keyfile (Optional)", style = MaterialTheme.typography.labelMedium)
                        Text(text = selectedKeyfileUri?.path?.substringAfterLast('/') ?: "None selected", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    if (selectedKeyfileUri != null) {
                        IconButton(onClick = { selectedKeyfileUri = null }, modifier = Modifier.size(24.dp)) { Icon(painterResource(R.drawable.ic_dialog_close), contentDescription = "Clear") }
                    } else if (isCreateMode) {
                        IconButton(onClick = { createKeyfileLauncher.launch("KeePassXC.keyx") }, modifier = Modifier.size(24.dp)) { Icon(painterResource(R.drawable.ic_entry_new), contentDescription = "Create New Keyfile") }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useYubiKey, onCheckedChange = { useYubiKey = it })
                Text("Use Hardware Key (Challenge-Response)", style = MaterialTheme.typography.bodyMedium)
            }
            if (lastError != null) {
                Text(text = lastError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    selectedUri?.let { uri ->
                        if (isCreateMode) {
                            onCreateDatabase(uri, password, selectedKeyfileUri, useYubiKey)
                        } else {
                            if (biometricUnlockEnabled) {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    val executor = ContextCompat.getMainExecutor(activity)
                                    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            val cipher = result.cryptoObject?.cipher
                                            if (cipher != null) {
                                                val encrypted = cipher.doFinal(password.toByteArray())
                                                val iv = cipher.iv
                                                coroutineScope.launch {
                                                    settingsManager.saveEncryptedPassword(
                                                        Base64.encodeToString(encrypted, Base64.DEFAULT),
                                                        Base64.encodeToString(iv, Base64.DEFAULT)
                                                    )
                                                }
                                            }
                                            onUnlock(uri, password, selectedKeyfileUri, useYubiKey)
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            onUnlock(uri, password, selectedKeyfileUri, useYubiKey)
                                        }
                                    })
                                    prompt.authenticate(
                                        BiometricPrompt.PromptInfo.Builder().setTitle("Set Biometric Unlock").setSubtitle("Confirm your biometric to save the password").setNegativeButtonText("Cancel").build(),
                                        biometricHelper.getEncryptCryptoObject()
                                    )
                                } else { onUnlock(uri, password, selectedKeyfileUri, useYubiKey) }
                            } else { onUnlock(uri, password, selectedKeyfileUri, useYubiKey) }
                        }
                    } 
                }, 
                enabled = selectedUri != null && password.isNotEmpty(), 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isCreateMode) "Create" else "Unlock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { isCreateMode = !isCreateMode }, modifier = Modifier.padding(top = 8.dp)) { Text(if (isCreateMode) "Switch to Unlock Existing" else "Create New Database") }
            
            if (!isCreateMode && biometricUnlockEnabled && selectedUri != null && encryptedPassword != null) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val iv = Base64.decode(encryptedPasswordIv, Base64.DEFAULT)
                        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                val cipher = result.cryptoObject?.cipher
                                if (cipher != null) {
                                    val decrypted = cipher.doFinal(Base64.decode(encryptedPassword, Base64.DEFAULT))
                                    onUnlock(selectedUri!!, String(decrypted), selectedKeyfileUri, useYubiKey)
                                }
                            }
                        })
                        prompt.authenticate(
                            BiometricPrompt.PromptInfo.Builder().setTitle("Unlock KeePassXC").setSubtitle("Log in using your biometric credential").setNegativeButtonText("Cancel").build(),
                            biometricHelper.getDecryptCryptoObject(iv)
                        )
                    }
                }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(painterResource(R.drawable.ic_fingerprint), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Biometric Unlock", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseView(
    currentGroupUuid: String?,
    groups: List<GroupNode>,
    entries: List<Entry>, 
    onEntryClick: (Entry) -> Unit,
    onGroupClick: (GroupNode) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddEntry: () -> Unit,
    onAddGroup: (String?, String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onEmptyRecycleBin: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showRenameGroupDialog by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }
    
    val currentGroup = groups.find { it.uuid == currentGroupUuid } ?: groups.firstOrNull { it.depth == 0 }
    val groupName = currentGroup?.name ?: "Database"

    val filteredEntries = if (searchQuery.isEmpty()) entries else entries.filter { it.title.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Groups", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                Divider()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(groups) { group ->
                        NavigationDrawerItem(
                            label = { Text(group.name) },
                            selected = group.uuid == currentGroupUuid,
                            onClick = { onGroupClick(group); scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            icon = { Icon(if (group.depth == 0) Icons.Default.FolderOpen else Icons.Default.Folder, contentDescription = null) },
                            badge = { if (group.depth > 0) Text("·".repeat(group.depth)) }
                        )
                    }
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { groupNameInput = ""; showAddGroupDialog = true }) { Icon(Icons.Default.Add, null); Text("New Group") }
                    TextButton(onClick = { onEmptyRecycleBin(); scope.launch { drawerState.close() } }) { Icon(Icons.Default.DeleteForever, null); Text("Empty Bin") }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    TopAppBar(
                        title = { Text(groupName, fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
                        actions = {
                            if (currentGroup != null && currentGroup.depth > 0) {
                                IconButton(onClick = { groupNameInput = currentGroup.name; showRenameGroupDialog = true }) { Icon(Icons.Default.Edit, "Rename Group") }
                                IconButton(onClick = { onDeleteGroup(currentGroup.uuid) }) { Icon(Icons.Default.Delete, "Delete Group") }
                            }
                            IconButton(onClick = onOpenSettings) { Icon(painterResource(R.drawable.ic_configure), contentDescription = "Settings") }
                            IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_database_lock), contentDescription = "Lock") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search entries...") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_system_search), contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = onAddEntry, icon = { Icon(painterResource(R.drawable.ic_entry_new), contentDescription = "Add Entry") }, text = { Text("Add Entry") })
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredEntries) { entry ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = { onEntryClick(entry) }) {
                        ListItem(
                            headlineContent = { Text(entry.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(entry.username) },
                            leadingContent = {
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_object_unlocked), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                if (filteredEntries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(text = if (entries.isEmpty()) "No entries found in this group." else "No matching entries found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("Add Group") },
            text = { OutlinedTextField(value = groupNameInput, onValueChange = { groupNameInput = it }, label = { Text("Group Name") }, singleLine = true) },
            confirmButton = { Button(onClick = { onAddGroup(currentGroupUuid, groupNameInput); showAddGroupDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddGroupDialog = false }) { Text("Cancel") } }
        )
    }
    if (showRenameGroupDialog) {
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = false },
            title = { Text("Rename Group") },
            text = { OutlinedTextField(value = groupNameInput, onValueChange = { groupNameInput = it }, label = { Text("New Name") }, singleLine = true) },
            confirmButton = { Button(onClick = { onRenameGroup(currentGroupUuid!!, groupNameInput); showRenameGroupDialog = false }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { showRenameGroupDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailView(entry: EntryDetails, onBack: () -> Unit, onEdit: () -> Unit, getTotp: () -> String?, getHistory: () -> List<org.keepassxc.android.models.EntryHistoryItem>, onRestoreHistory: (String) -> Unit) {
    BackHandler(onBack = onBack)
    
    var currentTotp by remember { mutableStateOf(entry.totp) }
    var secondsLeft by remember { mutableStateOf(30 - ((System.currentTimeMillis() / 1000) % 30).toInt()) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(entry.uuid) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            secondsLeft = 30 - ((System.currentTimeMillis() / 1000) % 30).toInt()
            if (secondsLeft == 30) {
                currentTotp = getTotp()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry.title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_dialog_close), contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) { Icon(Icons.Default.History, contentDescription = "History") }
                    IconButton(onClick = onEdit) { Icon(painterResource(R.drawable.ic_entry_edit), contentDescription = "Edit") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailItem(label = "Username", value = entry.username)
                    DetailItem(label = "Password", value = entry.password, isSensitive = true)
                    if (entry.url.isNotEmpty()) DetailItem(label = "URL", value = entry.url)
                    
                    if (currentTotp != null) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(text = "TOTP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(text = currentTotp!!, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), letterSpacing = 2.sp)
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = secondsLeft / 30f, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Text(text = "$secondsLeft", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Divider(modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    
                    if (entry.notes.isNotEmpty()) DetailItem(label = "Notes", value = entry.notes)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "UUID: ${entry.uuid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showHistoryDialog) {
        val history = remember(entry.uuid) { getHistory() }
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("History") },
            text = {
                if (history.isEmpty()) {
                    Text("No history available.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(history) { item ->
                            ListItem(
                                headlineContent = { Text(item.modifiedAtMs.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) }) },
                                supportingContent = { Text(item.title) },
                                modifier = Modifier.clickable { onRestoreHistory(item.uuid); showHistoryDialog = false }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String, isSensitive: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (isSensitive) {
            var isVisible by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (isVisible) value else "••••••••", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { isVisible = !isVisible }) { Text(if (isVisible) "Hide" else "Show") }
            }
        } else {
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        Divider(modifier = Modifier.padding(top = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val autofillEnabled by settingsManager.autofillEnabled.collectAsState(initial = false)
    val biometricUnlockEnabled by settingsManager.biometricUnlockEnabled.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_dialog_close), contentDescription = "Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Autofill Service", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Enable system-wide autofill integration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autofillEnabled, onCheckedChange = { isChecked -> coroutineScope.launch { settingsManager.setAutofillEnabled(isChecked) } })
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Biometric Unlock", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Use fingerprint or face unlock", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = biometricUnlockEnabled, onCheckedChange = { isChecked -> coroutineScope.launch { settingsManager.setBiometricUnlockEnabled(isChecked) } })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryScreen(entry: EntryDetails?, onSave: (String, String, String, String, String) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    var title by remember { mutableStateOf(entry?.title ?: "") }
    var username by remember { mutableStateOf(entry?.username ?: "") }
    var password by remember { mutableStateOf(entry?.password ?: "") }
    var url by remember { mutableStateOf(entry?.url ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    Scaffold(topBar = { TopAppBar(title = { Text(if (entry == null) "Add Entry" else "Edit Entry") }, navigationIcon = { IconButton(onClick = onCancel) { Icon(painterResource(R.drawable.ic_dialog_close), contentDescription = "Cancel") } }, actions = { IconButton(onClick = { onSave(title, username, password, url, notes) }) { Icon(painterResource(R.drawable.ic_dialog_ok), contentDescription = "Save") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onSave(title, username, password, url, notes) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(painterResource(R.drawable.ic_dialog_ok), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (entry == null) "Add Entry" else "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
