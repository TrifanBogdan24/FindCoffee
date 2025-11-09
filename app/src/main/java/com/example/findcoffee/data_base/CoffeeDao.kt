package com.example.findcoffee.data_base

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CoffeeDao {
    @Query("DELETE FROM coffees")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(coffee: Coffee): Long

    @Query("SELECT * FROM coffees WHERE LOWER(name) = :name LIMIT 1")
    suspend fun getByName(name: String): Coffee?

    @Query("SELECT name FROM coffees")
    suspend fun getAllNames(): List<String>
}


@Dao
interface IngredientDao {
    @Query("DELETE FROM ingredients")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(ingredient: Ingredient)

    @Query("SELECT * FROM ingredients WHERE coffeeId = :coffeeId AND LOWER(size) = :size")
    suspend fun getIngredientsForCoffeeAndSize(coffeeId: Int, size: String): List<Ingredient>
}

@Dao
interface StepDao {
    @Query("DELETE FROM steps")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(step: Step)

    @Query("SELECT * FROM steps WHERE coffeeId = :coffeeId ORDER BY stepNumber ASC")
    suspend fun getStepsForCoffee(coffeeId: Int): List<Step>
}


@Dao
interface CoffeeSizeDao {
    @Query("DELETE FROM coffee_sizes")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(size: CoffeeSize)

    @Query("SELECT * FROM coffee_sizes WHERE coffeeId = :coffeeId")
    suspend fun getSizesForCoffee(coffeeId: Int): List<CoffeeSize>

    @Query("SELECT * FROM coffee_sizes WHERE coffeeId = :coffeeId AND LOWER(size) = :size LIMIT 1")
    suspend fun getSizeForCoffeeAndName(coffeeId: Int, size: String): CoffeeSize?
}
