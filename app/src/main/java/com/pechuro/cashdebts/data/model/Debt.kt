package com.pechuro.cashdebts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val person: String,
    val value: Double,
    val description: String?
)