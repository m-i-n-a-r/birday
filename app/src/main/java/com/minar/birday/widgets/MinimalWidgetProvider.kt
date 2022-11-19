package com.minar.birday.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.minar.birday.R


class MinimalWidgetProvider : BirdayWidgetProvider() {

    override var widgetLayout
        get() = R.layout.widget_minimal
        set(_) {
            R.layout.widget_minimal
        }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, MinimalWidgetProvider::class.java)
            mgr.getAppWidgetIds(cn).forEach { appWidgetId ->
                updateAppWidget(context, mgr, appWidgetId)
            }
        }
        super.onReceive(context, intent)
    }
}