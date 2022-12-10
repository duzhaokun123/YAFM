package io.github.duzhaokun123.yafm.ui.settings

import android.os.Bundle
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.utils.maxSystemBarsDisplayCutout
import io.github.duzhaokun123.yafm.R
import io.github.duzhaokun123.yafm.databinding.ActivitySettingsBinding

class SettingsActivity : BaseActivity<ActivitySettingsBinding>(R.layout.activity_settings) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                .add(R.id.fl, SettingsFragment::class.java, null, "SettingsFragment")
                .commit()
    }

    override fun onResume() {
        super.onResume()
        findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)?.clipToPadding = false
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)?.updatePadding(bottom = insets.maxSystemBarsDisplayCutout.bottom)
    }
}