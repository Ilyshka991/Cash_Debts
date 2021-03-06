package com.pechuro.cashdebts.ui.activity.main

import com.pechuro.cashdebts.ui.utils.BaseEvent

sealed class MainActivityEvent : BaseEvent() {
    class OpenAddActivity(val isLocalDebt: Boolean, val id: String? = null) : MainActivityEvent()

    class UpdateTotalDebtSum(val value: Double) : MainActivityEvent()
}
