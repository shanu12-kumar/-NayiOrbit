package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.database.*
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.CookMateViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookMateApp(
    viewModel: CookMateViewModel = viewModel()
) {
    val currentTab = remember { mutableStateOf("Home") }
    val activeRecipe by viewModel.activeRecipe.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (activeRecipe == null) {
                CookMateBottomBar(currentTab = currentTab, language = appLanguage)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeRecipe,
                transitionSpec = {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
                },
                label = "RecipeDetailAndCore"
            ) { targetRecipe ->
                if (targetRecipe != null) {
                    RecipeDetailScreen(
                        recipe = targetRecipe,
                        viewModel = viewModel,
                        onClose = { viewModel.closeGuidance() }
                    )
                } else {
                    Crossfade(
                        targetState = currentTab.value,
                        animationSpec = tween(200),
                        label = "TabSwitcher"
                    ) { tab ->
                        when (tab) {
                            "Home" -> HomeScreen(viewModel, onTabSelect = { currentTab.value = it })
                            "Kitchen" -> AIKitchenScreen(viewModel)
                            "Planner" -> MealPlannerScreen(viewModel)
                            "Groceries" -> GroceryListScreen(viewModel)
                            "Profile" -> UserProfileScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- BOTTOM NAVIGATION BAR ----------------
@Composable
fun CookMateBottomBar(
    currentTab: MutableState<String>,
    language: String
) {
    val items = listOf("Home", "Kitchen", "Planner", "Groceries", "Profile")
    val icons = mapOf(
        "Home" to (Icons.Outlined.Home to Icons.Filled.Home),
        "Kitchen" to (Icons.Outlined.ContentPaste to Icons.Filled.ContentPaste),
        "Planner" to (Icons.Outlined.DateRange to Icons.Filled.DateRange),
        "Groceries" to (Icons.Outlined.ShoppingBag to Icons.Filled.ShoppingBag),
        "Profile" to (Icons.Outlined.Person to Icons.Filled.Person)
    )

    val labels = if (language == "Hindi") {
        mapOf("Home" to "होम", "Kitchen" to "रसोई", "Planner" to "योजना", "Groceries" to "सामान", "Profile" to "प्रोफाइल")
    } else {
        mapOf("Home" to "Home", "Kitchen" to "AI Kitchen", "Planner" to "Planner", "Groceries" to "Groceries", "Profile" to "Profile")
    }

    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            val isSelected = currentTab.value == item
            val iconPair = icons[item] ?: (Icons.Outlined.Home to Icons.Filled.Home)
            val icon = if (isSelected) iconPair.second else iconPair.first

            NavigationBarItem(
                selected = isSelected,
                onClick = { currentTab.value = item },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = item,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = labels[item] ?: item,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier.testTag("nav_tab_${item.lowercase()}")
            )
        }
    }
}

// ---------------- HEADER HELPER ----------------
@Composable
fun SectionHeader(title: String, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------- TABS: 1. HOME SCREEN ----------------
@Composable
fun HomeScreen(viewModel: CookMateViewModel, onTabSelect: (String) -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteRecipes.collectAsStateWithLifecycle()
    val groceries by viewModel.groceryList.collectAsStateWithLifecycle()
    val isHindi = userProfile.language == "Hindi"

    val featuredRecipe = remember(favorites, allRecipes) {
        favorites.firstOrNull() ?: allRecipes.firstOrNull() ?: getPredefinedRecommendations(isHindi).firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Welcome Header & Streak
        item {
            HomeGreetingBanner(profile = userProfile, isHindi = isHindi)
        }

        // BENTO GRID DASHBOARD
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Featured Recipe (Large Card - ColSpan 2)
                if (featuredRecipe != null) {
                    BentoFeaturedCard(
                        recipe = featuredRecipe,
                        isHindi = isHindi,
                        onClick = { viewModel.startRecipeGuidance(featuredRecipe) }
                    )
                }

                // 2. Row containing Streak (Peach) + Fridge Scan (Soft Blue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoStreakCard(
                        streakCount = userProfile.streakCount,
                        isHindi = isHindi,
                        modifier = Modifier.weight(1f)
                    )
                    BentoScanCard(
                        isHindi = isHindi,
                        onScan = {
                            viewModel.simulateFridgeScan("Tomatoes, Spinach, Eggs, Red Pepper")
                            onTabSelect("Kitchen")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Row containing Nutrition progress bars + AI Planner card (Dark charcoal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoNutritionCard(
                        profile = userProfile,
                        isHindi = isHindi,
                        modifier = Modifier.weight(1f)
                    )
                    BentoPlannerCard(
                        fitnessGoal = userProfile.fitnessGoal,
                        isHindi = isHindi,
                        onViewPlanner = { onTabSelect("Planner") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Grocery List wide card (Light gray sand)
                BentoGroceryCard(
                    groceriesCount = groceries.size,
                    isHindi = isHindi,
                    onClick = { onTabSelect("Groceries") }
                )
            }
        }

        // Seasonal Engine Recommendations (Styled beautifully as Bento card)
        item {
            SeasonalEngineBanner(isHindi = isHindi, onGetRecipes = {
                viewModel.generateAiRecipe("Healthy seasonal organic items", "Seasonal Engine")
            })
        }

        // Saved Recipes shortcuts
        item {
            SectionHeader(
                title = if (isHindi) "पसंदीदा व्यंजन" else "Saved Recipes Shortcut",
                actionText = if (favorites.isNotEmpty()) (if (isHindi) "सभी देखें" else "View All") else null
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RoundedCornerShape(28.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = "No Favorites",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHindi) "कोई पसंदीदा सहेज नहीं गया है। रसोई टैब से जोड़ें!" else "No saved recipes yet. Mark heart on any AI generation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favorites) { item ->
                        RecommendationCard(recipe = item, onClick = {
                            viewModel.startRecipeGuidance(item)
                        })
                    }
                }
            }
        }

        // Gamification / Level section
        item {
            GamificationProgressCard(profile = userProfile, isHindi = isHindi)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ---------------- WELCOME HEADER BANNER ----------------
@Composable
fun HomeGreetingBanner(profile: UserProfileEntity, isHindi: Boolean) {
    val currentDateStr = remember {
        try {
            java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(java.util.Date())
        } catch (e: Exception) {
            "Tuesday, Oct 24"
        }
    }
    
    val greeting = if (isHindi) "नमस्ते, शेफ ${profile.gender}!" else "Hello, Chef ${profile.gender}!"
    val sub = if (isHindi) "चलो आज कुछ सेहतमंद और स्वादिष्ट बनाते हैं!" else "Let's craft healthy kitchen victories today!"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Dynamic Date at the top of the bento dashboard
        Text(
            text = currentDateStr.uppercase(java.util.Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Chef Initials Profile Circle Avatar (Bento Style)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), CircleShape)
            ) {
                Text(
                    text = if (profile.gender.isNotEmpty()) profile.gender.take(1).uppercase() else "C",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Demo Key Notice Check
        val isKeyOk = com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && 
                      com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" && 
                      !com.example.BuildConfig.GEMINI_API_KEY.contains("PLACEHOLDER")
                      
        if (!isKeyOk) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Offline Mode",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "ऑफ़लाइन मोड सक्रिय। पूर्ण लाइव AI के लिए सीक्रेट्स पैनल में GEMINI_API_KEY सेट करें।" else "Offline Demo Mode Active. Set GEMINI_API_KEY in Secrets Panel for real-time AI scans.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

// ---------------- BENTO GRID ELEMENT COMPOSABLES ----------------

// 1. FEATURED RECIPE CARD (LARGE PANEL)
@Composable
fun BentoFeaturedCard(
    recipe: RecipeEntity,
    isHindi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip / Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (recipe.category).uppercase(java.util.Locale.getDefault()),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // AI Sparkle Indicator Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) "एआई सिफारिश" else "FEATURED AI PICK",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recipe Name
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Brief Description
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Calories & Prep Time Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calorie pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Calories",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${recipe.calories} kcal",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Time pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "Prep Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = recipe.prepTime,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// 2. STREAK BENTO CARD (SMALL PORTRAIT SQUARE)
@Composable
fun BentoStreakCard(
    streakCount: Int,
    isHindi: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSecondaryContainer),
        border = BorderStroke(1.dp, BentoOnSecondaryContainer.copy(alpha = 0.1f)),
        modifier = modifier
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "$streakCount " + (if (isHindi) "दिन" else "Days"),
                    color = BentoOnSecondaryContainer,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp
                )
                Text(
                    text = if (isHindi) "लगातार शेफ लय" else "Cooking Streak",
                    color = BentoOnSecondaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 3. FRIDGE SCANNER CARD (SMALL PORTRAIT SQUARE)
@Composable
fun BentoScanCard(
    isHindi: Boolean,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BentoInfoContainer),
        border = BorderStroke(1.dp, BentoOnInfoContainer.copy(alpha = 0.1f)),
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onScan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Scan",
                    tint = BentoOnInfoContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = if (isHindi) "फ्रिज स्कैनर" else "Fridge Scan",
                    color = BentoOnInfoContainer,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                )
                Text(
                    text = if (isHindi) "एआई सामग्री फोटो" else "Scan your leftovers",
                    color = BentoOnInfoContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 4. SUMMARY NUTRITION CARD (MEDIUM COMPACT VERTICAL)
@Composable
fun BentoNutritionCard(
    profile: UserProfileEntity,
    isHindi: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
            .height(190.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isHindi) "दैनिक पोषण" else "Nutrition",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "1,460",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = " / ${profile.caloryTarget} " + (if (isHindi) "कैलोरी" else "kcal"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )
                }
            }

            // Simple macros stack
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroSlimBar(
                    label = if (isHindi) "प्रोटीन" else "Protein",
                    current = "85g",
                    target = "${profile.proteinTarget}g",
                    percent = 0.71f,
                    color = ForestGreen
                )
                MacroSlimBar(
                    label = if (isHindi) "कार्ब्स" else "Carbs",
                    current = "155g",
                    target = "${profile.carbTarget}g",
                    percent = 0.65f,
                    color = GoldAmber
                )
                MacroSlimBar(
                    label = if (isHindi) "वसा" else "Fats",
                    current = "44g",
                    target = "${profile.fatTarget}g",
                    percent = 0.55f,
                    color = SpiceOrange
                )
            }
        }
    }
}

@Composable
fun MacroSlimBar(label: String, current: String, target: String, percent: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Text(text = "$current / $target", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

// 5. AI PLANNER CARD (MEDIUM LUXURIOUS DARK VERTICAL)
@Composable
fun BentoPlannerCard(
    fitnessGoal: String,
    isHindi: Boolean,
    onViewPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191C19)),
        modifier = modifier
            .height(190.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "एआई योजनाकार" else "AI Planner",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Planner",
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHindi) "मैक्रो लक्ष्य: $fitnessGoal" else "Tailored meals matching: $fitnessGoal",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onViewPlanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Text(
                    text = if (isHindi) "योजना देखें" else "View Plan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// 6. GROCERY BENTO CARD (WIDE LANDSCAPE BAR)
@Composable
fun BentoGroceryCard(
    groceriesCount: Int,
    isHindi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = "Grocery",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isHindi) "स्मार्ट राशन सामग्री सूची" else "Your Shopping List",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (groceriesCount == 0) {
                            (if (isHindi) "सप्ताह के लिए कोई वस्तु बाकी नहीं" else "Every ingredient is fully stocked!")
                        } else {
                            if (isHindi) "रसोई के लिए $groceriesCount वस्तुएं आवश्यक" else "$groceriesCount ingredients set to purchase"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ---------------- REST OF HOME COMPOSABLES WITH BENTO EDITS ----------------

@Composable
fun RecommendationCard(recipe: RecipeEntity, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (recipe.name) {
                        "Avocado Chia Pudding", "एवोकैडो चिया पुडिंग" -> Icons.Outlined.Icecream
                        "Mediterranean Salad Bowl", "भूमध्यसागरीय सलाद" -> Icons.Outlined.Spa
                        else -> Icons.Outlined.RestaurantMenu
                    },
                    contentDescription = recipe.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = recipe.category.uppercase(java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${recipe.calories} kcal",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = recipe.prepTime,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonalEngineBanner(isHindi: Boolean, onGetRecipes: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) "🌻 ग्रीष्म कालीन जैविक नुस्खे" else "☀️ Summer Seasonal Engine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHindi) "स्थानीय गर्मियों की ताजी उपज का अनुकूलन करें और अपशिष्ट को 40% घटाएं।" else "Optimize local summer fresh arrivals and reduce prep food waste today up to 40%.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGetRecipes,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isHindi) "स्थानीय व्यंजन खोजें" else "Explore Regional Dishes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = "Plant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun GamificationProgressCard(profile: UserProfileEntity, isHindi: Boolean) {
    val level = when {
        profile.streakCount < 3 -> 1
        profile.streakCount < 6 -> 2
        profile.streakCount < 10 -> 3
        else -> 4
    }
    val levelName = when (level) {
        1 -> if (isHindi) "नौसिखिया शेफ" else "Novice Cook"
        2 -> if (isHindi) "रसोई नायक" else "Kitchen Hero"
        3 -> if (isHindi) "स्तर 3: पाक विशेषज्ञ" else "Level 3: Culinary Sage"
        else -> if (isHindi) "स्तर 4: एआई मास्टरशेफ" else "Level 4: AI Masterchef"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)), CircleShape)
            ) {
                Text(
                    text = "Lvl $level",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = levelName,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isHindi) "कल नए व्यंजन बनाने पर +50 XP मिलेगा!" else "Earn +50 XP on cooking a new recipe tomorrow!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.45f }, // XP simulated
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// ---------------- TABS: 2. AI KITCHEN SCREEN ----------------
@Composable
fun AIKitchenScreen(viewModel: CookMateViewModel) {
    val kitchenState by viewModel.aiKitchenState.collectAsStateWithLifecycle()
    val recipeGenState by viewModel.recipeGenerationState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isHindi = userProfile.language == "Hindi"

    val ingredientInput = remember { mutableStateOf("") }
    val selectedOptionType = remember { mutableStateOf("Fusion") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Welcome and intro
        item {
            Text(
                text = if (isHindi) "एआई रसोईघर" else "AI Kitchen Lab",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isHindi) "गैली में खाना फालतू न फेंके; स्मार्ट एआई से नए आविष्कार बनाएं" else "Cook with what is left. Minimize waste with custom AI logic.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section A: Smart Ingredient chip generator
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "सामग्री आधारित एआई जेनेरेटर" else "Ingredient-Based AI Generator",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ingredientInput.value,
                        onValueChange = { ingredientInput.value = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "पनीर, टमाटर, आलू, प्याज..." else "Enter paneer, chicken, broccoli..."
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                // Simulate Voice input
                                ingredientInput.value = if (isHindi) "टमाटर, शिमला मिर्च, आलू" else "Paneer, bell peppers"
                            }) {
                                Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingredient_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Chips
                    Text(text = if (isHindi) "त्वरित सुझाव:" else "Quick inserts:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val items = listOf("Paneer", "Chicken", "Broccoli", "Tomato", "Potato", "Spinach")
                        items.forEach { chipName ->
                            SuggestionChip(
                                onClick = {
                                    val currentText = ingredientInput.value
                                    if (currentText.isBlank()) {
                                        ingredientInput.value = chipName
                                    } else if (!currentText.contains(chipName, true)) {
                                        ingredientInput.value = "$currentText, $chipName"
                                    }
                                },
                                label = { Text(text = chipName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (ingredientInput.value.isNotBlank()) {
                                viewModel.generateAiRecipe(ingredientInput.value, "Manual Ingredients")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = if (isHindi) "रेसिपी बनाएं" else "Generate Culinary Art")
                    }
                }
            }
        }

        // Section B: Option type: Fusion, Restaurant-Style, Budget-Friendly
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf("Fusion", "Restaurant Mode", "Budget Meals")
                modes.forEach { mode ->
                    val isSelected = selectedOptionType.value == mode
                    Button(
                        onClick = {
                            selectedOptionType.value = mode
                            ingredientInput.value = when(mode) {
                                "Fusion" -> "Butter Chicken Tacos"
                                "Restaurant Mode" -> "Famous Restaurant Paneer Lababdar"
                                "Budget Meals" -> "Under 2 Dollars Potato Medley"
                                else -> ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = mode, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // Section C: Premium AI Lab Scanners
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "प्रीमियम एआई स्कैनर्स" else "Premium AI Lab Scanners",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isHindi) "स्मार्ट रेकॉग्निशन से खाना और फ्रिज का विश्लेषण करें" else "Scan your real-world meals or fridge stock items.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Fridge scanner
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .clickable {
                                    viewModel.simulateFridgeScan("Tomatoes, Spinach, Eggs, Red Pepper")
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi) "एआई फ्रिज स्कैनर" else "Fridge Scanner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isHindi) "सामग्री डिटेक्ट करें" else "Detect Vegetables",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Food Recognizer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .clickable {
                                    viewModel.simulateFoodRecognizer("Bowl of Homemade Chicken Tikka Fried Rice")
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Camera,
                                contentDescription = "Scan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi) "भोजन पहचान (फोटो)" else "Food Recognition",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isHindi) "कैलोरी अनुमान लगाएं" else "Estimate Nutrition",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Section D: Output display states
        item {
            RecipeGenerationOutputSection(kitchenState, recipeGenState, viewModel, isHindi)
        }
    }
}

@Composable
fun RecipeGenerationOutputSection(
    kitchenState: UiState<String>,
    recipeGenState: UiState<RecipeEntity>,
    viewModel: CookMateViewModel,
    isHindi: Boolean
) {
    if (kitchenState != UiState.Idle || recipeGenState != UiState.Idle) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "एआई पाक प्रयोगशाला परिणाम" else "AI Lab Operations Output",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = {
                        viewModel.resetKitchenState()
                        viewModel.resetRecipeGenState()
                    }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Check combined loading
                if (kitchenState == UiState.Loading || recipeGenState == UiState.Loading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "रसोइया एआई सोच रहा है..." else "Masterchef AI model is calculating organic fits...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Check text state
                if (kitchenState is UiState.Success) {
                    Text(
                        text = kitchenState.data,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Check dynamic recipe state
                if (recipeGenState is UiState.Success) {
                    val recipe = recipeGenState.data
                    Text(
                        text = if (isHindi) "व्यंजन सफलतापूर्वक तैयार!" else "Custom Recipe Generated!",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.startRecipeGuidance(recipe)
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.RestaurantMenu,
                                contentDescription = "Recipe",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = recipe.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "${recipe.calories} kcal • ${recipe.prepTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (kitchenState is UiState.Error) {
                    Text(text = "Error: " + kitchenState.message, color = MaterialTheme.colorScheme.error)
                }
                if (recipeGenState is UiState.Error) {
                    Text(text = "Error: " + recipeGenState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ---------------- TABS: 3. MEAL PLANNER SCREEN ----------------
@Composable
fun MealPlannerScreen(viewModel: CookMateViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isHindi = userProfile.language == "Hindi"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = if (isHindi) "एआई भोजन समन्वयक" else "AI Meal Planner",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isHindi) "दैनिक, साप्ताहिक या मासिक वजन घटाने और मैक्रो लक्ष्यों को संरेखित करें" else "Align daily / weekly nutrition and physical fitness routines.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Coach Consultation segment
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "पोषण कोच विश्लेषण" else "AI Nutrition Coach Consult",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHindi) "पसंदीदा कैलोरी लक्ष्य: ${userProfile.caloryTarget}kcal. कोच से त्वरित स्वास्थ्य योजना लें!" else "Caloric goal: ${userProfile.caloryTarget}kcal. Connect with AI Coach now!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.generateAiRecipe("Establish macro weight loss fitness planner", "Nutrition Coach")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isHindi) "स्वास्थ्य रिपोर्ट लें" else "Request Nutrition Target Plan", fontSize = 11.sp)
                        }
                    }
                    Icon(imageVector = Icons.Filled.Spa, contentDescription = "Coach Health", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                }
            }
        }

        // Calendar planner tabs
        item {
            Text(
                text = if (isHindi) "आज की भोजन समय सारणी" else "Today's Meal Blueprint Plan",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Meal grid elements: Breakfast, Lunch, Dinner, Snack
        val mealSlots = getMockMealsList(isHindi)
        items(mealSlots) { slot ->
            MealSlotItemCard(slot, onCook = {
                viewModel.startRecipeGuidance(slot.recipe)
            })
        }

        // Family Meal Plan switch alert
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Group, contentDescription = "Family", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "सक्रिय फैमिली योजना (सक्रिय)" else "Active Family Profile Matching (Enabled)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isHindi) "बच्चों के लिए उपयुक्त कम मिर्च + बुजुर्गों के लिए अनुकूल मैक्रो संतुलन समन्वित।" else "Coordinating low-spices for kids + cholesterol conscious recipes for senior members.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class MealSlot(
    val title: String,
    val time: String,
    val rName: String,
    val calories: String,
    val recipe: RecipeEntity
)

fun getMockMealsList(isHindi: Boolean): List<MealSlot> {
    return listOf(
        MealSlot(
            title = if (isHindi) "सुबह का नाश्ता (Breakfast)" else "Breakfast slot",
            time = "08:30 AM",
            rName = if (isHindi) "एवोकैडो चिया पुडिंग" else "Avocado Chia Superfood Pudding",
            calories = "280 kcal",
            recipe = RecipeEntity(
                id = "pudding_avoc_001",
                name = if (isHindi) "एवोकैडो चिया पुडिंग" else "Avocado Chia Pudding",
                category = "Breakfast",
                description = "Nutrient-dense chia seeds soaked in unsweetened almond milk topped with mashed ripe avocado, sliced fresh almonds, and premium berries.",
                ingredients = "- 3 tbsp Chia seeds\n- 1 cup Almond milk (unsweetened)\n- 1/2 ripe Avocado\n- 1 tsp Honey\n- 5 Blueberries",
                instructions = "1. Whisk chia seeds, honey, and almond milk inside a bowl. Keep in the fridge for at least 2 hours.\n2. Layer pureed ripe avocado onto the pudding base.\n3. Add sliced almonds & fresh blueberries on top. Best served cold.",
                calories = 280,
                isFavorite = false
            )
        ),
        MealSlot(
            title = if (isHindi) "दोपहर का भोजन (Lunch)" else "Lunch slot",
            time = "01:30 PM",
            rName = if (isHindi) "भूमध्यसागरीय सलाद" else "Mediterranean Quinoa Greens Salad",
            calories = "420 kcal",
            recipe = RecipeEntity(
                id = "salad_quinoa_002",
                name = if (isHindi) "भूमध्यसागरीय सलाद" else "Mediterranean Salad Bowl",
                category = "Lunch",
                description = "A colorful high-protein Mediterranean super bowl filled with organic boiled quinoa, rich cherry tomatoes, crunchy cucumber, tossed in lemon vinaigrette.",
                ingredients = "- 1 cup Cooked Quinoa\n- 1/2 cup Chickpeas\n- 1 Cucumber, diced\n- 5 Olive slices\n- 1 tbsp olive oil\n- 1 tbsp lemon juice",
                instructions = "1. Boil quinoa completely and let cool down.\n2. Toss diced cucumber, boiled chickpeas, olives, and fresh lemon-olive-oil vinaigrette inside a spacious mixing bowl.\n3. Garnish with mint sprigs. Quick, balanced, organic.",
                calories = 420,
                isFavorite = false
            )
        ),
        MealSlot(
            title = if (isHindi) "साम का नाश्ता (Snack)" else "Snacks & Refreshment slot",
            time = "05:00 PM",
            rName = if (isHindi) "मसालेदार भुने हुए चने" else "Spicy Grilled Chickpeas Medley",
            calories = "180 kcal",
            recipe = RecipeEntity(
                id = "chickpeas_crisp_003",
                name = if (isHindi) "मसालेदार भुने हुए चने" else "Spicy Roasted Chickpeas",
                category = "Snack",
                description = "Crunchy high-fiber oven-baked garbanzo beans tossed in organic ground spices and a touch of mustard oil.",
                ingredients = "- 1.5 cups Boiled Chickpeas\n- 1/2 tsp Kashmiri Red Chilli powder\n- 1/2 tsp Cumin powder\n- 1 tsp Himalayan salt\n- 1 tbsp Mustard oil",
                instructions = "1. Pat chickpeas dry. Toss with oil and spices.\n2. Spread on the baking tray.\n3. Bake at 190C for 22 mins or air-fry until extra golden crispy.",
                calories = 180,
                isFavorite = false
            )
        )
    )
}

@Composable
fun MealSlotItemCard(slot: MealSlot, onCook: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = slot.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "• ${slot.time}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = slot.rName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = slot.calories, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onCook,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Details", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

// ---------------- TABS: 4. GROCERY LIST SCREEN ----------------
@Composable
fun GroceryListScreen(viewModel: CookMateViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val groceries by viewModel.groceryList.collectAsStateWithLifecycle()
    val isHindi = userProfile.language == "Hindi"

    val newGroceryName = remember { mutableStateOf("") }
    val newGroceryQty = remember { mutableStateOf("1 unit") }
    val newGroceryCat = remember { mutableStateOf("Produce") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = if (isHindi) "स्मार्ट राशन सामग्री सूची" else "Smart Grocery List",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isHindi) "स्वचालित राशन मूल्य विश्लेषण, अनुकूलन और सामग्री स्थानापन्न खोजें" else "Track item costs, optimize bundles, and unlock substitutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Estimated cost summary
        val totalCost = groceries.sumOf { if (!it.isPurchased) it.estimatedCost else 0.0 }
        val boughtCount = groceries.count { it.isPurchased }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "अनुमानित लागत (शेष राशन)" else "Estimated Remaining Cost",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.2f".format(totalCost)} USD",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isHindi) "खरीदे गए सामान:" else "Items Purchased",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$boughtCount / ${groceries.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (boughtCount > 0) {
                                IconButton(onClick = { viewModel.clearPurchased() }) {
                                    Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = "Clear purchased", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Instant add item manual
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isHindi) "मैन्युअल राशन जोड़ें" else "Quick Add New Item", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newGroceryName.value,
                            onValueChange = { newGroceryName.value = it },
                            placeholder = { Text(text = if (isHindi) "जैसे: टमाटर, मक्खन..." else "e.g. Tomatoes...") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("grocery_input_name"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newGroceryQty.value,
                            onValueChange = { newGroceryQty.value = it },
                            placeholder = { Text(text = "qty") },
                            modifier = Modifier
                                .width(80.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        IconButton(
                            onClick = {
                                if (newGroceryName.value.isNotBlank()) {
                                    viewModel.addGrocery(
                                        name = newGroceryName.value,
                                        category = newGroceryCat.value,
                                        qty = newGroceryQty.value,
                                        cost = 1.50 // static default manual estimate
                                    )
                                    newGroceryName.value = ""
                                    newGroceryQty.value = "1 unit"
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Section: AI Optimizer and substitutes engine
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "एआई राशन अनुकूलक सक्रिय" else "AI Grocery Cost Optimizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isHindi) "लागत घटाने एवं विकल्प खोजने के लिए अनुकूलक दबाएं।" else "Reduce purchase wastage & discover allergy-safe substitutions.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.simulateFridgeScan("Grocery Optimization substitutes engine request")
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = if (isHindi) "अनुकूलित करें" else "Optimize Now", fontSize = 10.sp)
                    }
                }
            }
        }

        // Groceries Listing
        if (groceries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Filled.ShoppingBasket, contentDescription = "Grocery Empty", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "सामग्री सूची खाली है। मैन्युअल तरीके से जोड़ें या किसी लाइव AI डिश से संघटक निर्यात करें!" else "Your grocery tray is empty. Populate items manually or export directly from an AI recipe step!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(groceries) { item ->
                GroceryListItemCard(item = item, onToggleChecked = {
                    viewModel.toggleGroceryPurchased(item)
                }, onDelete = {
                    viewModel.deleteGrocery(item.id)
                }, isHindi = isHindi)
            }
        }
    }
}

@Composable
fun GroceryListItemCard(item: GroceryEntity, onToggleChecked: () -> Unit, onDelete: () -> Unit, isHindi: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPurchased) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPurchased) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = item.isPurchased,
                    onCheckedChange = { onToggleChecked() },
                    modifier = Modifier.testTag("grocery_checkbox_${item.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.quantity} • ${item.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${"%.2f".format(item.estimatedCost)} USD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (item.isPurchased) Color.Gray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---------------- TABS: 5. USER PROFILE SCREEN ----------------
@Composable
fun UserProfileScreen(viewModel: CookMateViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isHindi = userProfile.language == "Hindi"

    val ageInput = remember { mutableStateOf(userProfile.age.toString()) }
    val weightInput = remember { mutableStateOf(userProfile.weight.toString()) }
    val heightInput = remember { mutableStateOf(userProfile.height.toString()) }
    val selectedFitnessGoal = remember { mutableStateOf(userProfile.fitnessGoal) }
    val selectedDiet = remember { mutableStateOf(userProfile.dietaryPref) }
    val selectedAllergy = remember { mutableStateOf(userProfile.allergies) }
    val selectedCuisinesStr = remember { mutableStateOf(userProfile.preferredCuisines) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = if (isHindi) "यूजर प्रोफाइल" else "User Demographics & Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isHindi) "अपनी पोषण आवश्यकताओं और भाषा विकल्पों को अनुकूलित करें" else "Customize active macro calibration profiles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Language switches
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isHindi) "एप्लिकेशन भाषा" else "Select Application Language", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.setLanguage("English") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isHindi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "English", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.setLanguage("Hindi") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHindi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "हिंदी", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Demographics Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = if (isHindi) "शारीरिक जानकारी" else "Bodily Profile Measurements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ageInput.value,
                            onValueChange = { ageInput.value = it },
                            label = { Text(text = if (isHindi) "उम्र (वर्ष)" else "Age") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = weightInput.value,
                            onValueChange = { weightInput.value = it },
                            label = { Text(text = if (isHindi) "वजन (किलोग्राम)" else "Weight (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = heightInput.value,
                            onValueChange = { heightInput.value = it },
                            label = { Text(text = if (isHindi) "ऊंचाई (सेमी)" else "Height (cm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Goal selector dropdown
                    Text(text = if (isHindi) "स्वास्थ्य एवं पोषण लक्ष्य" else "Select Fitness & Wellness Goal", fontWeight = FontWeight.Bold)
                    val fitnessGoals = listOf("Weight Loss", "Balanced Nutrition", "Muscle Gain", "Athletic Endurance")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fitnessGoals.forEach { goal ->
                            val isGoalSelected = selectedFitnessGoal.value == goal
                            FilterChip(
                                selected = isGoalSelected,
                                onClick = { selectedFitnessGoal.value = goal },
                                label = { Text(text = goal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Diet Preference selector
                    Text(text = if (isHindi) "आहार संबंधित प्राथमिकता (Diet)" else "Diet Preference Filters", fontWeight = FontWeight.Bold)
                    val diets = listOf("None", "Vegetarian", "Vegan", "Keto", "Jain")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        diets.forEach { diet ->
                            val isDietSelected = selectedDiet.value == diet
                            FilterChip(
                                selected = isDietSelected,
                                onClick = { selectedDiet.value = diet },
                                label = { Text(text = diet) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Allergies text field
                    OutlinedTextField(
                        value = selectedAllergy.value,
                        onValueChange = { selectedAllergy.value = it },
                        label = { Text(text = if (isHindi) "एलर्जी (यदि कोई हो, अल्पविराम लगाएं)" else "Allergies to Avoid (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Cuff / Cuisines Preferences
                    OutlinedTextField(
                        value = selectedCuisinesStr.value,
                        onValueChange = { selectedCuisinesStr.value = it },
                        label = { Text(text = if (isHindi) "पसंदीदा व्यंजन शैलियाँ" else "Preferred Cuisine Styles") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                age = ageInput.value.toIntOrNull() ?: 28,
                                gender = userProfile.gender,
                                height = heightInput.value.toDoubleOrNull() ?: 175.0,
                                weight = weightInput.value.toDoubleOrNull() ?: 70.0,
                                fitnessGoal = selectedFitnessGoal.value,
                                dietaryPref = selectedDiet.value,
                                allergies = selectedAllergy.value,
                                cuisines = selectedCuisinesStr.value
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = if (isHindi) "प्रोफाइल सहेजें" else "Save & Sync Demographics")
                    }
                }
            }
        }
    }
}

// ---------------- CORE FEATURE: 6. RECIPE DETAIL & ASSISTANT SCREEN ----------------
@Composable
fun RecipeDetailScreen(
    recipe: RecipeEntity,
    viewModel: CookMateViewModel,
    onClose: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isHindi = userProfile.language == "Hindi"

    val isVoiceActive by viewModel.isVoiceActive.collectAsStateWithLifecycle()
    val activeStepIndex by viewModel.activeStepIndex.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chefQueryStr = remember { mutableStateOf("") }

    val showVoiceControlSheet = remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top header detail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "CookMate Lab",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { viewModel.toggleFavorite(recipe) }) {
                Icon(
                    imageVector = if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Save favorite",
                    tint = if (recipe.isFavorite) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Recipe Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.RestaurantMenu,
                        contentDescription = "Recipe Food",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Recipe Metrics bar card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricUnit(label = if (isHindi) "तैयारी" else "Prep", value = recipe.prepTime)
                    MetricDivider()
                    MetricUnit(label = if (isHindi) "पकने का" else "Cook", value = recipe.cookTime)
                    MetricDivider()
                    MetricUnit(label = if (isHindi) "कठिनाई" else "Level", value = recipe.difficulty)
                    MetricDivider()
                    MetricUnit(label = if (isHindi) "भाग" else "Servings", value = recipe.servingSize)
                }
            }

            // Macros detail
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "पोषण विवरण (माइक्रो/मैक्रो)" else "Estimated Nutritional Yield",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroYieldItem(num = "${recipe.calories}", label = if (isHindi) "कैलोरी" else "kcal")
                        MacroYieldItem(num = recipe.protein, label = if (isHindi) "प्रोटीन" else "Protein")
                        MacroYieldItem(num = recipe.carbohydrates, label = if (isHindi) "कार्ब्स" else "Carbs")
                        MacroYieldItem(num = recipe.fat, label = if (isHindi) "वसा" else "Fat")
                        MacroYieldItem(num = recipe.fiber, label = if (isHindi) "फाइबर" else "Fiber")
                    }
                }
            }

            // Share / Print & Export to grocery shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.populateIngredientsToGrocery(recipe) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.AddShoppingCart, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isHindi) "राशन में जोड़ें" else "Save Ingredients")
                }

                Button(
                    onClick = {
                        // Launch Custom Hands-Free coaching mode
                        viewModel.startRecipeGuidance(recipe)
                        showVoiceControlSheet.value = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.RecordVoiceOver, contentDescription = "Voice mode")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isHindi) "हैंड्स-फ्री कुकिंग" else "AI Voice Guidance")
                }
            }

            // Section: Ingredients checklist box
            Text(
                text = if (isHindi) "आवश्यक सामग्री" else "Ingredients Required Checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Parse ingredients list
            recipe.ingredients.split("\n").filter { it.isNotBlank() }.forEach { rawIngredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Bullet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rawIngredient.removePrefix("-").trim(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Cooking steps step-by-step instructions
            Text(
                text = if (isHindi) "बनाने की विधि (स्टेप-बाय-स्टेप)" else "Step-by-Step Cooking Directs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            recipe.instructions.split("\n").filter { it.isNotBlank() }.forEachIndexed { index, step ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == activeStepIndex && showVoiceControlSheet.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = step.replace(Regex("^\\d+\\.\\s*"), ""), // remove leading step numbers if duplicated
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = if (index == activeStepIndex && showVoiceControlSheet.value) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Sub chef chat interaction inside recipe page
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.HeadsetMic, contentDescription = "Chef", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isHindi) "लाइव एआई शेफ असिस्टेंट" else "Live Chef Substitute & Swap Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(text = if (isHindi) "तैयारी या किसी सामग्री के लाइव रिप्लेसमेंट का सवाल पूछें" else "Ask allergy alternatives, cooking adjustments, or salt fixes.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Minimal scrollable Chat log
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chatMessages.takeLast(4).forEach { msg ->
                                val isChef = msg.sender == "Chef AI"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isChef) Arrangement.Start else Arrangement.End
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChef) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Text(text = msg.text, modifier = Modifier.padding(8.dp), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chefQueryStr.value,
                            onValueChange = { chefQueryStr.value = it },
                            placeholder = { Text(text = if (isHindi) "जैसे: भुने चने के बजाय क्या दाल डालें?" else "e.g. Can I swap quinoa for millet?", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (chefQueryStr.value.isNotBlank()) {
                                    viewModel.sendChefMessage(chefQueryStr.value)
                                    chefQueryStr.value = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isHindi) "पूछें" else "Query")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Active Hands-Free voice display bar overlay
        if (showVoiceControlSheet.value) {
            Surface(
                tonalElevation = 12.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoiceActive) Color.Green else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isVoiceActive) (if (isHindi) "एआई वॉयस कुकिंग मोड (सक्रिय)" else "AI Voice Command Mode (Listening)") else (if (isHindi) "वॉयस मोड रोक दिया गया है" else "Voice Companion Standby"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = { showVoiceControlSheet.value = false }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close sheet")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val stepsTextList = recipe.instructions.split("\n").filter { it.isNotBlank() }
                    val currentStepText = if (activeStepIndex >= 0 && activeStepIndex < stepsTextList.size) {
                        stepsTextList[activeStepIndex].replace(Regex("^\\d+\\.\\s*"), "")
                    } else {
                        "Completed!"
                    }

                    Text(
                        text = "Step ${activeStepIndex + 1} of ${stepsTextList.size}:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentStepText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulator quick actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.executeVoiceCommand("Previous Step") },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isHindi) "पिछला" else "Back")
                        }

                        // Play/Pause voice listener simulator
                        Button(
                            onClick = {
                                viewModel.toggleVoiceMode()
                                // simulate speech instruction prompt triggers
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isVoiceActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isVoiceActive) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Mic Toggle"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isVoiceActive) (if (isHindi) "ध्वनि म्यूट" else "Mute Mic") else (if (isHindi) "बोलें (सिमुलेटर)" else "Trigger Mic"))
                        }

                        OutlinedButton(
                            onClick = { viewModel.executeVoiceCommand("Next Step") },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isHindi) "अगला" else "Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }

                    if (isVoiceActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "💡 सिमुलेशन कमांड बोलें: 'Next Step' (अगला कदम), 'Previous Step' (पिछला कदम)." else "💡 Simulated inputs: Tap Back / Next button, or issue words via click triggers.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricUnit(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.LightGray)
    )
}

@Composable
fun MacroYieldItem(num: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = num, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ForestGreen)
        Text(text = label, fontSize = 10.sp, color = Color.DarkGray)
    }
}

// ---------------- STATIC PRESETS MOCK RECOMMENDATIONS ----------------
fun getPredefinedRecommendations(isHindi: Boolean): List<RecipeEntity> {
    return if (isHindi) {
        listOf(
            RecipeEntity(
                id = "reco_001",
                name = "एवोकैडो चिया पुडिंग",
                category = "Breakfast",
                description = "Nutrient-dense chia seeds soaked in unsweetened almond milk topped with mashed ripe avocado, sliced fresh almonds, and premium berries.",
                ingredients = "- 3 tbsp Chia seeds\n- 1 cup Almond milk (unsweetened)\n- 1/2 ripe Avocado\n- 1 tsp Honey\n- 5 Blueberries",
                instructions = "1. Whisk chia seeds, honey, and almond milk inside a bowl. Keep in the fridge for at least 2 hours.\n2. Layer pureed ripe avocado onto the pudding base.\n3. Add sliced almonds & fresh blueberries on top. Best served cold.",
                calories = 280,
                isFavorite = false
            ),
            RecipeEntity(
                id = "reco_002",
                name = "भूमध्यसागरीय सलाद",
                category = "Lunch",
                description = "A colorful high-protein Mediterranean super bowl filled with organic boiled quinoa, rich cherry tomatoes, crunchy cucumber, tossed in lemon vinaigrette.",
                ingredients = "- 1 cup Cooked Quinoa\n- 1/2 cup Chickpeas\n- 1 Cucumber, diced\n- 5 Olive slices\n- 1 tbsp olive oil\n- 1 tbsp lemon juice",
                instructions = "1. Boil quinoa completely and let cool down.\n2. Toss diced cucumber, boiled chickpeas, olives, and fresh lemon-olive-oil vinaigrette inside a spacious mixing bowl.\n3. Garnish with mint sprigs. Quick, balanced, organic.",
                calories = 420,
                isFavorite = false
            )
        )
    } else {
        listOf(
            RecipeEntity(
                id = "reco_001",
                name = "Avocado Chia Pudding",
                category = "Breakfast",
                description = "Nutrient-dense chia seeds soaked in unsweetened almond milk topped with mashed ripe avocado, sliced fresh almonds, and premium berries.",
                ingredients = "- 3 tbsp Chia seeds\n- 1 cup Almond milk (unsweetened)\n- 1/2 ripe Avocado\n- 1 tsp Honey\n- 5 Blueberries",
                instructions = "1. Whisk chia seeds, honey, and almond milk inside a bowl. Keep in the fridge for at least 2 hours.\n2. Layer pureed ripe avocado onto the pudding base.\n3. Add sliced almonds & fresh blueberries on top. Best served cold.",
                calories = 280,
                isFavorite = false
            ),
            RecipeEntity(
                id = "reco_002",
                name = "Mediterranean Salad Bowl",
                category = "Lunch",
                description = "A colorful high-protein Mediterranean super bowl filled with organic boiled quinoa, rich cherry tomatoes, crunchy cucumber, tossed in lemon vinaigrette.",
                ingredients = "- 1 cup Cooked Quinoa\n- 1/2 cup Chickpeas\n- 1 Cucumber, diced\n- 5 Olive slices\n- 1 tbsp olive oil\n- 1 tbsp lemon juice",
                instructions = "1. Boil quinoa completely and let cool down.\n2. Toss diced cucumber, boiled chickpeas, olives, and fresh lemon-olive-oil vinaigrette inside a spacious mixing bowl.\n3. Garnish with mint sprigs. Quick, balanced, organic.",
                calories = 420,
                isFavorite = false
            )
        )
    }
}
