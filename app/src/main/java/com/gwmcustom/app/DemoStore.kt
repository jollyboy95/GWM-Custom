package com.gwmcustom.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DemoStore(context: Context) {
    private val prefs = context.getSharedPreferences("demo_v1", Context.MODE_PRIVATE)
    var vehicle: Vehicle
        get() = Vehicle.entries.firstOrNull { it.name == prefs.getString("vehicle", null) }
            ?: Vehicle.JOLION
        set(value) { prefs.edit().putString("vehicle", value.name).apply() }

    var expert: Boolean
        get() = prefs.getBoolean("expert", false)
        set(value) { prefs.edit().putBoolean("expert", value).apply() }

    fun load(vehicle: Vehicle): DemoProfile {
        val saved = prefs.getString(vehicle.name, null) ?: return DemoProfile()
        val root = JSONObject(saved)
        return DemoProfile(
            values = decode(root.getJSONObject("values")),
            backups = root.getJSONArray("backups").objects().map {
                Backup(it.getLong("time"), decode(it.getJSONObject("values")))
            },
            history = root.getJSONArray("history").objects().map {
                Change(it.getLong("time"), it.getString("description"))
            }
        )
    }

    fun save(vehicle: Vehicle, profile: DemoProfile) {
        val root = JSONObject()
            .put("values", encode(profile.values))
            .put("backups", JSONArray(profile.backups.map {
                JSONObject().put("time", it.time).put("values", encode(it.values))
            }))
            .put("history", JSONArray(profile.history.map {
                JSONObject().put("time", it.time).put("description", it.description)
            }))
        prefs.edit().putString(vehicle.name, root.toString()).apply()
    }

    private fun encode(values: Map<Setting, Boolean>) =
        JSONObject(values.mapKeys { it.key.name })

    private fun decode(json: JSONObject) = Setting.entries.filter { json.has(it.name) }
        .associateWith { json.getBoolean(it.name) }

    private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
}
