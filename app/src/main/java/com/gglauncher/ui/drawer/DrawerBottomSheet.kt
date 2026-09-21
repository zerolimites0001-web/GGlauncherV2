package com.gglauncher.ui.drawer

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.gglauncher.data.apps.AppRepository
import com.gglauncher.databinding.SheetDrawerBinding
import com.gglauncher.ui.home.AppListAdapter
import com.gglauncher.util.AppLauncher
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DrawerBottomSheet : BottomSheetDialogFragment() {

    private var _b: SheetDrawerBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: AppListAdapter
    private var searchJob: Job? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = SheetDrawerBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adapter = AppListAdapter(
            onClick = { AppLauncher.open(requireContext(), it) },
            onLongClick = { app ->
                // Arrastar pra home: adiciona na página 0, próxima célula livre
                lifecycleScope.launch {
                    val ctx = requireContext()
                    val cur = com.gglauncher.data.layout.WorkspaceStore.load(ctx).toMutableList()
                    val used = cur.map { it.cell }.toSet()
                    val free = (0 until 20).firstOrNull { it !in used } ?: 0
                    cur += com.gglauncher.data.layout.HomeItem(app.componentName, 0, free)
                    com.gglauncher.data.layout.WorkspaceStore.save(ctx, cur)
                    android.widget.Toast.makeText(ctx, "${app.label} na tela inicial", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        val cols = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        b.drawerRecycler.layoutManager = GridLayoutManager(context, cols)
        b.drawerRecycler.setHasFixedSize(true)
        b.drawerRecycler.setItemViewCacheSize(24)
        b.drawerRecycler.adapter = adapter
        adapter.submit(AppRepository.cached)

        b.drawerSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(a: CharSequence?, b1: Int, c1: Int, d1: Int) {}
            override fun onTextChanged(a: CharSequence?, b1: Int, c1: Int, d1: Int) {}
            override fun afterTextChanged(e: android.text.Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(120) // debounce: não filtra a cada tecla em CPU fraca
                    adapter.submit(AppRepository.filter(e.toString()))
                }
            }
        })
        // Foco direto na busca (estilo OneUI: busca embaixo)
        b.drawerSearch.requestFocus()
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}
