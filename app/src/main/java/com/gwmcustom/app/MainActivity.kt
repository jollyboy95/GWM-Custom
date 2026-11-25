package com.gwmcustom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private lateinit var discovery: BleDiscovery
    private val requestScan = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (discovery.hasPermissions()) discovery.start() else discovery.permissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        discovery = BleDiscovery(this)
        val store = DemoStore(this)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme()) {
                darkColorScheme(primary = Color(0xFF91D7C6), primaryContainer = Color(0xFF164E44))
            } else {
                lightColorScheme(primary = Color(0xFF1E6658), primaryContainer = Color(0xFFBCEEE1))
            }
            MaterialTheme(colorScheme = colors) {
                CustomApp(store, discovery) {
                    if (discovery.hasPermissions()) discovery.start()
                    else requestScan.launch(discovery.permissions)
                }
            }
        }
    }

    override fun onStop() {
        discovery.clear()
        super.onStop()
    }
}

private enum class Screen(val title: String) {
    GARAGE("Garage"), BLUETOOTH("Bluetooth"), HISTORY("History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomApp(store: DemoStore, discovery: BleDiscovery, onScan: () -> Unit) {
    var screen by rememberSaveable { mutableStateOf(Screen.GARAGE) }
    var vehicle by remember { mutableStateOf(store.vehicle) }
    var profile by remember(vehicle) { mutableStateOf(store.load(vehicle)) }
    var expert by remember { mutableStateOf(store.expert) }

    fun update(updated: DemoProfile) {
        store.save(vehicle, updated)
        profile = updated
    }

    Scaffold(topBar = {
        Column(Modifier.statusBarsPadding()) {
            HorizontalTabs(Screen.entries.map { it.title }, screen.ordinal) {
                if (screen == Screen.BLUETOOTH && it != screen.ordinal) discovery.clear()
                screen = Screen.entries[it]
            }
        }
    }) { padding ->
        key(screen) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (screen) {
                    Screen.GARAGE -> item {
                        Garage(vehicle, profile, expert, {
                            store.vehicle = it
                            vehicle = it
                        }, {
                            store.expert = it
                            expert = it
                        }, { setting, enabled ->
                            update(profile.edit(setting, enabled, System.currentTimeMillis()))
                        })
                    }
                    Screen.BLUETOOTH -> {
                        item {
                            Heading("Nearby adapters", "Discovery only. The app does not pair with or connect to adapters.")
                            Spacer(Modifier.height(16.dp))
                            Text(discovery.status)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { if (discovery.scanning) discovery.stop() else onScan() }) {
                                Text(if (discovery.scanning) "Stop scan" else "Scan nearby")
                            }
                            if (discovery.devices.isNotEmpty()) {
                                Text("ThinkDiag matches are based on the advertised name.")
                            }
                        }
                        items(discovery.devices) { device ->
                            ListItem(
                                headlineContent = { Text(device.name ?: "Unnamed device") },
                                supportingContent = if (device.thinkDiag) {
                                    { Text("Possible ThinkDiag adapter") }
                                } else null,
                                trailingContent = { Text("${device.signal} dBm") }
                            )
                        }
                    }
                    Screen.HISTORY -> {
                        item {
                            Heading("Backups & history", "${vehicle.title} · Demo configuration")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { update(profile.save(System.currentTimeMillis())) }) {
                                Text("Save demo backup")
                            }
                        }
                        item {
                            SectionTitle("Backups")
                            if (profile.backups.isEmpty()) Text("No backups saved for this profile.")
                        }
                        items(profile.backups) { backup ->
                            BackupRow(backup) { update(profile.restore(backup, System.currentTimeMillis())) }
                        }
                        item {
                            SectionTitle("Recent changes")
                            if (profile.history.isEmpty()) Text("Changes to this profile will appear here.")
                        }
                        items(profile.history) { change ->
                            ListItem(
                                headlineContent = { Text(change.description) },
                                supportingContent = { Text(dateLabel(change.time)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorizontalTabs(titles: List<String>, selected: Int, select: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected) {
        titles.forEachIndexed { index, title ->
            Tab(selected == index, onClick = { select(index) }, text = { Text(title) })
        }
    }
}

@Composable
private fun Garage(
    vehicle: Vehicle,
    profile: DemoProfile,
    expert: Boolean,
    selectVehicle: (Vehicle) -> Unit,
    selectExpert: (Boolean) -> Unit,
    edit: (Setting, Boolean) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var module by rememberSaveable { mutableStateOf(Module.HUT) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Heading("GWM Custom", "v0.1.0 · Demo mode")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Vehicle profile", style = MaterialTheme.typography.labelLarge)
                Box {
                    FilledTonalButton(onClick = { menu = true }) { Text(vehicle.title + " ▾") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Vehicle.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.title) }, onClick = {
                                selectVehicle(option)
                                menu = false
                            })
                        }
                    }
                }
                Text("Settings are simulated and saved on this phone.")
            }
        }
        ToggleRow("Expert mode", "Show additional demo settings.", expert, selectExpert)
        HorizontalTabs(Module.entries.map { it.name }, module.ordinal) { module = Module.entries[it] }
        SectionTitle(module.title)
        Setting.entries.filter { it.module == module && (expert || !it.expert) }.forEach { setting ->
            ToggleRow(setting.title, setting.description, profile.value(setting)) { edit(setting, it) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, description: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value, role = Role.Switch, onValueChange = change)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = value, onCheckedChange = null)
    }
}

@Composable
private fun BackupRow(backup: Backup, restore: () -> Unit) {
    var confirm by remember(backup) { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(dateLabel(backup.time)) },
        trailingContent = { TextButton(onClick = { confirm = true }) { Text("Restore") } }
    )
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Restore demo backup?") },
        text = { Text("This replaces the current settings for this demo profile.") },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                restore()
            }) { Text("Restore") }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
    )
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge)
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionTitle(title: String) = Text(title, style = MaterialTheme.typography.titleLarge)

private fun dateLabel(time: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(time))
