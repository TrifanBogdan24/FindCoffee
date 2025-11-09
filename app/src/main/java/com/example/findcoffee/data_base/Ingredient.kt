package com.example.findcoffee.data_base

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coffeeId: Int,
    val size: String,
    val ingredient: String,
    val quantity: String?
)
