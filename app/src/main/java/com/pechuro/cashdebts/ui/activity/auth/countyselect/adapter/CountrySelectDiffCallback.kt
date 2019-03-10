package com.pechuro.cashdebts.ui.activity.auth.countyselect.adapter

import androidx.recyclerview.widget.DiffUtil
import com.pechuro.cashdebts.ui.custom.phone.CountryData

class CountrySelectDiffCallback : DiffUtil.Callback() {

    private var oldList: List<CountryData> = emptyList()
    private var newList: List<CountryData> = emptyList()

    fun setData(oldList: List<CountryData>, newList: List<CountryData>) {
        this.oldList = oldList
        this.newList = newList
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].code == newList[newItemPosition].code

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].code == newList[newItemPosition].code
}