package com.pechuro.cashdebts.ui.activity.auth

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.ui.activity.auth.code.AuthCodeFragment
import com.pechuro.cashdebts.ui.activity.auth.phone.AuthPhoneFragment
import com.pechuro.cashdebts.ui.activity.main.MainActivity
import com.pechuro.cashdebts.ui.base.activity.FragmentSwitcherBaseActivity
import com.pechuro.cashdebts.ui.fragment.profileedit.ProfileEditEvent
import com.pechuro.cashdebts.ui.fragment.profileedit.ProfileEditFragment
import com.pechuro.cashdebts.ui.utils.EventManager
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_scrolling_container.*

class AuthActivity : FragmentSwitcherBaseActivity<AuthActivityViewModel>() {
    override val isCloseButtonEnabled: Boolean
        get() = false

    override fun getViewModelClass() = AuthActivityViewModel::class

    override fun getHomeFragment() = AuthPhoneFragment.newInstance()

    override fun onStart() {
        super.onStart()
        setEventListener()
        setViewModelListener()
    }

    private fun setViewModelListener() {
        viewModel.command.subscribe {
            when (it) {
                is AuthActivityViewModel.Events.OnCodeSent -> showFragment(AuthCodeFragment.newInstance())
                is AuthActivityViewModel.Events.OnComplete -> {
                    if (it.isUserExist) {
                        openMainActivity()
                    } else {
                        isBackAllowed = false
                        showFragment(ProfileEditFragment.newInstance(true))
                    }
                }
                is AuthActivityViewModel.Events.OnError -> showSnackBar(it.id)
            }
        }.addTo(weakCompositeDisposable)
    }

    private fun setEventListener() {
        EventManager.listen(ProfileEditEvent::class.java).subscribe {
            when (it) {
                is ProfileEditEvent.OnSaved -> openMainActivity()
            }
        }.addTo(weakCompositeDisposable)
    }

    private fun showSnackBar(@StringRes id: Int) {
        Snackbar.make(container, id, Snackbar.LENGTH_LONG)
            .setActionTextColor(ResourcesCompat.getColor(resources, R.color.colorOrange, theme))
            .show()
    }

    private fun openMainActivity() {
        val intent = MainActivity.newIntent(this)
        startActivity(intent)
        finish()
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, AuthActivity::class.java)
    }
}
