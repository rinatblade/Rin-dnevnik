package kz.rin.dnevnik

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MediaItem(val file: String, val isVideo: Boolean)

data class Entry(val date: String, val text: String, val media: List<MediaItem>) {
    val isEmpty: Boolean get() = text.isBlank() && media.isEmpty()
}

/** Хранилище дневника: один JSON-файл на каждый день + папка с медиа этого дня. */
class Repo(private val ctx: Context) {
    private val dir = File(ctx.filesDir, "days").apply { mkdirs() }

    fun mediaDir(date: String): File = File(ctx.filesDir, "media/$date").apply { mkdirs() }

    fun mediaFile(date: String, name: String): File = File(mediaDir(date), name)

    fun load(date: String): Entry {
        val f = File(dir, "$date.json")
        if (!f.exists()) return Entry(date, "", emptyList())
        return try {
            val o = JSONObject(f.readText())
            val arr = o.optJSONArray("media") ?: JSONArray()
            val list = ArrayList<MediaItem>()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                list.add(MediaItem(m.getString("file"), m.optBoolean("video", false)))
            }
            Entry(date, o.optString("text", ""), list)
        } catch (e: Exception) {
            Entry(date, "", emptyList())
        }
    }

    fun save(e: Entry) {
        val f = File(dir, "${e.date}.json")
        if (e.isEmpty) {
            f.delete()
            return
        }
        val o = JSONObject()
        o.put("text", e.text)
        val arr = JSONArray()
        e.media.forEach {
            arr.put(JSONObject().put("file", it.file).put("video", it.isVideo))
        }
        o.put("media", arr)
        f.writeText(o.toString())
    }

    fun delete(date: String) {
        File(dir, "$date.json").delete()
        File(ctx.filesDir, "media/$date").deleteRecursively()
    }

    /** Все даты, у которых что-то записано, от новых к старым. */
    fun listDates(): List<String> =
        (dir.listFiles() ?: emptyArray())
            .filter { it.name.endsWith(".json") }
            .map { it.name.removeSuffix(".json") }
            .sortedDescending()
}

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("rin_dnevnik", Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }

    var fontIndex: Int
        get() = sp.getInt("font", 0)
        set(v) { sp.edit().putInt("font", v).apply() }

    var lastWeather: String?
        get() = sp.getString("weather", null)
        set(v) { sp.edit().putString("weather", v).apply() }
}
