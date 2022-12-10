package io.github.duzhaokun123.yafm.ui.main

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.utils.maxSystemBarsDisplayCutout
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main, Config.NO_BACK, Config.LAYOUT_MATCH_HORI) {
    private lateinit var navController: NavController

    override fun initViews() {
        super.initViews()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment
        navController = navHostFragment.navController
//        baseBinding.nv?.setupWithNavController(navController)
        baseBinding.nrv?.setupWithNavController(navController)
        baseBinding.bnv?.setupWithNavController(navController)
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        insets.maxSystemBarsDisplayCutout.let { i ->
            rootBinding.rootTb.updatePadding(left = i.left, right = i.right)
            baseBinding.fcv.updatePadding(left = i.left, right = i.right)
        }
    }
}