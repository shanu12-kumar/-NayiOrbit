package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---------------- USER PROFILE ENTITY ----------------
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1, // Singleton style
    val age: Int = 28,
    val gender: String = "Male",
    val height: Double = 175.0,
    val weight: Double = 70.0,
    val fitnessGoal: String = "Balanced Nutrition",
    val dietaryPref: String = "None",
    val allergies: String = "None",
    val preferredCuisines: String = "Indian, Italian, Continental",
    val language: String = "English",
    val streakCount: Int = 3,
    val lastCookedDate: Long = 0L,
    val caloryTarget: Int = 2000,
    val proteinTarget: Int = 120, // grams
    val carbTarget: Int = 220,    // grams
    val fatTarget: Int = 65        // grams
)

// ---------------- RECIPE ENTITY ----------------
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String, // Dynamic or random string ID
    val name: String,
    val category: String, // e.g. "Breakfast", "Lunch", "Dinner"
    val description: String,
    val ingredients: String, // Comma or newline separated
    val instructions: String, // Newline separated steps
    val prepTime: String = "15 mins",
    val cookTime: String = "20 mins",
    val totalTime: String = "35 mins",
    val difficulty: String = "Easy",
    val servingSize: String = "2 servings",
    val calories: Int = 300,
    val protein: String = "15g",
    val carbohydrates: String = "40g",
    val fat: String = "10g",
    val fiber: String = "4g",
    val imageUrl: String = "",
    val source: String = "AI Chef Assistant",
    val rating: Float = 4.5f,
    val isFavorite: Boolean = false,
    val isLeftoverIdea: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// ---------------- GROCERY ENTITY ----------------
@Entity(tableName = "groceries")
data class GroceryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Produce", // Vegetables, Fruits, Dairy, Pantry...
    val quantity: String = "1 unit",
    val isPurchased: Boolean = false,
    val estimatedCost: Double = 0.0,
    val originalRecipeId: String? = null
)
