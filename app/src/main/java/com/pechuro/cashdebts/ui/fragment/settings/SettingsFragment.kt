package com.pechuro.cashdebts.ui.fragment.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.SpinnerAdapter
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.model.prefs.PrefsManager
import com.pechuro.cashdebts.ui.base.BaseFragment
import com.pechuro.cashdebts.ui.utils.EventManager
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

class SettingsFragment : BaseFragment<SettingsFragmentViewModel>() {
    @Inject
    protected lateinit var prefsManager: PrefsManager
    @Inject
    protected lateinit var languagesAdapter: SpinnerAdapter
    @Inject
    protected lateinit var languagesId: Array<String>

    override val layoutId: Int
        get() = R.layout.fragment_settings

    override fun getViewModelClass() = SettingsFragmentViewModel::class

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setViewListeners()
    }

    private fun setupView() {
        switch_add_plus.isChecked = prefsManager.settingsAutoAddPlus
        spinner_language.apply {
            adapter = languagesAdapter
            setSelection(languagesId.indexOf(prefsManager.settingCurrentLocale))
        }
    }

    private fun setViewListeners() {
        switch_add_plus.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.settingsAutoAddPlus = isChecked
        }
        spinner_language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                setLanguage(position)
            }
        }
    }

    private fun setLanguage(position: Int) {
        val languageCode = languagesId[position]
        if (languageCode != prefsManager.settingCurrentLocale) {
            prefsManager.settingCurrentLocale = languageCode
            EventManager.publish(SettingsFragmentEvent.OnLanguageChanged)
        }
    }

    companion object {

        fun newInstance() = SettingsFragment()
    }
}
