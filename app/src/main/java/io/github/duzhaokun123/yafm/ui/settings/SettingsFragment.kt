package io.github.duzhaokun123.yafm.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.utils.Freezeit
import java.nio.charset.StandardCharsets
import java.util.Arrays

class SettingsFragment: PreferenceFragmentCompat() {
    companion object {
        const val TAG = "Settings"

        const val CLUSTER_BIND = 1
        const val FREEZE_TIMEOUT = 2
        const val WAKEUP_TIMEOUT = 3
        const val TERMINATE_TIMEOUT = 4
        const val MODE = 5
        const val RADICA = 10
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
            putBoolean("foreground", settingsVar[RADICA].toInt() != 0)
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
        findPreference<SwitchPreferenceCompat>("foreground")!!.setOnPreferenceChangeListener { _, newValue ->
            val newTs = System.currentTimeMillis()
            if (newTs - lastTimestamp < 1_000) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnPreferenceChangeListener false
            }
            indexForHandler = RADICA
            lastTimestamp = newTs
            runIO { Freezeit.freezeitTask(Freezeit.setSettingsVar, byteArrayOf(indexForHandler.toByte(), if (newValue as Boolean) 1 else 0), seekbarHandler) }
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
    }
}