package com.pechuro.cashdebts.ui.activity.countryselection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.databinding.ActivityContainerBinding
import com.pechuro.cashdebts.ui.activity.countryselection.fragment.CountrySelectionFragment
import com.pechuro.cashdebts.ui.base.BaseActivity
import com.pechuro.cashdebts.ui.custom.phone.CountryData
import com.pechuro.cashdebts.ui.utils.BaseEvent
import com.pechuro.cashdebts.ui.utils.EventBus
import com.pechuro.cashdebts.ui.utils.transaction

class CountrySelectionActivity : BaseActivity<ActivityContainerBinding, CountrySelectionActivityViewModel>() {

    override val viewModel: CountrySelectionActivityViewModel
        get() = ViewModelProviders.of(this, viewModelFactory).get(CountrySelectionActivityViewModel::class.java)
    override val layoutId: Int
        get() = R.layout.activity_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
        if (savedInstanceState == null) homeFragment()
    }

    override fun onStart() {
        super.onStart()
        subscribeToEvents()
    }

    override fun onSupportNavigateUp(): Boolean {
        onCanceled()
        return true
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_action_close_white)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun homeFragment() {
        val fragment = CountrySelectionFragment.newInstance()
        supportFragmentManager.transaction {
            replace(viewDataBinding.container.id, fragment)
        }
    }

    private fun subscribeToEvents() {
        EventBus.listen(CountySelectEvent::class.java).subscribe {
            when (it) {
                is CountySelectEvent.OnCountySelect -> onCountrySelected(it.country)
            }
        }.let(weakCompositeDisposable::add)
    }

    private fun onCanceled() {
        val intent = Intent()
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun onCountrySelected(country: CountryData) {
        val intent = Intent().apply {
            putExtra(INTENT_DATA_SELECTED_COUNTRY, country)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        const val INTENT_DATA_SELECTED_COUNTRY = "country"

        fun newIntent(context: Context) = Intent(context, CountrySelectionActivity::class.java)
    }
}

sealed class CountySelectEvent : BaseEvent() {
    class OnCountySelect(val country: CountryData) : CountySelectEvent()
}