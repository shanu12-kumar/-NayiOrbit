package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.network.*
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class CookMateRepository(
    private val userProfileDao: UserProfileDao,
    private val recipeDao: RecipeDao,
    private val groceryDao: GroceryDao
) {
    val userProfileFlow: Flow<UserProfileEntity?> = userProfileDao.getUserProfileFlow()
    val allRecipesFlow: Flow<List<RecipeEntity>> = recipeDao.getAllRecipesFlow()
    val favoriteRecipesFlow: Flow<List<RecipeEntity>> = recipeDao.getFavoriteRecipesFlow()
    val allGroceriesFlow: Flow<List<GroceryEntity>> = groceryDao.getAllGroceriesFlow()

    suspend fun getProfile(): UserProfileEntity? = userProfileDao.getUserProfile()
    suspend fun saveProfile(profile: UserProfileEntity) = userProfileDao.insertOrUpdateProfile(profile)

    suspend fun saveRecipe(recipe: RecipeEntity) = recipeDao.insertRecipe(recipe)
    suspend fun deleteRecipe(recipe: RecipeEntity) = recipeDao.deleteRecipe(recipe)
    suspend fun deleteRecipeById(id: String) = recipeDao.deleteRecipeById(id)

    suspend fun addGroceryItem(item: GroceryEntity) = groceryDao.insertGroceryItem(item)
    suspend fun updateGroceryItem(item: GroceryEntity) = groceryDao.updateGroceryItem(item)
    suspend fun deleteGroceryById(id: Int) = groceryDao.deleteGroceryById(id)
    suspend fun clearPurchasedGroceries() = groceryDao.clearPurchasedGroceries()
    suspend fun clearAllGroceries() = groceryDao.clearAllGroceries()

    /**
     * Checks if a valid Gemini API key is configured.
     */
    fun isKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Calls Gemini to generate content.
     */
    suspend fun askGemini(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext getOfflineMockResponse(prompt)
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )
            val output = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            output ?: "No culinary insights generated. Please attempt again."
        } catch (e: Exception) {
            Log.e("CookMateRepository", "Gemini error", e)
            "Error calling CookMate AI: ${e.localizedMessage}. Falling back to offline cookbook."
        }
    }

    /**
     * Parse a recipe from Gemini's response, or use offline fallbacks
     */
    suspend fun generateRecipe(inputIngredients: String, type: String = "Ingredients"): RecipeEntity = withContext(Dispatchers.Default) {
        val profile = getProfile() ?: UserProfileEntity()
        val prompt = """
            You are world-class AI Master Chef CookMate. Please generate a highly optimized recipe based on:
            Type: $type
            Key Inputs: $inputIngredients
            Diet Preferences: ${profile.dietaryPref}
            Allergies to avoid: ${profile.allergies}
            Fitness Goal: ${profile.fitnessGoal}
            Preferred Cuisines: ${profile.preferredCuisines}
            
            You MUST respond in the EXACT format below. Do not use any markdown bold stars (*) or markdown headers (#) around the tags. Fill details accurately.
            
            [NAME]
            Write Name of dish
            
            [DESCRIPTION]
            A single sentence culinary description.
            
            [CATEGORY]
            Breakfast, Lunch, Dinner, or Snack
            
            [METRIC]
            Prep: 15 mins | Cook: 20 mins | Servings: 2 | Difficulty: Medium
            
            [INGREDIENTS]
            - Ingredient 1
            - Ingredient 2
            
            [INSTRUCTIONS]
            1. First step text.
            2. Second step text.
            
            [NUTRITION]
            Calories: 420 kcal | Protein: 22g | Carbs: 45g | Fat: 12g | Fiber: 6g
        """.trimIndent()

        val aiResult = if (isKeyConfigured()) {
            askGemini(prompt, "You are a gourmet chef with clinical nutritional focus.")
        } else {
            "" // Trigger mock parse
        }

        val recipe = if (aiResult.isNotEmpty() && aiResult.contains("[NAME]")) {
            parseRecipeText(aiResult)
        } else {
            getMockRecipeForInputs(inputIngredients, type)
        }

        // Auto-save generated recipes
        recipeDao.insertRecipe(recipe)
        recipe
    }

    private fun parseRecipeText(text: String): RecipeEntity {
        var name = "Gourmet AI Specialty"
        var description = "A healthy and customized meal crafted just for you."
        var category = "Lunch"
        var metricText = "Prep: 15 mins | Cook: 20 mins | Servings: 2 | Difficulty: Medium"
        var ingredients = ""
        var instructions = ""
        var nutritionText = "Calories: 350 kcal | Protein: 18g | Carbs: 40g | Fat: 10g | Fiber: 5g"

        val sections = text.split("[")
        for (sec in sections) {
            val trimSec = sec.trim()
            if (trimSec.startsWith("NAME]")) {
                name = trimSec.substringAfter("NAME]").trim()
            } else if (trimSec.startsWith("DESCRIPTION]")) {
                description = trimSec.substringAfter("DESCRIPTION]").trim()
            } else if (trimSec.startsWith("CATEGORY]")) {
                category = trimSec.substringAfter("CATEGORY]").trim()
            } else if (trimSec.startsWith("METRIC]")) {
                metricText = trimSec.substringAfter("METRIC]").trim()
            } else if (trimSec.startsWith("INGREDIENTS]")) {
                ingredients = trimSec.substringAfter("INGREDIENTS]").trim()
            } else if (trimSec.startsWith("INSTRUCTIONS]")) {
                instructions = trimSec.substringAfter("INSTRUCTIONS]").trim()
            } else if (trimSec.startsWith("NUTRITION]")) {
                nutritionText = trimSec.substringAfter("NUTRITION]").trim()
            }
        }

        // Extract sub metric tags
        // Prep: 15 mins | Cook: 20 mins | Servings: 2 | Difficulty: Medium
        var prep = "15 mins"
        var cook = "20 mins"
        var servings = "2 servings"
        var difficulty = "Medium"
        metricText.split("|").forEach { m ->
            val part = m.trim()
            if (part.contains("Prep:", true)) prep = part.substringAfter(":").trim()
            else if (part.contains("Cook:", true)) cook = part.substringAfter(":").trim()
            else if (part.contains("Servings:", true)) servings = part.substringAfter(":").trim()
            else if (part.contains("Difficulty:", true)) difficulty = part.substringAfter(":").trim()
        }

        // Extract nutritional elements
        // Calories: 420 kcal | Protein: 22g | Carbs: 45g | Fat: 12g | Fiber: 6g
        var calories = 350
        var protein = "18g"
        var carbs = "40g"
        var fat = "10g"
        var fiber = "5g"
        nutritionText.split("|").forEach { n ->
            val part = n.trim()
            if (part.contains("Calories:", true)) {
                calories = part.substringAfter(":").trim().filter { it.isDigit() }.toIntOrNull() ?: 350
            } else if (part.contains("Protein:", true)) protein = part.substringAfter(":").trim()
            else if (part.contains("Carbs:", true) || part.contains("Carbohydrates:", true)) carbs = part.substringAfter(":").trim()
            else if (part.contains("Fat:", true)) fat = part.substringAfter(":").trim()
            else if (part.contains("Fiber:", true)) fiber = part.substringAfter(":").trim()
        }

        return RecipeEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            description = description,
            ingredients = ingredients,
            instructions = instructions,
            prepTime = prep,
            cookTime = cook,
            totalTime = "${prep.filter { it.isDigit() }.toIntOrNull()?.plus(cook.filter { it.isDigit() }.toIntOrNull() ?: 0) ?: 30} mins",
            difficulty = difficulty,
            servingSize = servings,
            calories = calories,
            protein = protein,
            carbohydrates = carbs,
            fat = fat,
            fiber = fiber,
            isFavorite = false
        )
    }

    private fun getMockRecipeForInputs(inputs: String, type: String): RecipeEntity {
        Log.d("CookMateRepository", "Using mock recipe template due to missing key or api error")
        val cleanInputs = inputs.lowercase()
        return when {
            cleanInputs.contains("paneer") || cleanInputs.contains("cottage cheese") -> RecipeEntity(
                id = "paneer_butter_masala",
                name = "Gourmet Paneer Tikka Masala",
                category = "Dinner",
                description = "Marinated cottage cheese cubes chargrilled in clay ovens and simmered in a velvet tomato cream gravy with rich fenugreek accents.",
                ingredients = "- 250g Paneer (Cottage Cheese) cut into cubes\n- 1 large Onion, finely chopped\n- 2 Tomatoes, pureed\n- 1 tbsp Ginger-garlic paste\n- 1 tsp Garam Masala\n- 1/2 tsp Turmeric powder\n- 1 tsp Kashmiri Red Chilli powder\n- 2 tbsp Fresh Cream\n- 1 tsp Kasoori Methi (dried fenugreek leaves)\n- 1 tbsp Butter",
                instructions = "1. Heat butter in a pan, sauté onions until translucent, then add ginger-garlic paste and spices.\n2. Pour tomato puree and cook on low heat until oil separates from the gravy.\n3. Add paneer cubes, splash in 1/2 cup water, cover and simmer gently for 5 minutes.\n4. Stir in fresh cream and crushed kasoori methi. Serve piping hot with flatbread or basmati rice.",
                prepTime = "15 mins",
                cookTime = "15 mins",
                totalTime = "30 mins",
                difficulty = "Medium",
                servingSize = "2 servings",
                calories = 380,
                protein = "16g",
                carbohydrates = "12g",
                fat = "30g",
                fiber = "3g",
                isFavorite = false
            )
            cleanInputs.contains("chicken") -> RecipeEntity(
                id = "chicken_stir_fry",
                name = "AI-Chef Honey Garlic Chicken Stir-fry",
                category = "Dinner",
                description = "Tender sliced chicken breast cooked in a sweet honey garlic glaze, tossed with bell peppers and fresh broccoli florets.",
                ingredients = "- 300g Chicken breast, thinly sliced\n- 1 cup Broccoli florets\n- 1 Red Bell Pepper, julienned\n- 3 cloves Garlic, minced\n- 2 tbsp Honey\n- 3 tbsp Soy sauce\n- 1 tbsp Olive oil\n- 1 tsp Sesame seeds for garnish",
                instructions = "1. Heat olive oil in a wide sauté pan and sauté chicken until golden brown and cooked through.\n2. Add garlic and vegetables, stir-frying on medium-high heat for 3-4 minutes until veggies are tender-crisp.\n3. Whisk honey and soy sauce together and pour into pan. Let simmer for 2 minutes until it thickens into a glossy coat.\n4. Garnish with toasted sesame seeds and serve over white rice.",
                prepTime = "10 mins",
                cookTime = "10 mins",
                totalTime = "20 mins",
                difficulty = "Easy",
                servingSize = "2 servings",
                calories = 310,
                protein = "28g",
                carbohydrates = "24g",
                fat = "8g",
                fiber = "4g",
                isFavorite = false
            )
            cleanInputs.contains("potato") || cleanInputs.contains("aloo") -> RecipeEntity(
                id = "crispy_aloo_herb",
                name = "Crispy Herb Roasted Potatoes",
                category = "Snack",
                description = "Golden wedge potatoes flavored with garlic, organic rosemary, and extra virgin olive oil.",
                ingredients = "- 3 Medium Potatoes, washed and wedged\n- 2 tbsp Olive oil\n- 1 tsp Garlic powder\n- 1/2 tsp Dried Rosemary\n- Sea salt and crushed black pepper to taste",
                instructions = "1. Preheat oven to 200°C (390°F) or prep your air fryer.\n2. Toss potato wedges in olive oil, garlic powder, rosemary, salt, and black pepper.\n3. Spread in a single layer on parchment sheets.\n4. Roast for 25-30 mins, rotating halfway, until perfectly gold and crispy on the edges.",
                prepTime = "10 mins",
                cookTime = "25 mins",
                totalTime = "35 mins",
                difficulty = "Easy",
                servingSize = "3 servings",
                calories = 190,
                protein = "3g",
                carbohydrates = "32g",
                fat = "6g",
                fiber = "4g",
                isFavorite = false
            )
            else -> RecipeEntity(
                id = "healthy_culinary_medley",
                name = "CookMate Superfood Green Medley",
                category = "Lunch",
                description = "A warm, high-fiber nutritious grain bowl featuring seasonal greens, avocado, toasted chickpeas, and tahini drizzle.",
                ingredients = "- 1 cup Cooked Quinoa or Brown Rice\n- 1/2 cup Boiled Chickpeas\n- 1 cup Chopped Spinach/Kale\n- 1/2 Avocado, sliced\n- 1 tbsp Tahini paste\n- Lemon juice, salt and pepper to taste",
                instructions = "1. Toss kale/spinach with a splash of fresh lemon juice and sea salt.\n2. Assemble a grain base of quinoa, then partition chickpeas, greens, and creamy avocado slices on top.\n3. Whisk tahini with a tablespoon of warm water and lemon juice to create a smooth cream consistency.\n4. Drizzle tahini cream over the superfood setup, adjust black pepper, and serve fresh.",
                prepTime = "10 mins",
                cookTime = "5 mins",
                totalTime = "15 mins",
                difficulty = "Easy",
                servingSize = "1 serving",
                calories = 340,
                protein = "12g",
                carbohydrates = "48g",
                fat = "14g",
                fiber = "9g",
                isFavorite = false
            )
        }
    }

    private fun getOfflineMockResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("substitute") || lower.contains("alternative") -> """
                CookMate AI Substitute Engine:
                For missing ingredients, here are clinical & allergen-safe replacements:
                1. Butter -> Extra Virgin Olive Oil (1:1) or Mashed Avocado (for moist baking).
                2. Paneer/Cheese -> Extra Firm Organic Tofu or Cashew Nut Cheese.
                3. Heavy Cream -> Coconut Cream or Blended Cashew Butter Paste (allergy-safe: Sunflower cream).
                4. Egg -> 1 tbsp Ground Chia Seeds soaked in 3 tbsp warm water (Chia egg) or Applesauce.
            """.trimIndent()
            lower.contains("grocery") || lower.contains("optimize") -> """
                CookMate Shopping Optimizer Suggestions:
                - Purchase bulk staples (Rice, Lentils, Spices) at local co-ops to save up to 22% cost.
                - Substitute out-of-season fresh berries with organic frozen mixes.
                - Reuse leftovers: Leftover roasted cauliflower can be pureed into soup bases next morning to minimize grocery waste.
            """.trimIndent()
            lower.contains("scanned") || lower.contains("fridge") || lower.contains("recognize") -> """
                [SCANNED INGREDIENTS DETECTED]
                - Fresh Tomato (Grade A)
                - Spinach Greens (Organic)
                - Brown Eggs
                - Red Bell Pepper
                
                CookMate instant recipe suggestion: "Italian Herb Egg Scramble with Sautéed Bell Peppers & Tomatoes". Nutrition estimated: 240kcal, 18g Protein. High micronutrients.
            """.trimIndent()
            lower.contains("coach") || lower.contains("nutrition") || lower.contains("macro") -> """
                * CookMate Personal Nutrition Coach Session *
                Goals analyzed: Active Wellness & Nutrient Density
                - Daily recommendation: Target 2,050 Kcal.
                - Macros: 130g Protein, 210g Carbs, 60g Healthy Fats.
                - Pro-tip: Hydrate with 3.2L filtered water daily and include 35g fiber to optimize metabolic rate and stamina.
            """.trimIndent()
            else -> """
                Welcome to CookMate AI. Add your official API key in the Secrets Panel to gain unlimited real-time AI recipe generation, personalized nutrition consultations, and Fridge Scans. 
                
                For this offline session: Simulating Masterchef Cooking Mode. Let us cook healthy culinary triumphs together!
            """.trimIndent()
        }
    }
}
