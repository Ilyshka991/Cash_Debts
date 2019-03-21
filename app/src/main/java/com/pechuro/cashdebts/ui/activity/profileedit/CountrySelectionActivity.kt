package com.pechuro.cashdebts.ui.activity.profileedit

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.pechuro.cashdebts.ui.base.ContainerBaseActivity
import com.pechuro.cashdebts.ui.fragment.profileedit.ProfileEditEvent
import com.pechuro.cashdebts.ui.fragment.profileedit.ProfileEditFragment
import com.pechuro.cashdebts.ui.utils.EventBus
import io.reactivex.rxkotlin.addTo

class ProfileEditActivity : ContainerBaseActivity<ProfileEditActivityViewModel>() {
    override val homeFragment: Fragment
        get() = ProfileEditFragment.newInstance()

    override fun getViewModelClass() = ProfileEditActivityViewModel::class

    override fun onStart() {
        super.onStart()
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        EventBus.listen(ProfileEditEvent::class.java).subscribe {
            when (it) {
                is ProfileEditEvent.OnSaved -> finish()
            }
        }.addTo(weakCompositeDisposable)
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, ProfileEditActivity::class.java)
    }
}