package io.github.duzhaokun123.yafm.ui.main

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.androidapptemplate.bases.BaseFragment
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.databinding.FragmentLogBinding
import io.github.duzhaokun123.yafm.utils.Freezeit
import java.nio.charset.StandardCharsets
import java.util.Timer
import java.util.TimerTask

class LogFragment : BaseFragment<FragmentLogBinding>(R.layout.fragment_log), MenuProvider {
    companion object {
        const val TAG = "LogFragment"
    }

    private lateinit var timer: Timer
    private var lastLogLen = 0

    private var handler: Handler? = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")

            if (response == null || response.isEmpty()) {
                baseBinding.tvLog.text = "null"
                return
            }

            if (lastLogLen == response.size) return

            lastLogLen = response.size

            baseBinding.tvLog.movementMethod = ScrollingMovementMethod.getInstance() //流畅滑动
            baseBinding.tvLog.text = String(response, StandardCharsets.UTF_8)

            baseBinding.vBottom.requestFocus() //请求焦点，直接到日志底部
            baseBinding.vBottom.clearFocus()
        }
    }

    private val appNameHandler: Handler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) {
                val errorTips = getString(R.string.freezeit_offline)
                TipUtil.showTip(context, errorTips)
                Log.e(TAG, errorTips)
                return
            }
            val res = String(response, StandardCharsets.UTF_8)
            if (res == "success") {
                TipUtil.showTip(context, R.string.update_success)
            } else {
                val errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]"
                TipUtil.showTip(context, errorTips)
                Log.e(TAG, errorTips)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireBaseActivity().addMenuProvider(this, viewLifecycleOwner)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.log_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.clear_log -> {
                runIO { Freezeit.freezeitTask(Freezeit.clearLog, null, handler) }
                return true
            }

            R.id.printf_freeze -> {
                runIO { Freezeit.freezeitTask(Freezeit.printFreezerProc, null, handler) }
                return true
            }

            R.id.help_log -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setView(ImageView(requireContext()).apply {
                        setImageResource(R.drawable.help_log)
                    }).setTitle(R.string.help)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return true
            }

            R.id.update_label -> {
                TipUtil.showTip(context, R.string.update_start)
                runIO {
                    val appName = StringBuilder()

                    val pm = requireContext().packageManager
                    val applicationsInfo =
                        pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
                    for (appInfo in applicationsInfo) {
                        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM > 0) continue
                        if (appInfo.uid < 10000) continue
                        val label = pm.getApplicationLabel(appInfo).toString()
                        appName.append(appInfo.uid).append(" ").append(label).append('\n')
                    }
                    Freezeit.freezeitTask(
                        Freezeit.setAppLabel,
                        appName.toString().toByteArray(StandardCharsets.UTF_8),
                        appNameHandler)
                }
                return true
            }

            else -> return false
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runIO { Freezeit.freezeitTask(Freezeit.getLog, null, handler) }
            }
        }, 0, 3000)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler = null
    }
}