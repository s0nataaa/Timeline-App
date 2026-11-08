package com.sonata.timeline

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState


object Store {
    val items = mutableStateListOf<Item>()

    private fun file(c: Context) = File(c.filesDir, "items.json")
    private fun date(s: String) = s.takeIf { it.isNotEmpty() }?.let(LocalDate::parse)

    fun importJson(c: Context, json: String): Boolean {
        val f = file(c)
        val old = if (f.exists()) f.readText() else null
        return try {
            f.writeText(JSONArray(json.trim()).toString())
            read(c)            // tes parse, error kalau formatnya salah
            load(c); save(c)   // save juga refresh widget
            true
        } catch (e: Exception) {
            if (old != null) f.writeText(old) else f.delete()  // balik ke data lama
            false
        }
    }

    fun read(c: Context): List<Item> {
        val f = file(c)
        if (!f.exists()) return emptyList()
        val arr = JSONArray(f.readText())
        return List(arr.length()) { n ->
            val o = arr.getJSONObject(n)
            Item(
                id = o.getString("id"),
                title = o.getString("title"),
                type = Type.valueOf(o.getString("type")),
                due = date(o.optString("due")),
                note = o.optString("note"),
                done = o.optBoolean("done"),
                lastDone = date(o.optString("lastDone"))
            )
        }
    }

    fun load(c: Context) {
        items.clear()
        items += read(c)
    }

    fun save(c: Context) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("title", it.title); put("type", it.type.name)
                put("due", it.due?.toString() ?: ""); put("note", it.note)
                put("done", it.done); put("lastDone", it.lastDone?.toString() ?: "")
            })
        }
        file(c).writeText(arr.toString())
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val app = c.applicationContext
            GlanceAppWidgetManager(app).getGlanceIds(TimelineWidget::class.java).forEach { id ->
                updateAppWidgetState(app, id) { it[longPreferencesKey("t")] = System.currentTimeMillis() }
                TimelineWidget().update(app, id)
            }
        }
    }
}