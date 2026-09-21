package com.gglauncher.data.layout

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HomeItem(val component: String, val page: Int, val cell: Int)

object WorkspaceStore {
    private const val FILE = "workspace.json"

    suspend fun load(c: Context): List<HomeItem> = withContext(Dispatchers.IO) {
        val f = File(c.filesDir, FILE)
        if (!f.exists()) return@withContext emptyList()
        try {
            val arr = JSONArray(f.readText())
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                HomeItem(o.getString("c"), o.getInt("p"), o.getInt("cell"))
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun save(c: Context, items: List<HomeItem>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("c", it.component).put("p", it.page).put("cell", it.cell)) }
        File(c.filesDir, FILE).writeText(arr.toString())
    }
}
