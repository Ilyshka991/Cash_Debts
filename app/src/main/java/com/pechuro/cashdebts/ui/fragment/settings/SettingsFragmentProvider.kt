package com.pechuro.cashdebts.ui.fragment.settings

import com.pechuro.cashdebts.di.annotations.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface SettingsFragmentProvider {

    @FragmentScope
    @ContributesAndroidInjector(modules = [SettingsFragmentModule::class])
    fun bind(): SettingsFragment
}
