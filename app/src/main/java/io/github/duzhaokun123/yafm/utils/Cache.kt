package io.github.duzhaokun123.yafm.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object Cache {
    lateinit var pm: PackageManager
    val map = mutableMapOf<String, Pair<Drawable, CharSequence>>()

    fun init(pm: PackageManager) {
        this.pm = pm
    }

    @Synchronized
    fun getIconLabel(info: ApplicationInfo): Pair<Drawable, CharSequence> {
        val pn = info.packageName
        var r = map[pn]
        if (r == null) {
            r = pm.getApplicationIcon(pn) to pm.getApplicationLabel(info)
            map[pn] = r
        }
        return r
    }
}