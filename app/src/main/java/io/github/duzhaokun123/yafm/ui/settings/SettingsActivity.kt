package io.github.duzhaokun123.yafm.ui.settings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.utils.maxSystemBarsDisplayCutout
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.databinding.ActivitySettingsBinding
import java.text.SimpleDateFormat
import java.util.Date

class SettingsActivity: BaseActivity<ActivitySettingsBinding>(R.layout.activity_settings) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .add(R.id.fl, SettingsFragment::class.java, null, "SettingsFragment")
            .commit()

        val infoString = StringBuilder()
        try {
            infoString.append("Build.ID: ").append(Build.ID).append('\n')
            infoString.append("Build.DISPLAY: ").append(Build.DISPLAY).append('\n')
            infoString.append("Build.PRODUCT: ").append(Build.PRODUCT).append('\n')
            infoString.append("Build.DEVICE: ").append(Build.DEVICE).append('\n')
            infoString.append("Build.BOARD: ").append(Build.BOARD).append('\n')
            infoString.append("Build.MANUFACTURER: ").append(Build.MANUFACTURER).append('\n')
            infoString.append("Build.BRAND: ").append(Build.BRAND).append('\n')
            infoString.append("Build.MODEL: ").append(Build.MODEL).append('\n')
            if (Build.VERSION.SDK_INT >= 31) {
                infoString.append("Build.SOC_MANUFACTURER: ").append(Build.SOC_MANUFACTURER)
                    .append('\n')
                infoString.append("Build.SOC_MODEL: ").append(Build.SOC_MODEL).append('\n')
                infoString.append("Build.SKU: ").append(Build.SKU).append('\n')
                infoString.append("Build.ODM_SKU: ").append(Build.ODM_SKU).append('\n')
            }
            infoString.append("Build.BOOTLOADER: ").append(Build.BOOTLOADER).append('\n')
            infoString.append("Build.HARDWARE: ").append(Build.HARDWARE).append('\n')
            infoString.append("Build.SUPPORTED_ABIS: ")
            for (abi in Build.SUPPORTED_ABIS) infoString.append('[').append(abi).append("] ")
            infoString.append('\n')
            infoString.append("Build.VERSION.INCREMENTAL: ").append(Build.VERSION.INCREMENTAL)
                .append('\n')
            infoString.append("Build.VERSION.RELEASE: ").append(Build.VERSION.RELEASE).append('\n')
            if (Build.VERSION.SDK_INT >= 30) infoString.append(
                "Build.VERSION.RELEASE_OR_CODENAME: ").append(
                Build.VERSION.RELEASE_OR_CODENAME).append('\n')
            infoString.append("Build.VERSION.BASE_OS: ").append(Build.VERSION.BASE_OS).append('\n')
            infoString.append("Build.VERSION.SECURITY_PATCH: ").append(Build.VERSION.SECURITY_PATCH)
                .append('\n')
            infoString.append("Build.VERSION.SDK_INT: ").append(Build.VERSION.SDK_INT).append('\n')
            infoString.append("Build.VERSION.CODENAME: ").append(Build.VERSION.CODENAME)
                .append('\n')
            infoString.append("Build.TYPE: ").append(Build.TYPE).append('\n')
            infoString.append("Build.TAGS: ").append(Build.TAGS).append('\n')
            infoString.append("Build.FINGERPRINT: ").append(Build.FINGERPRINT).append('\n')
            infoString.append("Build.TIME: ").append(Build.TIME).append(' ')
            @SuppressLint("SimpleDateFormat") val simpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = Date(Build.TIME)
            infoString.append(simpleDateFormat.format(date)).append('\n')
            infoString.append("Build.USER: ").append(Build.USER).append('\n')
            infoString.append("Build.HOST: ").append(Build.HOST).append('\n')
            infoString.append("RadioVersion: ").append(Build.getRadioVersion()).append('\n')
        } catch (e: Exception) {
            Log.e(SettingsFragment.TAG, "refreshView: $e")
            infoString.append(e)
        }
        baseBinding.tvSystemInfo.text = infoString
    }

    override fun onResume() {
        super.onResume()
        findViewById<RecyclerView>(androidx.preference.R.id.recycler_view).isNestedScrollingEnabled = false
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        baseBinding.tvSystemInfo.updatePadding(bottom = insets.maxSystemBarsDisplayCutout.bottom)
    }
}