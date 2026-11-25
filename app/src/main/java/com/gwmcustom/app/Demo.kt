package com.gwmcustom.app

enum class Vehicle(val title: String) {
    JOLION("Haval Jolion"), H7("Haval H7"), H6("Haval H6"),
    TANK_300("Tank 300"), TANK_500("Tank 500")
}

enum class Module(val title: String) {
    HUT("Head unit"), CEM("Central electronics")
}

enum class Setting(
    val module: Module,
    val title: String,
    val description: String,
    val default: Boolean = false,
    val expert: Boolean = false
) {
    STARTUP_ANIMATION(Module.HUT, "Startup animation", "Show an animation when the demo head unit starts.", true),
    TOUCH_SOUNDS(Module.HUT, "Touch sounds", "Play a sound on demo head unit taps."),
    SERVICE_MENU(Module.HUT, "Service menu shortcut", "Show a simulated service menu shortcut.", expert = true),
    WELCOME_LIGHTS(Module.CEM, "Welcome lights", "Turn on the demo welcome lights when unlocking.", true),
    LOCK_CONFIRMATION(Module.CEM, "Lock confirmation", "Flash the demo lights when locking."),
    COURTESY_TIMER(Module.CEM, "Extended courtesy lights", "Use a longer demo courtesy light timer.", expert = true)
}

data class Change(val time: Long, val description: String)
data class Backup(val time: Long, val values: Map<Setting, Boolean>)

data class DemoProfile(
    val values: Map<Setting, Boolean> = emptyMap(),
    val backups: List<Backup> = emptyList(),
    val history: List<Change> = emptyList()
) {
    fun value(setting: Setting): Boolean = values[setting] ?: setting.default

    fun edit(setting: Setting, enabled: Boolean, now: Long): DemoProfile {
        if (value(setting) == enabled) return this
        val before = if (enabled) "Off" else "On"
        val after = if (enabled) "On" else "Off"
        return copy(
            values = values + (setting to enabled),
            history = record(now, "${setting.module.name} · ${setting.title}: $before → $after")
        )
    }

    fun save(now: Long): DemoProfile = copy(
        backups = listOf(Backup(now, Setting.entries.associateWith(::value))) + backups,
        history = record(now, "Saved a demo backup")
    )

    fun restore(backup: Backup, now: Long): DemoProfile {
        require(backup in backups)
        return copy(values = backup.values, history = record(now, "Restored a demo backup"))
    }

    private fun record(now: Long, description: String) =
        (listOf(Change(now, description)) + history).take(200)
}
