package com.gglauncher.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.gglauncher.data.apps.AppModel

object AppLauncher {

    fun open(context: Context, app: AppModel) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .setComponent(ComponentName(app.packageName, app.className))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val opts = if (context is Activity) {
                context.window?.decorView?.let { v ->
                    android.app.ActivityOptions.makeScaleUpAnimation(
                        v, v.width / 2, v.height / 2, 0, 0
                    ).toBundle()
                }
            } else null

            if (opts != null) context.startActivity(intent, opts)
            else context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Não foi possível abrir ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }
}
