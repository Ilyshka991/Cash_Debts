package com.pechuro.cashdebts.ui.fragment.profileview

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.data.data.model.FirestoreUser
import com.pechuro.cashdebts.ui.base.BaseFragment
import com.pechuro.cashdebts.ui.fragment.profileedit.ProfileEditEvent
import com.pechuro.cashdebts.ui.utils.EventBus
import com.pechuro.cashdebts.ui.utils.loadAvatar
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_profile_view.*

class ProfileViewFragment : BaseFragment<ProfileViewFragmentViewModel>() {
    override val layoutId: Int
        get() = R.layout.fragment_profile_view

    override fun getViewModelClass() = ProfileViewFragmentViewModel::class

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewListeners()
        setEventListeners()
    }

    override fun onStart() {
        super.onStart()
        setViewModelListeners()
    }

    private fun setViewListeners() {
        button_edit.setOnClickListener {
            EventBus.publish(ProfileViewEvent.OpenEditProfile)
        }
        button_logout.setOnClickListener {
            EventBus.publish(ProfileViewEvent.OnLogout)
        }
    }

    private fun setViewModelListeners() {
        viewModel.loadingState.subscribe {
            when (it) {
                is ProfileViewFragmentViewModel.LoadingState.OnStart -> showProgressDialog()
                is ProfileViewFragmentViewModel.LoadingState.OnStop -> dismissProgressDialog()
                is ProfileViewFragmentViewModel.LoadingState.OnError -> showErrorSnackbar()
            }
        }.addTo(weakCompositeDisposable)

        viewModel.user.subscribe {
            setUser(it)
        }.addTo(weakCompositeDisposable)
    }

    private fun setEventListeners() {
        EventBus.listen(ProfileEditEvent::class.java).subscribe {
            when (it) {
                is ProfileEditEvent.OnSaved -> viewModel.loadUser()
            }
        }.addTo(strongCompositeDisposable)
    }

    private fun setUser(user: FirestoreUser) {
        with(user) {
            image_avatar.loadAvatar(photoUrl)
            text_first_name.text = firstName
            text_last_name.text = lastName
            text_phone.text = phoneNumber
        }
    }

    private fun showErrorSnackbar() {
        Snackbar.make(layout_coordinator, R.string.error_load, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_retry) {
                viewModel.loadUser()
            }
            .show()
    }

    companion object {
        fun newInstance() = ProfileViewFragment()
    }
}

