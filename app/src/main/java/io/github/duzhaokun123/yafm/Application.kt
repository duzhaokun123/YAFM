package io.github.duzhaokun123.yafm

import com.google.android.material.color.DynamicColors

lateinit var application: Application
    private set

class Application : android.app.Application() {
    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}