package com.gglauncher.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gglauncher.data.apps.AppModel
import com.gglauncher.data.apps.AppRepository
import com.gglauncher.databinding.ItemAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListAdapter(
    private val onClick: (AppModel) -> Unit,
    private val onLongClick: (AppModel) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    private var items: List<AppModel> = emptyList()

    inner class VH(val b: ItemAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.b.label.text = app.label
        holder.b.icon.setImageDrawable(null) // evita ícone reciclado errado
        holder.b.root.setOnClickListener { onClick(app) }
        holder.b.root.setOnLongClickListener { onLongClick(app); true }

        // Ícone carrega em background + cache (não trava o scroll)
        CoroutineScope(Dispatchers.IO).launch {
            val drawable = AppRepository.getIcon(app)
            withContext(Dispatchers.Main) {
                if (holder.bindingAdapterPosition == position) {
                    holder.b.icon.setImageDrawable(drawable)
                }
            }
        }
    }

    fun submit(newList: List<AppModel>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) =
                items[o].componentName == newList[n].componentName
            override fun areContentsTheSame(o: Int, n: Int) =
                items[o] == newList[n]
        })
        items = newList
        diff.dispatchUpdatesTo(this)
    }
}
