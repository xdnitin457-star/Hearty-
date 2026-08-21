package com.ourspace.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.util.concurrent.Executors

class OurSpaceWidgetProvider : AppWidgetProvider() {
    private val exec = Executors.newCachedThreadPool()

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> update(context, manager, id) }
    }

    private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget)
        val prefs = context.getSharedPreferences("ourspace", Context.MODE_PRIVATE)
        val token = prefs.getString("access", null)
        val uid = prefs.getString("uid", null)
        val role = prefs.getString("role", "boy")
        val coupleId = prefs.getString("couple", null)

        if (role == "girl") {
            views.setTextViewText(R.id.widgetTitle, "HEARTLY 🎀💗")
            views.setInt(R.id.widgetRoot, "setBackgroundColor", Color.rgb(100, 35, 65))
            views.setTextColor(R.id.widgetNote, Color.WHITE)
            views.setTextColor(R.id.widgetTime, Color.rgb(255, 220, 235))
        } else {
            views.setTextViewText(R.id.widgetTitle, "HEARTLY 🦇💙")
        }

        val launch = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, launch)
        manager.updateAppWidget(widgetId, views)

        if (token.isNullOrBlank() || uid.isNullOrBlank() || coupleId.isNullOrBlank()) {
            views.setTextViewText(R.id.widgetNote, "Open Heartly and connect your couple ❤️")
            views.setTextViewText(R.id.widgetTime, "Tap to open")
            manager.updateAppWidget(widgetId, views)
            return
        }

        exec.execute {
            try {
                val s = Session(token, prefs.getString("refresh","") ?: "", uid)
                val notes = Api.listNotes(s, coupleId)
                val n = notes.firstOrNull()
                if (n != null) {
                    val text = n.text?.take(100) ?: "📸 A new photo was shared."
                    views.setTextViewText(R.id.widgetNote, text)
                    views.setTextViewText(R.id.widgetTime, "Latest moment • ${n.createdAt.replace("T"," ").take(16)}")
                } else {
                    views.setTextViewText(R.id.widgetNote, "No shared moments yet. Send the first one 💙")
                    views.setTextViewText(R.id.widgetTime, "Tap to open")
                }
                manager.updateAppWidget(widgetId, views)
            } catch (_: Exception) {
                views.setTextViewText(R.id.widgetNote, "Open the app to refresh your space.")
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}
