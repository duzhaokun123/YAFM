package io.github.duzhaokun123.yafm.ui.main

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.core.view.MenuProvider
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.androidapptemplate.bases.BaseFragment
import io.github.duzhaokun123.androidapptemplate.utils.getAttr
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.androidapptemplate.utils.runMain
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.databinding.FragmentHomeBinding
import io.github.duzhaokun123.yafm.ui.settings.SettingsActivity
import io.github.duzhaokun123.yafm.utils.Freezeit
import io.github.duzhaokun123.yafm.utils.Http
import org.json.JSONException
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.pow

class HomeFragment: BaseFragment<FragmentHomeBinding>(R.layout.fragment_home), MenuProvider {
    companion object {
        const val TAG = "HomeFragment"
    }

    private var moduleVersionCode = 0
    lateinit var timer: Timer

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireBaseActivity().addMenuProvider(this, viewLifecycleOwner)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.home_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
                return true
            }
            R.id.help_home -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setView(ImageView(requireContext()).apply {
                        setImageResource(R.drawable.help_home)
                    }).setTitle(R.string.help)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return true
            }
            else -> return false
        }
    }

    private fun setStatusError() {
        val colorError = requireBaseActivity().theme.getAttr(com.google.android.material.R.attr.colorError).data
        val colorOnError = requireBaseActivity().theme.getAttr(com.google.android.material.R.attr.colorOnError).data
        baseBinding.mcvStatus.setCardBackgroundColor(colorError)
        baseBinding.mcvStatus.outlineAmbientShadowColor = colorError
        baseBinding.mcvStatus.outlineSpotShadowColor = colorError
        baseBinding.tvMagisk.setTextColor(colorOnError)
        baseBinding.tvMagisk.setText(R.string.freezeit_offline)
        baseBinding.tvVersion.setTextColor(colorOnError)
        baseBinding.ivIcon.setImageResource(R.drawable.ic_round_error)
        Log.e(TAG, getString(R.string.freezeit_offline))
    }

    private val statusHandler = object : Handler(Looper.getMainLooper()){
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) {
                setStatusError()
                return
            }

            // info [0]:moduleID [1]:moduleName [2]:moduleVersion [3]:moduleVersionCode [4]:moduleAuthor
            //      [5]:xxx xxx xxx xxx (全部内存 可用内存 全部虚拟内存 可用虚拟内存: bytes)
            val info = String(response, StandardCharsets.UTF_8).split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            if (info.size < 5 || info[0] != "freezeit") {
                setStatusError()
                return
            }

            moduleVersionCode = info[3].toInt()

            baseBinding.tvMagisk.setText(R.string.magisk_online)
            baseBinding.tvVersion.text = "${info[2]} ($moduleVersionCode)"

            runIO { Http.getData("https://raw.fastgit.org/jark006/freezeitRelease/master/update.json", checkUpdateHandler) }
        }
    }

    private val checkUpdateHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) return


            try {
                val json = JSONObject(String(response, StandardCharsets.UTF_8))
                val version = json.getString("version")
                val versionCode = json.getInt("versionCode")
                if (versionCode > moduleVersionCode) {
                    baseBinding.mcvUpdate.visibility = View.VISIBLE
                    baseBinding.tvUpdateTitle.text = getString(R.string.need_update, "$version ($versionCode)")
                    baseBinding.btnUpdate.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(json.getString("zipUrl"))
                        })
                    }
                    baseBinding.tvUpdateSummary.setText(R.string.loading)
                    runIO { Http.getData(json.getString("changelog"), changelogHandler) }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private val changelogHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) return

            val s = response.toString(StandardCharsets.UTF_8).split("### ")
            baseBinding.tvUpdateSummary.text = s[1].split("\n").filter { it.isNotBlank() }.joinToString("\n")
        }
    }

    lateinit var am: ActivityManager
    lateinit var memoryInfo: ActivityManager.MemoryInfo
    var viewWidth = 0
    var viewHeight = 0
    var availMem: Long = 0

    var realTimeTask = Runnable {
        am.getMemoryInfo(memoryInfo)
        availMem = memoryInfo.availMem
        Freezeit.freezeitTask(
            Freezeit.getRealTimeInfo,
            ("" + viewHeight / 3 + " " + viewWidth / 3 + " " + availMem).toByteArray(),
            realTimeHandler)
    }

    private val realTimeHandler: Handler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n", "DefaultLocale")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) {
                baseBinding.memLayout.visibility = View.GONE
                return
            }
            if (viewHeight == 0 || viewWidth == 0) return
            runIO {
                var bitmap = Bitmap.createBitmap(viewWidth / 3, viewHeight / 3, Bitmap.Config.ARGB_8888)
                val buffer = ByteBuffer.wrap(response)
                bitmap.copyPixelsFromBuffer(buffer)
                val matrix = Matrix()
                matrix.postScale(3F, 3F)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, viewWidth / 3, viewHeight / 3, matrix, false)
                runMain {
                    baseBinding.cpuImg.setImageBitmap(bitmap)
                }
            }
            val offset: Int = viewWidth / 3 * (viewHeight / 3) * 4
            if (response.size - offset <= 0) {
                val errorTips = "handleMessage: viewWidth" + viewWidth + " viewHeight" +
                        viewHeight + " response.length" + response.size + " offset" + offset
                Log.e(TAG, errorTips)
                baseBinding.memInfo.text = errorTips
                return
            }
            val tmpBytes = ByteArray(response.size - offset)
            System.arraycopy(response, offset, tmpBytes, 0, response.size - offset)
            val tmpStr = String(tmpBytes, StandardCharsets.UTF_8)
            val realTimeInfo = tmpStr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()

            // [0/1/2/3]内存情况 [4-11]八个核心频率 [12-19]八个核心使用率
            // [20]CPU总使用率 [21]CPU温度(需除以1000) [22]电流(mA)
            if (realTimeInfo.size < 23) {
                val tmp = StringBuilder("handleMessage: memSplit.length" + realTimeInfo.size)
                for (i in realTimeInfo.indices) tmp.append(" [").append(i).append("]").append(
                    realTimeInfo[i])
                Log.e(TAG, tmp.toString())
                baseBinding.memInfo.text = tmp
                return
            }
            val memList = LongArray(4)
            try {
                for (i in 0..3) memList[i] = realTimeInfo[i].toLong()
            } catch (e: Exception) {
                Log.e(TAG, "handleMessage: memList long:$tmpStr\n$e")
                baseBinding.cpu.text = "tmpStr$tmpStr"
                return
            }
            @SuppressLint("DefaultLocale") var tmp: String = String.format(
                "[物理内存] 全部: %.2f GiB\n已用:%.1f%% 剩余: %.2f %s",
                memList[0] / 1024.0.pow(3.0), 100.0 * (memList[0] - availMem) / memList[0],
                if (availMem > 1024.0.pow(3.0)) availMem / 1024.0.pow(3.0) else availMem / 1024.0.pow(2.0),
                if (availMem > 1024.0.pow(3.0)) "GiB" else "MiB")
            baseBinding.memInfo.text = tmp
            if (memList[2] > 0) { //可能没有 虚拟内存
                tmp = String.format(
                    "[虚拟内存] 全部: %.2f GiB\n已用:%.1f%% 剩余: %.2f %s",
                    memList[2] / 1024.0.pow(3.0),
                    100.0 * (memList[2] - memList[3]) / memList[2],
                    if (memList[3] > 1024.0.pow(3.0)) memList[3] / 1024.0.pow(3.0) else memList[3] / 1024.0.pow(2.0),
                    if (memList[3] > 1024.0.pow(3.0)) "GiB" else "MiB")
                baseBinding.zramInfo.text = tmp
            }

            // [4-11]八个核心频率 [12-19]八个核心使用率
            // [20]CPU总使用率 [21]CPU温度(需除以1000) [22]电流(mA)
            var percent = 0
            var temperature = 0
            var mA = 0
            try {
                percent = realTimeInfo[20].toInt()
                temperature = realTimeInfo[21].toInt()
                mA = realTimeInfo[22].toInt()
                mA = if (mA == 0) 0 else mA / -1000
            } catch (e: Exception) {
                Log.e(TAG, "fail percent:[" + realTimeInfo[20] + "] temperature[" + realTimeInfo[21] + "] mA[" + realTimeInfo[22] + "]")
            }
            baseBinding.cpu.text =
                String.format(getString(R.string.realtime_text), percent, temperature / 1000.0)
            baseBinding.battery.text = if (abs(mA) > 2000) String.format("%.2f A", mA / 1e3) else "$mA mA"
            baseBinding.cpu0.text = "cpu0\n${realTimeInfo[4]}MHz\n${realTimeInfo[12]}%"
            baseBinding.cpu1.text = "cpu1\n${realTimeInfo[5]}MHz\n${realTimeInfo[13]}%"
            baseBinding.cpu2.text = "cpu2\n${realTimeInfo[6]}MHz\n${realTimeInfo[14]}%"
            baseBinding.cpu3.text = "cpu3\n${realTimeInfo[7]}MHz\n${realTimeInfo[15]}%"
            baseBinding.cpu4.text = "cpu4\n${realTimeInfo[8]}MHz\n${realTimeInfo[16]}%"
            baseBinding.cpu5.text = "cpu5\n${realTimeInfo[9]}MHz\n${realTimeInfo[17]}%"
            baseBinding.cpu6.text = "cpu6\n${realTimeInfo[10]}MHz\n${realTimeInfo[18]}%"
            baseBinding.cpu7.text = "cpu7\n${realTimeInfo[11]}MHz\n${realTimeInfo[19]}%"
        }
    }

    override fun initViews() {
        super.initViews()
        baseBinding.mcvUpdate.visibility = View.GONE
        baseBinding.cpuImg.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewWidth = baseBinding.cpuImg.width
                viewHeight = baseBinding.cpuImg.height
                baseBinding.cpuImg.viewTreeObserver.removeOnGlobalLayoutListener(this)
                Thread(realTimeTask).start()
            }
        })
        MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_small)).apply {
            baseBinding.cpu0.setTextColor(this)
            baseBinding.cpu1.setTextColor(this)
            baseBinding.cpu2.setTextColor(this)
            baseBinding.cpu3.setTextColor(this)
        }
        MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_mid)).apply {
            baseBinding.cpu4.setTextColor(this)
            baseBinding.cpu5.setTextColor(this)
            baseBinding.cpu6.setTextColor(this)
        }
        MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_big)).apply {
            baseBinding.cpu7.setTextColor(this)
        }
    }

    override fun initData() {
        runIO { Freezeit.freezeitTask(Freezeit.getInfo, null, statusHandler) }
        am = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        memoryInfo = ActivityManager.MemoryInfo()
    }

    override fun onResume() {
        super.onResume()
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (viewHeight == 0 || viewWidth == 0) return
                Thread(realTimeTask).start()
            }
        }, 2000, 3000)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }
}