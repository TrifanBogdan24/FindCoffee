package com.example.findcoffee.data_base

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps")
data class Step(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coffeeId: Int,
    val stepNumber: Int,
    val title: String?,
    val description: String?
)
