package com.pechuro.cashdebts.ui.activity.adddebt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.databinding.ActivityContainerBinding
import com.pechuro.cashdebts.ui.activity.adddebt.user.AddDebtUserFragment
import com.pechuro.cashdebts.ui.base.BaseActivity
import com.pechuro.cashdebts.ui.utils.transaction

class AddActivity : BaseActivity<ActivityContainerBinding, AddDebtActivityViewModel>() {

    override val viewModel: AddDebtActivityViewModel
        get() = ViewModelProviders.of(this, viewModelFactory).get(AddDebtActivityViewModel::class.java)
    override val layoutId: Int
        get() = R.layout.activity_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) homeFragment()
    }

    private fun homeFragment() {
        val fragment = AddDebtUserFragment.newInstance()
        supportFragmentManager.transaction {
            replace(viewDataBinding.container.id, fragment)
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, AddActivity::class.java)
    }
}
