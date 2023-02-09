package io.github.duzhaokun123.yafm.ui.main

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.Toast
import androidx.core.view.MenuProvider
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.androidapptemplate.bases.BaseFragment
import io.github.duzhaokun123.androidapptemplate.utils.getAttr
import io.github.duzhaokun123.androidapptemplate.utils.runIO
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
            //      [5]:clusterType: 2: 4+4 3: 4+3+1 4:3+2+2+1
            val info = String(response, StandardCharsets.UTF_8).split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            if (info.size < 6 || info[0] != "freezeit") {
                setStatusError()
                return
            }

            moduleVersionCode = info[3].toInt()

            baseBinding.tvMagisk.setText(R.string.magisk_online)
            baseBinding.tvVersion.text = "${info[2]} ($moduleVersionCode)"
            val clusterType = info[5]
            MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_small)).apply {
                baseBinding.cpu0.setTextColor(this)
                baseBinding.cpu1.setTextColor(this)
                baseBinding.cpu2.setTextColor(this)
                if (clusterType == "3" || clusterType == "2") {
                    baseBinding.cpu3.setTextColor(this)
                }
            }
            MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_mid)).apply {
                if (clusterType == "4") {
                    baseBinding.cpu3.setTextColor(this)
                }
                baseBinding.cpu4.setTextColor(this)
                if (clusterType == "3") {
                    baseBinding.cpu5.setTextColor(this)
                    baseBinding.cpu6.setTextColor(this)
                }
            }
            MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_mid_plus)).apply {
                if (clusterType == "4") {
                    baseBinding.cpu5.setTextColor(this)
                    baseBinding.cpu6.setTextColor(this)
                }
            }
            MaterialColors.harmonizeWithPrimary(requireContext(), requireContext().getColor(R.color.cpu_big)).apply {
                if (clusterType == "2") {
                    baseBinding.cpu4.setTextColor(this)
                    baseBinding.cpu5.setTextColor(this)
                    baseBinding.cpu6.setTextColor(this)
                }
                baseBinding.cpu7.setTextColor(this)
            }

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
        val payload = ByteArray(12)
        Freezeit.Int2Byte(viewHeight / 3, payload, 0)
        Freezeit.Int2Byte(viewWidth / 3, payload, 4)

        am.getMemoryInfo(memoryInfo) // 底层 /proc/meminfo 的 MemAvailable 不可靠
        Freezeit.Int2Byte((memoryInfo.availMem shr 20).toInt(), payload, 8) //Unit: MiB

        Freezeit.freezeitTask(Freezeit.getRealTimeInfo, payload, realTimeHandler)
    }

    private val realTimeHandler: Handler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n", "DefaultLocale")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")

            val height = viewHeight / 3
            val width = viewWidth / 3

            if (response == null || response.isEmpty() || viewHeight == 0 || viewWidth == 0) return

            // response[0 ~ imgBuffBytes-1]CPU曲线图像数据, [imgBuffBytes ~ end]是其他实时数据
            val imgBuffBytes: Int = height * width * 4 // ARGB 每像素4字节
            if (response.size <= imgBuffBytes) {
                Toast.makeText(requireContext(), String(response), Toast.LENGTH_LONG).show()
                val errorTips = "imgWidth" + width +
                        " imgHeight" + height +
                        " response.length" + response.size +
                        " imgBuffBytes" + imgBuffBytes
                baseBinding.memInfo.text = errorTips
                return
            }

            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(response, 0, imgBuffBytes))
            bitmap = Freezeit.resize(bitmap, 3F, 3F)
            baseBinding.cpuImg.setImageBitmap(bitmap)

            val elementNum = 23
            val realTimeInfoLen: Int = response.size - imgBuffBytes
            if (realTimeInfoLen != 4 * elementNum) {
                baseBinding.memInfo.text = "正常字节长度:" + 4 * elementNum + " 收到长度:" + realTimeInfoLen
                return
            }

            // [0]全部物理内存 [1]可用内存 [2]全部虚拟内存 [3]可用虚拟内存  Unit: MiB
            // [4-11]八个核心频率(MHz) [12-19]八个核心使用率(%)
            // [20]CPU总使用率(%) [21]CPU温度(m℃) [22]电流(mA)
            val realTimeInfo = IntArray(elementNum) //ARM64和X64  Native层均为小端

            Freezeit.Byte2Int(response, imgBuffBytes, elementNum * 4, realTimeInfo, 0)

            val GiB = 1024.0
            val MemTotal = realTimeInfo[0]
            val MemAvailable = realTimeInfo[1]
            val SwapTotal = realTimeInfo[2]
            val SwapFree = realTimeInfo[3]

            @SuppressLint("DefaultLocale")
            var tmp = if (MemTotal <= 0) "" else String.format(
                getString(R.string.physical_ram_text),
                MemTotal / GiB, 100.0 * (MemTotal - MemAvailable) / MemTotal,
                if (MemAvailable > GiB) MemAvailable / GiB else MemAvailable,
                if (MemAvailable > GiB) "GiB" else "MiB"
            )
            baseBinding.memInfo.text = tmp

            tmp = if (SwapTotal <= 0) "" else String.format(
                getString(R.string.virtual_ram_text),
                SwapTotal / GiB, 100.0 * (SwapTotal - SwapFree) / SwapTotal,
                if (SwapFree > GiB) SwapFree / GiB else SwapFree,
                if (SwapFree > GiB) "GiB" else "MiB"
            )
            baseBinding.zramInfo.text = tmp

            val percent = realTimeInfo[20]
            val temperature = realTimeInfo[21] / 1e3 // m℃ -> ℃

            val mA = realTimeInfo[22] / -1000 // uA -> mA

            baseBinding.cpu.text =
                String.format(getString(R.string.cpu_format), percent, temperature)
            baseBinding.battery.text = if (abs(mA) > 2000) String.format("%.2f A\uD83D\uDD0B", mA / 1e3) else "$mA mA\uD83D\uDD0B"
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