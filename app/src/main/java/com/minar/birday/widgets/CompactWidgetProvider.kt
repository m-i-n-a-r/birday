package com.minar.birday.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.minar.birday.R


class CompactWidgetProvider : BirdayWidgetProvider() {

    override var widgetLayout
        get() = R.layout.widget_compact
        set(_) {
            R.layout.widget_compact
        }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            // Deprecated since API 29 but still the only signal for a wallpaper change: activities are
            // recreated automatically when the Monet palette changes, widgets are not
            @Suppress("DEPRECATION") Intent.ACTION_WALLPAPER_CHANGED -> {
                val mgr = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, CompactWidgetProvider::class.java)
                val ids = mgr.getAppWidgetIds(cn)
                ids.forEach { id -> updateAppWidget(context, mgr, id) }
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.compactWidgetList)
            }
        }
        super.onReceive(context, intent)
    }

    // Recalculate visible rows when the widget is resized
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}
