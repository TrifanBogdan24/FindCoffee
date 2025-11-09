package com.example.findcoffee.data_base

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coffee_sizes")
data class CoffeeSize(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coffeeId: Int,
    val size: String,
    val finalVolume: String?
)
