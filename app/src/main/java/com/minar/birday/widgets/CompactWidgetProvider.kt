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
        val action = intent.action
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, CompactWidgetProvider::class.java)
            mgr.getAppWidgetIds(cn).forEach { appWidgetId ->
                updateAppWidget(context, mgr, appWidgetId)
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
