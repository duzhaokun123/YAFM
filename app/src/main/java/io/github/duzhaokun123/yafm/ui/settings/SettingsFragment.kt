package io.github.duzhaokun123.yafm.ui.settings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.utils.Freezeit
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date

class SettingsFragment: PreferenceFragmentCompat() {
    companion object {
        const val TAG = "Settings"

        const val CLUSTER_BIND = 1
        const val FREEZE_TIMEOUT = 2
        const val WAKEUP_TIMEOUT = 3
        const val TERMINATE_TIMEOUT = 4
        const val MODE = 5
        const val BATTERY_MONITOR = 13
        const val BATTERY_FIX = 14
        const val KILL_MSF = 15
        const val LMK_ADJUST = 16
        const val DOZE = 17
    }

    var lastTimestamp = System.currentTimeMillis()
    var settingsVar = ByteArray(256)
    var indexForHandler = 0

    override fun onResume() {
        super.onResume()
        runIO { Freezeit.freezeitTask(Freezeit.getSettings, null, handler) }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val response = msg.data.getByteArray("response")
            if (response == null || response.size != 256) {
                val errorTips = "handleMessage: 设置数据获取失败"
                TipUtil.showTip(context, errorTips)
                Log.e(TAG, errorTips)
                return
            }
            settingsVar = Arrays.copyOf(response, response.size)
            refresh()
        }
    }

    private val seekbarHandler = object: Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) {
                val errorTips = "handleMessage: seekbarHandler回应失败"
                TipUtil.showTip(context, errorTips)
                Log.e(TAG, errorTips)
                return
            }
            val res = String(response, StandardCharsets.UTF_8)
            if (res == "success") {
                TipUtil.showTip(context, R.string.success)
            } else {
                TipUtil.showToast("设置失败 $res")
                requireActivity().recreate()
            }
        }
    }

    private fun refresh() {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            putString("bind_core", settingsVar[CLUSTER_BIND].toString())
            putInt("timeout_freeze", settingsVar[FREEZE_TIMEOUT].toInt())
            putInt("timeout_wakeup", settingsVar[WAKEUP_TIMEOUT].toInt())
            putInt("timeout_terminate", settingsVar[TERMINATE_TIMEOUT].toInt())
            putString("freeze_mode", settingsVar[MODE].toString())
            putBoolean("battery", settingsVar[BATTERY_MONITOR].toInt() != 0)
            putBoolean("current", settingsVar[BATTERY_FIX].toInt() != 0)
            putBoolean("kill_msf", settingsVar[KILL_MSF].toInt() != 0)
            putBoolean("lmk", settingsVar[LMK_ADJUST].toInt() != 0)
            putBoolean("doze", settingsVar[DOZE].toInt() != 0)
        }
        preferenceScreen = null
        addPreferencesFromResource(R.xml.preference_main)

        findPreference<DropDownPreference>("bind_core")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = CLUSTER_BIND
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), (newValue as String).toInt().toByte()), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SeekBarPreference>("timeout_freeze")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = FREEZE_TIMEOUT
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), (newValue as Int).toByte()), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SeekBarPreference>("timeout_wakeup")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = WAKEUP_TIMEOUT
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), (newValue as Int).toByte()), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SeekBarPreference>("timeout_terminate")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = TERMINATE_TIMEOUT
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), (newValue as Int).toByte()), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<DropDownPreference>("freeze_mode")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = MODE
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), (newValue as String).toInt().toByte()), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SwitchPreferenceCompat>("battery")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = BATTERY_MONITOR
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), if (newValue as Boolean) 1 else 0), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SwitchPreferenceCompat>("current")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = BATTERY_FIX
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), if (newValue as Boolean) 1 else 0), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SwitchPreferenceCompat>("kill_msf")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = KILL_MSF
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), if (newValue as Boolean) 1 else 0), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SwitchPreferenceCompat>("lmk")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = LMK_ADJUST
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), if (newValue as Boolean) 1 else 0), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<SwitchPreferenceCompat>("doze")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = DOZE
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), if (newValue as Boolean) 1 else 0), seekbarHandler) }
            return@setOnPreferenceChangeListener true
        }
        findPreference<Preference>("system_info")!!.setOnPreferenceClickListener {
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
                Log.e(TAG, "refreshView: $e")
                infoString.append(e)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.system_info)
                .setMessage(infoString)
                .show()
                .findViewById<TextView>(android.R.id.message)!!.setTextIsSelectable(true)
            return@setOnPreferenceClickListener true
        }
    }
}