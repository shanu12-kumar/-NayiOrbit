package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.GroceryEntity
import com.example.data.database.RecipeEntity
import com.example.data.database.UserProfileEntity
import com.example.data.repository.CookMateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "User" or "Chef AI"
    val text: String,
    val time: Long = System.currentTimeMillis()
)

class CookMateViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = CookMateRepository(
        userProfileDao = db.userProfileDao(),
        recipeDao = db.recipeDao(),
        groceryDao = db.groceryDao()
    )

    // Flow observations
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfileFlow
        .map { it ?: UserProfileEntity() } // Default profile if empty
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    val favoriteRecipes: StateFlow<List<RecipeEntity>> = repository.favoriteRecipesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<List<RecipeEntity>> = repository.allRecipesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groceryList: StateFlow<List<GroceryEntity>> = repository.allGroceriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive States
    private val _recipeGenerationState = MutableStateFlow<UiState<RecipeEntity>>(UiState.Idle)
    val recipeGenerationState: StateFlow<UiState<RecipeEntity>> = _recipeGenerationState.asStateFlow()

    private val _aiKitchenState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiKitchenState: StateFlow<UiState<String>> = _aiKitchenState.asStateFlow()

    // Smart Filter Selections
    private val _selectedDietFilter = MutableStateFlow("All")
    val selectedDietFilter: StateFlow<String> = _selectedDietFilter.asStateFlow()

    private val _selectedCuisineFilter = MutableStateFlow("All")
    val selectedCuisineFilter: StateFlow<String> = _selectedCuisineFilter.asStateFlow()

    // Interactive Chef Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(sender = "Chef AI", text = "Bon appétit! I am your AI Chef Assistant. Ask me anything about cooking, swaps, or leftover tips!"))
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // active recipe for guidance & hands-free voice mode
    private val _activeRecipe = MutableStateFlow<RecipeEntity?>(null)
    val activeRecipe: StateFlow<RecipeEntity?> = _activeRecipe.asStateFlow()

    private val _activeStepIndex = MutableStateFlow(0)
    val activeStepIndex: StateFlow<Int> = _activeStepIndex.asStateFlow()

    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    // Language setting
    private val _appLanguage = MutableStateFlow("English")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    init {
        // Initialize user profile in DB on startup if it doesn't exist
        viewModelScope.launch {
            val p = repository.getProfile()
            if (p == null) {
                repository.saveProfile(UserProfileEntity())
            }
        }
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            repository.saveProfile(current.copy(language = lang))
        }
    }

    fun selectDiet(diet: String) {
        _selectedDietFilter.value = diet
    }

    fun selectCuisine(cuisine: String) {
        _selectedCuisineFilter.value = cuisine
    }

    // Toggle Favorite
    fun toggleFavorite(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.saveRecipe(recipe.copy(isFavorite = !recipe.isFavorite))
        }
    }

    // Save dynamic recipe
    fun saveCustomRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.saveRecipe(recipe)
        }
    }

    // Delete dynamic recipe
    fun deleteCustomRecipe(id: String) {
        viewModelScope.launch {
            repository.deleteRecipeById(id)
        }
    }

    // Generate Ingredients-Based or AI Recipe
    fun generateAiRecipe(inputs: String, type: String = "Ingredients") {
        _recipeGenerationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.generateRecipe(inputs, type)
                _recipeGenerationState.value = UiState.Success(result)
                incrementStreak()
            } catch (e: Exception) {
                _recipeGenerationState.value = UiState.Error(e.localizedMessage ?: "Unknown Error occurred.")
            }
        }
    }

    fun resetRecipeGenState() {
        _recipeGenerationState.value = UiState.Idle
    }

    // AI Fridge scanner simulate
    fun simulateFridgeScan(imageUriDescription: String) {
        _aiKitchenState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val prompt = "Analyze this fridge stock image. Ingredients present: $imageUriDescription. Give grocery detection count and list potential fast recipes that minimize food waste."
                val output = repository.askGemini(prompt)
                _aiKitchenState.value = UiState.Success(output)
            } catch (e: Exception) {
                _aiKitchenState.value = UiState.Error(e.localizedMessage ?: "Failed scanning fridge photo.")
            }
        }
    }

    // AI Food photo recognizer
    fun simulateFoodRecognizer(dishBrief: String) {
        _aiKitchenState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val prompt = "Analyze this high-resolution food snapshot. Dish: $dishBrief. Identify dish name, estimate Calories, protein, fat, fiber. Provide healthier organic substitutes."
                val output = repository.askGemini(prompt)
                _aiKitchenState.value = UiState.Success(output)
            } catch (e: Exception) {
                _aiKitchenState.value = UiState.Error(e.localizedMessage ?: "Failed recognizing food item.")
            }
        }
    }

    fun resetKitchenState() {
        _aiKitchenState.value = UiState.Idle
    }

    // AI Chef Interaction
    fun sendChefMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(sender = "User", text = text)
        _chatMessages.update { it + userMsg }

        viewModelScope.launch {
            val response = repository.askGemini(
                prompt = text,
                systemInstruction = "You are CookMate Chef, an expert culinary master, micro-nutritionist, and friendly advisor. Answer kitchen queries under 3 sentences with warmth and precision."
            )
            val chefMsg = ChatMessage(sender = "Chef AI", text = response)
            _chatMessages.update { it + chefMsg }
        }
    }

    // Active Recipe Guidance & Voice Commands
    fun startRecipeGuidance(recipe: RecipeEntity) {
        _activeRecipe.value = recipe
        _activeStepIndex.value = 0
        _isVoiceActive.value = false
    }

    fun closeGuidance() {
        _activeRecipe.value = null
        _activeStepIndex.value = 0
        _isVoiceActive.value = false
    }

    fun toggleVoiceMode() {
        _isVoiceActive.value = !_isVoiceActive.value
    }

    fun executeVoiceCommand(command: String) {
        val stepsCount = _activeRecipe.value?.instructions?.split("\n")?.filter { it.isNotBlank() }?.size ?: 0
        when {
            command.equals("Next Step", true) -> {
                if (_activeStepIndex.value < stepsCount - 1) {
                    _activeStepIndex.value++
                }
            }
            command.equals("Previous Step", true) -> {
                if (_activeStepIndex.value > 0) {
                    _activeStepIndex.value--
                }
            }
            command.equals("Restart Staging", true) -> {
                _activeStepIndex.value = 0
            }
        }
    }

    // Shopping list logic
    fun addGrocery(name: String, category: String, qty: String, cost: Double = 0.0) {
        viewModelScope.launch {
            repository.addGroceryItem(
                GroceryEntity(
                    name = name,
                    category = category,
                    quantity = qty,
                    estimatedCost = cost
                )
            )
        }
    }

    fun toggleGroceryPurchased(item: GroceryEntity) {
        viewModelScope.launch {
            repository.updateGroceryItem(item.copy(isPurchased = !item.isPurchased))
        }
    }

    fun deleteGrocery(id: Int) {
        viewModelScope.launch {
            repository.deleteGroceryById(id)
        }
    }

    fun clearPurchased() {
        viewModelScope.launch {
            repository.clearPurchasedGroceries()
        }
    }

    fun populateIngredientsToGrocery(recipe: RecipeEntity) {
        viewModelScope.launch {
            val rawIngs = recipe.ingredients.split("\n")
            rawIngs.forEach { raw ->
                val clean = raw.trim().removePrefix("-").trim()
                if (clean.isNotBlank()) {
                    val qty = if (clean.contains(" ")) clean.substringBefore(" ") else "1 unit"
                    val name = if (clean.contains(" ")) clean.substringAfter(" ") else clean
                    repository.addGroceryItem(
                        GroceryEntity(
                            name = name,
                            category = "Recipe Essentials",
                            quantity = qty,
                            estimatedCost = 0.85,
                            originalRecipeId = recipe.id
                        )
                    )
                }
            }
        }
    }

    // Update Profile Parameters
    fun updateProfile(
        age: Int,
        gender: String,
        height: Double,
        weight: Double,
        fitnessGoal: String,
        dietaryPref: String,
        allergies: String,
        cuisines: String
    ) {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            // Estimate appropriate targets based on fitness plan
            val targetCalories = when (fitnessGoal) {
                "Weight Loss" -> 1600
                "Muscle Gain" -> 2500
                "Athletic Endurance" -> 2800
                else -> 2000
            }
            val targetProtein = when (fitnessGoal) {
                "Muscle Gain" -> 150
                "Weight Loss" -> 110
                else -> 90
            }

            repository.saveProfile(
                current.copy(
                    age = age,
                    gender = gender,
                    height = height,
                    weight = weight,
                    fitnessGoal = fitnessGoal,
                    dietaryPref = dietaryPref,
                    allergies = allergies,
                    preferredCuisines = cuisines,
                    caloryTarget = targetCalories,
                    proteinTarget = targetProtein
                )
            )
        }
    }

    // Gamification Incrementor
    private fun incrementStreak() {
        viewModelScope.launch {
            val current = repository.getProfile() ?: UserProfileEntity()
            val now = System.currentTimeMillis()
            // simple check: if a day passed since last cooked
            if (now - current.lastCookedDate > 12 * 60 * 60 * 1000) {
                repository.saveProfile(
                    current.copy(
                        streakCount = current.streakCount + 1,
                        lastCookedDate = now
                    )
                )
            }
        }
    }
}
