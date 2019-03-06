package com.pechuro.cashdebts.ui.activity.auth.phone

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.pechuro.cashdebts.BR
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.databinding.FragmentAuthPhoneBinding
import com.pechuro.cashdebts.ui.activity.auth.AuthActivityViewModel
import com.pechuro.cashdebts.ui.activity.auth.Events
import com.pechuro.cashdebts.ui.base.BaseFragment
import com.pechuro.cashdebts.ui.custom.phone.CountryData

class AuthPhoneFragment : BaseFragment<FragmentAuthPhoneBinding, AuthActivityViewModel>() {
    override val viewModel: AuthActivityViewModel
        get() = ViewModelProviders.of(requireActivity(), viewModelFactory).get(AuthActivityViewModel::class.java)
    override val bindingVariables: Map<Int, Any>
        get() = mapOf(BR.viewModel to viewModel)
    override val layoutId: Int
        get() = R.layout.fragment_auth_phone

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupView()
    }

    private fun setupView() {
        viewDataBinding.textCounty.setOnClickListener {
            viewModel.command.onNext(Events.OpenCountySelection)
        }
        viewDataBinding.textPhone.setCountryData(CountryData("BY", "375", "Belarus", "–– –– –– XXX"))
    }

    companion object {
        fun newInstance() = AuthPhoneFragment().apply {
            arguments = Bundle().apply {
            }
        }
    }
}