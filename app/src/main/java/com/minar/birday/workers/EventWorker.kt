package com.minar.birday.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventResult

class EventWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext

        val eventDao: EventDao = EventDatabase.getBirdayDataBase(appContext)!!.eventDao();
        val nextEvents: List<EventResult> = eventDao.getOrderedNextEvents()

        return Result.success()
    }
}