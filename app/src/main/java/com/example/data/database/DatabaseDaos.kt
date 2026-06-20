package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY timestamp DESC")
    fun getAllRecipesFlow(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteRecipesFlow(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: String): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: String)
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM groceries ORDER BY isPurchased ASC, category ASC")
    fun getAllGroceriesFlow(): Flow<List<GroceryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryEntity)

    @Update
    suspend fun updateGroceryItem(item: GroceryEntity)

    @Delete
    suspend fun deleteGroceryItem(item: GroceryEntity)

    @Query("DELETE FROM groceries WHERE id = :id")
    suspend fun deleteGroceryById(id: Int)

    @Query("DELETE FROM groceries WHERE isPurchased = 1")
    suspend fun clearPurchasedGroceries()

    @Query("DELETE FROM groceries")
    suspend fun clearAllGroceries()
}
