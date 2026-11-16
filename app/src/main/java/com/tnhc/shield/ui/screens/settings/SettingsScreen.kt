package com.tnhc.shield.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tnhc.shield.dns.DnsServerDef
import com.tnhc.shield.dns.DnsServers
import com.tnhc.shield.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel = hiltViewModel()) {
    val dnsServerName by viewModel.dnsServerName.collectAsStateWithLifecycle()
    val dnsServerUrl by viewModel.dnsServerUrl.collectAsStateWithLifecycle()
    val autoStart by viewModel.autoStart.collectAsStateWithLifecycle()
    val autoUpdate by viewModel.autoUpdateFilters.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    var showDnsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportJsonContent by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item { SectionHeader("DNS") }

        item {
            SettingsItem(
                icon = Icons.Filled.Dns,
                title = "DNS Server",
                subtitle = if (dnsServerName == "Custom") dnsServerUrl else dnsServerName.ifEmpty { "Cloudflare" },
                onClick = { showDnsDialog = true }
            )
        }

        item { SectionHeader("Protection") }

        item {
            SettingsSwitchItem(
                icon = Icons.Filled.PlayArrow,
                title = "Start on Boot",
                subtitle = "Enable Shield automatically when device starts",
                checked = autoStart,
                onCheckedChange = viewModel::setAutoStart
            )
        }

        item {
            SettingsSwitchItem(
                icon = Icons.Filled.Update,
                title = "Auto-Update Filters",
                subtitle = "Download filter list updates every 12 hours",
                checked = autoUpdate,
                onCheckedChange = viewModel::setAutoUpdateFilters
            )
        }

        item { SectionHeader("Backup & Restore") }

        item {
            SettingsItem(
                icon = Icons.Filled.CloudUpload,
                title = "Export Configuration",
                subtitle = "Export custom rules, whitelists, and app exclusions as JSON",
                onClick = {
                    coroutineScope.launch {
                        exportJsonContent = viewModel.exportBackupJson()
                        showExportDialog = true
                    }
                }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Filled.CloudDownload,
                title = "Import Configuration",
                subtitle = "Restore custom rules and exclusions from a backup JSON",
                onClick = {
                    importJsonInput = ""
                    importStatusMessage = null
                    showImportDialog = true
                }
            )
        }

        item { SectionHeader("Updates") }

        if (updateInfo?.hasUpdate == true) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Shield v${updateInfo?.latestVersion} Available!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (!updateInfo?.releaseNotes.isNullOrBlank()) {
                            Text(
                                updateInfo?.releaseNotes ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = {
                                val url = updateInfo?.downloadUrl ?: updateInfo?.releaseUrl
                                url?.let {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Text("Download Update")
                        }
                    }
                }
            }
        }

        item {
            SettingsItem(
                icon = Icons.Filled.Sync,
                title = if (isCheckingUpdate) "Checking for updates..." else "Check for Updates",
                subtitle = "Check GitHub for the latest Shield release",
                onClick = { viewModel.checkForAppUpdates() }
            )
        }

        item { SectionHeader("About") }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Shield by TNHC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Version 1.5.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Free forever. No telemetry. Fully local processing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    Text(
                        "Part of TNHC's ecosystem alongside DevTrack and other tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDnsDialog) {
        DnsServerDialog(
            currentName = dnsServerName,
            currentUrl = dnsServerUrl,
            onSelect = { server ->
                viewModel.setDnsServer(server.url, server.name)
                showDnsDialog = false
            },
            onSelectCustom = { customUrl ->
                viewModel.setDnsServer(customUrl, "Custom")
                showDnsDialog = false
            },
            onDismiss = { showDnsDialog = false }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy this JSON configuration to backup your rules and settings:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = exportJsonContent,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Shield Backup", exportJsonContent)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your Shield JSON backup below:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importStatusMessage != null) {
                        Text(
                            importStatusMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = viewModel.importBackupJson(importJsonInput)
                            if (res.isSuccess) {
                                importStatusMessage = "Successfully imported ${res.getOrNull()} rules!"
                                kotlinx.coroutines.delay(1000)
                                showImportDialog = false
                            } else {
                                importStatusMessage = "Invalid backup format: ${res.exceptionOrNull()?.message}"
                            }
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun DnsServerDialog(
    currentName: String,
    currentUrl: String,
    onSelect: (DnsServerDef) -> Unit,
    onSelectCustom: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isCustomSelected by remember { mutableStateOf(currentName == "Custom") }
    var customUrlInput by remember { mutableStateOf(if (currentName == "Custom") currentUrl else "") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select DNS Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DnsServers.all.forEach { server ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCustomSelected = false
                                onSelect(server)
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(server.name, style = MaterialTheme.typography.bodyLarge)
                            Text(server.ipHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!isCustomSelected && server.name == currentName) {
                            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustomSelected = true }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Custom (DoH)", style = MaterialTheme.typography.bodyLarge)
                        Text("Enter your own NextDNS, AdGuard, or Pi-hole URL", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isCustomSelected) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isCustomSelected) {
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = {
                            customUrlInput = it
                            hasError = false
                        },
                        label = { Text("https://...") },
                        placeholder = { Text("https://dns.nextdns.io/abc123") },
                        isError = hasError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isCustomSelected) {
                Button(
                    onClick = {
                        val clean = customUrlInput.trim()
                        if (clean.startsWith("https://") && clean.length > 10) {
                            onSelectCustom(clean)
                        } else {
                            hasError = true
                        }
                    }
                ) {
                    Text("Save Custom")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (isCustomSelected) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
