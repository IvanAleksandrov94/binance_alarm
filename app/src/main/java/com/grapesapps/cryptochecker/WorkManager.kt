package com.grapesapps.cryptochecker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class RequestWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {


        return Result.success()
    }

}