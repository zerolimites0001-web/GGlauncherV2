package com.gglauncher.ui.home

import android.content.ClipData
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gglauncher.data.apps.AppRepository
import com.gglauncher.data.layout.HomeItem
import com.gglauncher.data.layout.WorkspaceStore
import com.gglauncher.databinding.ActivityLauncherBinding
import com.gglauncher.theming.ThemeManager
import com.gglauncher.ui.drawer.DrawerBottomSheet
import com.gglauncher.util.AppLauncher
import com.gglauncher.util.SwipeListener
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private var items = mutableListOf<HomeItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.apply())
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wallpaper do sistema visível: fundo transparente
        window.setBackgroundDrawable(null)
        binding.root.background = null

        // Swipe up = abre gaveta
        binding.root.setOnTouchListener(object : SwipeListener(this) {
            override fun onSwipeUp() { DrawerBottomSheet().show(supportFragmentManager, "drawer") }
            override fun onSwipeDown() { expandNotifications() }
        })

        binding.settingsBtn.setOnClickListener {
            startActivity(android.content.Intent(this, com.gglauncher.ui.settings.SettingsActivity::class.java))
        }
        binding.drawerFab.setOnClickListener {
            DrawerBottomSheet().show(supportFragmentManager, "drawer")
        }

        lifecycleScope.launch {
            val apps = AppRepository.loadApps()
            items = WorkspaceStore.load(this@LauncherActivity).toMutableList()
            if (items.isEmpty()) {
                // Primeira vez: fixa 8 apps mais comuns na home
                items = apps.take(8).mapIndexed { i, a -> HomeItem(a.componentName, 0, i) }.toMutableList()
                WorkspaceStore.save(this@LauncherActivity, items)
            }
            renderHome()
        }
        AppRepository.addListener { lifecycleScope.launch { renderHome() } }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Girau a tela: só reajusta colunas e redesenha (layout já salvo por cell, sem perder nada)
        binding.workspace.columnCount = if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        renderHome()
    }

    private fun renderHome() {
        // Auto-arruma: colunas conforme orientação atual
        binding.workspace.columnCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        binding.workspace.removeAllViews()
        val byComp = AppRepository.cached.associateBy { it.componentName }
        items.forEachIndexed { idx, item ->
            val app = byComp[item.component] ?: return@forEachIndexed
            val view = layoutInflater.inflate(com.gglauncher.R.layout.item_app, binding.workspace, false)
            val icon = view.findViewById<android.widget.ImageView>(com.gglauncher.R.id.icon)
            val label = view.findViewById<android.widget.TextView>(com.gglauncher.R.id.label)
            label.text = app.label
            // Ícone no pool multithread (4 threads), sem travar UI
            lifecycleScope.launch(AppRepository.ioPool) {
                val d = AppRepository.getIcon(app)
                launch(kotlinx.coroutines.Dispatchers.Main) { icon.setImageDrawable(d) }
            }
            view.setOnClickListener { AppLauncher.open(this, app) }

            // LONG-PRESS = arrastar pra reorganizar
            view.setOnLongClickListener { v ->
                val data = ClipData.newPlainText("idx", idx.toString())
                val shadow = View.DragShadowBuilder(v)
                if (Build.VERSION.SDK_INT >= 24) v.startDragAndDrop(data, shadow, null, 0)
                else @Suppress("DEPRECATION") v.startDrag(data, shadow, null, 0)
                true
            }
            // DROP sobre outro ícone = troca de posição
            view.setOnDragListener { _, e ->
                if (e.action == DragEvent.ACTION_DROP) {
                    val from = e.clipData.getItemAt(0).text.toString().toInt()
                    val tmp = items[from]; items[from] = items[idx]; items[idx] = tmp
                    lifecycleScope.launch { WorkspaceStore.save(this@LauncherActivity, items) }
                    renderHome()
                    true
                } else true
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            binding.workspace.addView(view, lp)
        }
    }

    @Suppress("DEPRECATION")
    private fun expandNotifications() {
        try {
            val sb = getSystemService("statusbar")
            val m = Class.forName("android.app.StatusBarManager")
            m.getMethod("expandNotificationsPanel").invoke(sb)
        } catch (e: Exception) { /* sem root/sem permissão: ignora */ }
    }

    override fun onBackPressed() { /* launcher: ignora */ }
}
