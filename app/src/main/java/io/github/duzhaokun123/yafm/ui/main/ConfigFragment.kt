package io.github.duzhaokun123.yafm.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.MenuProvider
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.androidapptemplate.bases.BaseFragment
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.dpToPx
import io.github.duzhaokun123.androidapptemplate.utils.runIO
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.databinding.FragmentConfigBinding
import io.github.duzhaokun123.yafm.databinding.ItemAppBinding
import io.github.duzhaokun123.yafm.ui.base.BaseSimpleAdapter
import io.github.duzhaokun123.yafm.utils.Cache
import io.github.duzhaokun123.yafm.utils.Freezeit
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.function.Consumer

class ConfigFragment : BaseFragment<FragmentConfigBinding>(R.layout.fragment_config), MenuProvider {
    companion object {
        const val TAG = "ConfigFragment"
    }

    private var adapter: Adapter? = null
    private var lastTimestamp = System.currentTimeMillis()
    var searchView: SearchView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireBaseActivity().addMenuProvider(this, viewLifecycleOwner)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.config_menu, menu)
        val searchItem = menu.findItem(R.id.search_view)
        searchView = searchItem.actionView as SearchView?
        if (searchView != null) {
            searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean { //按下搜索触发
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    if (adapter != null) adapter!!.filter(newText)
                    return false
                }
            })
        } else {
            Log.e(TAG, "onCreateMenu: searchView == null")
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.help_config -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setView(ImageView(requireContext()).apply {
                        setImageResource(R.drawable.help_config)
                    }).setTitle(R.string.help)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }

            else -> false
        }
    }

    override fun initViews() {
        baseBinding.rv.layoutManager = GridLayoutManager(context, 1)
        baseBinding.rv.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val w = 500.dpToPx()
            var a = (v.width - w) / 2
            if (a < 0) a = 0
            if (a * 2 <= w / 3) a = 0
            v.updatePadding(left = a, right = a)
        }
        val space = 6.dpToPx()
        baseBinding.rv.addItemDecoration(object : ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                with(outRect) {
                    if (parent.getChildAdapterPosition(view) == 0) {
                        top = space
                    }
                    left = space
                    right = space
                    bottom = space
                }
            }
        })
        baseBinding.srl.setOnRefreshListener {
            runIO { Freezeit.freezeitTask(Freezeit.getAppCfg, null, getAppCfgHandler) }
        }
        baseBinding.fab.setOnClickListener {
            if (System.currentTimeMillis() - lastTimestamp < 500) {
                TipUtil.showTip(context, R.string.slowly_tips)
                return@setOnClickListener
            }
            lastTimestamp = System.currentTimeMillis()
            adapter ?: return@setOnClickListener
            runIO { Freezeit.freezeitTask(Freezeit.setAppCfg, adapter!!.getCfgBytes(), setAppCfgHandler) }
        }
    }

    override fun initData() {
        val pm = requireContext().packageManager
        val applicationList = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        for (appInfo in applicationList) {
            if (appInfo.uid < 10000) continue
            if (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) continue
            applicationInfoList.add(appInfo)
            runIO {
                Cache.getIconLabel(appInfo)
            }
        }
        baseBinding.pv.visibility = View.VISIBLE
        runIO { Freezeit.freezeitTask(Freezeit.getAppCfg, null, getAppCfgHandler) }
    }

    var applicationInfoList: MutableList<ApplicationInfo> = mutableListOf()

    private val getAppCfgHandler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            if (response == null || response.isEmpty()) return

            // 配置名单 <uid, <cfg, isTolerant>>
            // cfg: [10]:杀死 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
            val appCfg = HashMap<Int, Pair<Int, Int>>()
            val list = String(response, StandardCharsets.UTF_8).split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (item in list) {
                val packageMode = item.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (packageMode.size != 3) {
                    Log.e(TAG, "handleMessage: unknownItem:[$item]")
                    continue
                }
                try {
                    appCfg[packageMode[0].toInt()] =
                        Pair(
                            packageMode[1].toInt(), packageMode[2].toInt())
                } catch (e: Exception) {
                    Log.e(TAG, "handleMessage: unknownItem:[$item]")
                }
            }

            // 补全
            applicationInfoList.forEach(
                Consumer { info: ApplicationInfo ->
                    if (!appCfg.containsKey(info.uid)) appCfg[info.uid] = Pair(Freezeit.CFG_FREEZER, 1) // 默认Freezer 宽松
                })
            // 检查非法配置
            appCfg.forEach { (uid: Int, cfg: Pair<Int, Int>) ->
                if (cfg.first < Freezeit.CFG_TERMINATE || cfg.first > Freezeit.CFG_WHITEFORCE) appCfg[uid] =
                    Pair(Freezeit.CFG_FREEZER, cfg.second)
            }

            // [10]:杀死后台 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
            val applicationInfoListSort: MutableList<ApplicationInfo> = ArrayList()

            // 先排 自由
            for (info in applicationInfoList) {
                val mode = appCfg[info.uid]
                if (mode != null && mode.first == Freezeit.CFG_WHITELIST) applicationInfoListSort.add(info)
            }

            // 再排序 宽松前台 的 FREEZER SIGSTOP 杀死后台
            for (info in applicationInfoList) {
                val mode = appCfg[info.uid]
                if (mode != null && mode.first <= Freezeit.CFG_FREEZER && mode.second != 0) applicationInfoListSort.add(info)
            }

            // 再排序 严格前台 的 FREEZER SIGSTOP 杀死后台
            for (info in applicationInfoList) {
                val mode = appCfg[info.uid]
                if (mode != null && mode.first == Freezeit.CFG_FREEZER && mode.second == 0) applicationInfoListSort.add(info)
            }
            for (info in applicationInfoList) {
                val mode = appCfg[info.uid]
                if (mode != null && mode.first == Freezeit.CFG_SIGSTOP && mode.second == 0) applicationInfoListSort.add(info)
            }
            for (info in applicationInfoList) {
                val mode = appCfg[info.uid]
                if (mode != null && mode.first == Freezeit.CFG_TERMINATE && mode.second == 0) applicationInfoListSort.add(info)
            }

            // 最后排 内置自由
            for (info in applicationInfoList) {
                val mode = appCfg[info.uid]
                if (mode != null && mode.first == Freezeit.CFG_WHITEFORCE) applicationInfoListSort.add(info)
            }
            adapter = Adapter(requireContext(), applicationInfoListSort, appCfg)
            baseBinding.rv.adapter = adapter
            baseBinding.srl.isRefreshing = false
            adapter!!.notifyItemRangeChanged(0, applicationInfoListSort.size - 1)
            baseBinding.pv.visibility = View.GONE
        }
    }

    private val setAppCfgHandler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val response = msg.data.getByteArray("response")
            val res = String(response!!, StandardCharsets.UTF_8)
            if (res == "success") {
                TipUtil.showTip(context, R.string.update_success)
            } else {
                val errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]"
                TipUtil.showTip(context, errorTips)
                Log.e(TAG, errorTips)
            }
        }
    }

    class Adapter(context: Context, private val applicationInfoListSort: MutableList<ApplicationInfo>, private val appCfg: HashMap<Int, Pair<Int, Int>>) :
        BaseSimpleAdapter<ItemAppBinding>(context, R.layout.item_app) {
        private var applicationListFilter: MutableList<ApplicationInfo>? = null

        fun getCfgBytes(): ByteArray {
            val tmp = StringBuilder()
            appCfg.forEach { (key, value) ->
                tmp.append(key).append(' ').append(value.first).append(' ').append(value.second).append('\n')
            }
            return  tmp.toString().toByteArray()
        }

        @SuppressLint("NotifyDataSetChanged")
        fun filter(keyWord: String?) {
            if (keyWord.isNullOrEmpty()) {
                applicationListFilter = applicationInfoListSort
            } else {
                applicationListFilter = mutableListOf()
                for (appInfo in applicationInfoListSort) {
                    if (appInfo.packageName.contains(keyWord, true)) {
                        applicationListFilter!!.add(appInfo)
                        continue
                    }
                    val label = Cache.getIconLabel(appInfo).second
                    if (label.contains(keyWord, true)) {
                        applicationListFilter!!.add(appInfo)
                    }
                }
            }
            notifyDataSetChanged()
        }

        override fun initViews(baseBinding: ItemAppBinding, position: Int) {
            val list = applicationListFilter ?: applicationInfoListSort
            val uid = list[position].uid
            baseBinding.spConfig.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val m = when(position) {
                        0 -> 10
                        1 -> 20
                        2 -> 30
                        3 -> 40
                        else -> throw RuntimeException("unknown position $position")
                    }
                    appCfg[uid] = appCfg[uid]?.let { Pair(m, it.second) } ?: return
                    if (m == 30) {
                        baseBinding.spLevel.visibility = View.VISIBLE
                    } else {
                        baseBinding.spLevel.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            baseBinding.spLevel.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val l = when(position) {
                        0 -> 1
                        1 -> 0
                        else -> throw RuntimeException("unknown position $position")
                    }
                    appCfg[uid] = appCfg[uid]?.let { Pair(it.first, l) } ?: return
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            baseBinding.mcv.setOnClickListener {  }
        }

        override fun initData(baseBinding: ItemAppBinding, position: Int) {
            val list = applicationListFilter ?: applicationInfoListSort
            list[position].let { applicationInfo ->
//                baseBinding.ivIcon.setImageDrawable(applicationInfo.loadIcon(pm))
//                baseBinding.tvLabel.text = applicationInfo.loadLabel(pm)
                Cache.getIconLabel(applicationInfo).let { (icon, label) ->
                    baseBinding.ivIcon.setImageDrawable(icon)
                    baseBinding.tvLabel.text = label
                }
                baseBinding.tvPackage.text = applicationInfo.packageName
                val config = appCfg[applicationInfo.uid] ?: return
                val (m, l) = config
                val mi = when(m) {
                    10 -> 0
                    20 -> 1
                    30 -> 2
                    40 -> 3
                    else -> -1
                }
                if (mi >= 0) {
                    baseBinding.spConfig.visibility = View.VISIBLE
                    baseBinding.spConfig.setSelection(mi)
                    if (mi == 2) {
                        baseBinding.spLevel.visibility = View.VISIBLE
                        val li = if (l != 0) 0 else 1
                        baseBinding.spLevel.setSelection(li)
                    } else {
                        baseBinding.spLevel.visibility = View.GONE
                    }
                } else {
                    baseBinding.spConfig.visibility = View.GONE
                    baseBinding.spLevel.visibility = View.GONE
                }
            }
        }

        override fun getItemCount() = applicationListFilter?.size ?: applicationInfoListSort.size
    }
}