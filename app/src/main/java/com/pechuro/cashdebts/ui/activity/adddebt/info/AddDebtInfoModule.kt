package com.pechuro.cashdebts.ui.activity.adddebt.info

import com.pechuro.cashdebts.di.annotations.FragmentScope
import dagger.Module
import dagger.Provides
import java.text.SimpleDateFormat
import java.util.*

@Module
class AddDebtInfoModule {

    @Provides
    @FragmentScope
    fun provideDateFormatter() = SimpleDateFormat(DATE_FORMATE_PATTERN, Locale.getDefault())

    companion object {
        private const val DATE_FORMATE_PATTERN = "dd-MM-yyyy HH:mm"
    }
}