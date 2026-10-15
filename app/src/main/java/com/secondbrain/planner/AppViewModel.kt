package com.secondbrain.planner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FocusTimer(private val scope: CoroutineScope) {
    // پومودورو
    var mode by mutableIntStateOf(0) // 0 تمرکز، 1 استراحت
    var focusMin by mutableIntStateOf(25)
    var breakMin by mutableIntStateOf(5)
    var remaining by mutableIntStateOf(25 * 60)
    var running by mutableStateOf(false)
    var sessions by mutableIntStateOf(0)
    private var job: Job? = null

    fun toggle() {
        if (running) {
            job?.cancel()
            running = false
        } else {
            running = true
            job = scope.launch {
                while (remaining > 0) {
                    delay(1000)
                    remaining--
                }
                running = false
                if (mode == 0) sessions++
                next()
            }
        }
    }

    fun next() {
        job?.cancel()
        running = false
        mode = if (mode == 0) 1 else 0
        remaining = (if (mode == 0) focusMin else breakMin) * 60
    }

    fun reset() {
        job?.cancel()
        running = false
        remaining = (if (mode == 0) focusMin else breakMin) * 60
    }

    fun setFocus(m: Int) {
        focusMin = m.coerceIn(5, 120)
        if (!running && mode == 0) remaining = focusMin * 60
    }

    fun setBreak(m: Int) {
        breakMin = m.coerceIn(1, 60)
        if (!running && mode == 1) remaining = breakMin * 60
    }

    // کرنومتر
    var swMs by mutableLongStateOf(0L)
    var swRunning by mutableStateOf(false)
    val laps = mutableStateListOf<Long>()
    private var swJob: Job? = null

    fun swToggle() {
        if (swRunning) {
            swJob?.cancel()
            swRunning = false
        } else {
            swRunning = true
            val base = swMs
            val start = System.currentTimeMillis()
            swJob = scope.launch {
                while (true) {
                    swMs = base + (System.currentTimeMillis() - start)
                    delay(50)
                }
            }
        }
    }

    fun swReset() {
        swJob?.cancel()
        swRunning = false
        swMs = 0
        laps.clear()
    }

    fun swLap() {
        if (swRunning) laps.add(0, swMs)
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val settings = AppSettings(app)
    val auth = AuthStore(app)
    private val dao = AppDb.get(app).dao()

    val items: StateFlow<List<Item>> =
        dao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val logs: StateFlow<List<HabitLog>> =
        dao.logs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val timer = FocusTimer(viewModelScope)

    fun save(i: Item) {
        viewModelScope.launch { dao.upsert(i) }
    }

    fun delete(i: Item) {
        viewModelScope.launch {
            dao.delete(i)
            if (i.type == Kind.HABIT.name) dao.clearLogs(i.id)
        }
    }

    fun toggleDone(i: Item) {
        val k = Kind.valueOf(i.type)
        val nd = !i.done
        save(i.copy(done = nd, progress = if (k.hasProgress) (if (nd) 100 else 0) else i.progress))
    }

    fun toggleLog(habitId: Long, day: Long) {
        viewModelScope.launch {
            val l = HabitLog(habitId, day)
            if (logs.value.contains(l)) dao.delLog(l) else dao.addLog(l)
        }
    }

    fun wipeAll() {
        viewModelScope.launch {
            dao.clearItems()
            dao.clearAllLogs()
        }
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "secondbrain")
        root.put("version", 1)
        val a = JSONArray()
        items.value.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id).put("type", it.type).put("title", it.title)
                    .put("note", it.note).put("done", it.done)
                    .put("day", it.day ?: JSONObject.NULL)
                    .put("amount", it.amount).put("category", it.category)
                    .put("priority", it.priority).put("progress", it.progress)
                    .put("createdAt", it.createdAt)
            )
        }
        root.put("items", a)
        val l = JSONArray()
        logs.value.forEach { l.put(JSONObject().put("h", it.habitId).put("d", it.day)) }
        root.put("logs", l)
        return root.toString()
    }

    fun importJson(text: String): Boolean {
        return try {
            val root = JSONObject(text)
            val a = root.getJSONArray("items")
            val newItems = ArrayList<Item>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                newItems.add(
                    Item(
                        id = o.getLong("id"),
                        type = o.getString("type"),
                        title = o.getString("title"),
                        note = o.optString("note", ""),
                        done = o.optBoolean("done", false),
                        day = if (o.isNull("day")) null else o.getLong("day"),
                        amount = o.optDouble("amount", 0.0),
                        category = o.optString("category", ""),
                        priority = o.optInt("priority", 1),
                        progress = o.optInt("progress", 0),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            val l = root.getJSONArray("logs")
            val newLogs = ArrayList<HabitLog>()
            for (i in 0 until l.length()) {
                val o = l.getJSONObject(i)
                newLogs.add(HabitLog(o.getLong("h"), o.getLong("d")))
            }
            viewModelScope.launch {
                dao.clearItems()
                dao.clearAllLogs()
                newItems.forEach { dao.upsert(it) }
                newLogs.forEach { dao.addLog(it) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
