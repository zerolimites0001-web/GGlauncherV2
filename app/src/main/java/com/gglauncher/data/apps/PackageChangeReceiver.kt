package com.gglauncher.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Recarrega a lista quando qualquer pacote é instalado/removido/alterado. */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.data == null) return
        CoroutineScope(Dispatchers.IO).launch {
            AppRepository.loadApps()
        }
    }
}
