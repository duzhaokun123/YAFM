package io.github.duzhaokun123.yafm

import com.google.android.material.color.DynamicColors
import io.github.duzhaokun123.yafm.utils.Cache

lateinit var application: Application
    private set

class Application : android.app.Application() {
    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        Cache.init(packageManager)
    }
}