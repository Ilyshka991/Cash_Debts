package com.pechuro.cashdebts.ui.fragment.profileedit

import com.pechuro.cashdebts.ui.utils.BaseEvent

sealed class ProfileEditEvents : BaseEvent() {
    object OnSaved : ProfileEditEvents()
}