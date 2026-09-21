package com.gglauncher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.*
import java.text.Normalizer
import java.util.concurrent.Executors

data class AppModel(
    val packageName: String,
    val className: String,
    val label: String
) {
    val componentName: String get() = "$packageName/$className"
    val normalized: String by lazy { unaccent(label.lowercase()) }
}

private fun unaccent(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")

object AppRepository {

    private lateinit var appContext: Context
    fun init(c: Context) { appContext = c.applicationContext }

    private val listeners = mutableListOf<() -> Unit>()
    fun addListener(l: () -> Unit) { listeners += l }

    @Volatile var cached: List<AppModel> = emptyList()
        private set

    val iconCache: LruCache<String, Drawable> by lazy {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        object : LruCache<String, Drawable>(maxKb / 8) {
            override fun sizeOf(k: String, v: Drawable) = 48
        }
    }

    // Multithread: Núcleos fracos (até 4) — pool fixo de 4 threads p/ ícones + load
    val ioPool = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
    // Carrega lista principal em Default (rápido), ícones sob demanda no pool
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Puxa lista de apps RÁPIDO: query + sort off-main, sem loadIcon aqui. */
    suspend fun loadApps(): List<AppModel> = withContext(Dispatchers.Default) {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val raw = pm.queryIntentActivities(intent, 0)
        // Parse em paralelo (4 threads) — cada item é barato mas soma em CPU fraca
        val out = raw.chunked((raw.size / 4).coerceAtLeast(1)).map { chunk ->
            async(ioPool) {
                chunk.map {
                    AppModel(
                        it.activityInfo.packageName,
                        it.activityInfo.name,
                        it.loadLabel(pm).toString()
                    )
                }
            }
        }.awaitAll().flatten().sortedBy { it.normalized }
        cached = out
        // Pré-aquece cache dos 20 primeiros ícones em background (não bloqueia UI)
        scope.launch(ioPool) {
            out.take(20).forEach { getIcon(it) }
        }
        withContext(Dispatchers.Main) { listeners.forEach { it() } }
        out
    }

    fun filter(query: String): List<AppModel> {
        if (query.isBlank()) return cached
        val q = unaccent(query.lowercase().trim())
        return cached.filter { it.normalized.contains(q) }
    }

    fun getIcon(app: AppModel): Drawable? {
        iconCache.get(app.componentName)?.let { return it }
        return try {
            val pm = appContext.packageManager
            val info = pm.getActivityInfo(
                android.content.ComponentName(app.packageName, app.className), 0
            )
            val d = info.loadIcon(pm)
            iconCache.put(app.componentName, d)
            d
        } catch (e: PackageManager.NameNotFoundException) { null }
    }

    fun evict(packageName: String) {
        // Remove ícones do pacote desinstalado
        for (m in snapshotKeys().filter { it.startsWith("$packageName/") }) iconCache.remove(m)
    }

    private fun snapshotKeys(): List<String> = cached.map { it.componentName }
}
