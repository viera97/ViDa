package com.vida.core.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A selectable category icon.
 *
 * @property name String identifier matching Material icon resource names (e.g. "restaurant").
 * @property icon The [ImageVector] from [Icons.Filled] that renders the icon.
 */
data class CategoryIcon(
    val name: String,
    val icon: ImageVector,
)

/**
 * Curated list of ~36 Material icons representative of financial categories.
 * Each entry maps a human-readable string name to its [ImageVector] so the
 * name can be persisted (e.g. in a database or network model) independently
 * of the Compose dependency.
 */
val CATEGORY_ICONS: List<CategoryIcon> = listOf(
    // ── Comida / Bebida ─────────────────────────────────────────────────────
    CategoryIcon("restaurant", Icons.Filled.Restaurant),
    CategoryIcon("local_dining", Icons.Filled.LocalDining),
    CategoryIcon("coffee", Icons.Filled.Coffee),
    CategoryIcon("local_cafe", Icons.Filled.LocalCafe),
    CategoryIcon("local_bar", Icons.Filled.LocalBar),
    CategoryIcon("fastfood", Icons.Filled.Fastfood),
    CategoryIcon("bakery", Icons.Filled.BakeryDining),
    CategoryIcon("icecream", Icons.Filled.Icecream),

    // ── Compras ──────────────────────────────────────────────────────────────
    CategoryIcon("shopping_cart", Icons.Filled.ShoppingCart),
    CategoryIcon("shopping_bag", Icons.Filled.ShoppingBag),
    CategoryIcon("store", Icons.Filled.Store),
    CategoryIcon("clothing", Icons.Filled.Checkroom),
    CategoryIcon("card_giftcard", Icons.Filled.CardGiftcard),

    // ── Transporte ───────────────────────────────────────────────────────────
    CategoryIcon("directions_car", Icons.Filled.DirectionsCar),
    CategoryIcon("directions_bus", Icons.Filled.DirectionsBus),
    CategoryIcon("train", Icons.Filled.Train),
    CategoryIcon("flight", Icons.Filled.Flight),
    CategoryIcon("two_wheeler", Icons.Filled.TwoWheeler),

    // ── Hogar / Utilities ───────────────────────────────────────────────────
    CategoryIcon("home", Icons.Filled.Home),
    CategoryIcon("electrical_services", Icons.Filled.ElectricalServices),
    CategoryIcon("water_drop", Icons.Filled.WaterDrop),
    CategoryIcon("local_fire_department", Icons.Filled.LocalFireDepartment),
    CategoryIcon("yard", Icons.Filled.Yard),

    // ── Salud ────────────────────────────────────────────────────────────────
    CategoryIcon("local_hospital", Icons.Filled.LocalHospital),
    CategoryIcon("health_and_safety", Icons.Filled.HealthAndSafety),
    CategoryIcon("fitness_center", Icons.Filled.FitnessCenter),
    CategoryIcon("medication", Icons.Filled.Medication),
    CategoryIcon("pets", Icons.Filled.Pets),

    // ── Entretenimiento ─────────────────────────────────────────────────────
    CategoryIcon("movie", Icons.Filled.Movie),
    CategoryIcon("sports_esports", Icons.Filled.SportsEsports),
    CategoryIcon("weekend", Icons.Filled.Weekend),
    CategoryIcon("music_note", Icons.Filled.MusicNote),
    CategoryIcon("theater_comedy", Icons.Filled.TheaterComedy),
    CategoryIcon("celebration", Icons.Filled.Celebration),

    // ── Finanzas ─────────────────────────────────────────────────────────────
    CategoryIcon("account_balance", Icons.Filled.AccountBalance),
    CategoryIcon("account_balance_wallet", Icons.Filled.AccountBalanceWallet),
    CategoryIcon("payments", Icons.Filled.Payments),
    CategoryIcon("savings", Icons.Filled.Savings),
    CategoryIcon("credit_card", Icons.Filled.CreditCard),

    // ── Servicios ────────────────────────────────────────────────────────────
    CategoryIcon("school", Icons.Filled.School),
    CategoryIcon("work", Icons.Filled.Work),
    CategoryIcon("subscriptions", Icons.Filled.Subscriptions),
    CategoryIcon("local_phone", Icons.Filled.LocalPhone),
    CategoryIcon("wifi", Icons.Filled.Wifi),
    CategoryIcon("construction", Icons.Filled.Construction),

    // ── General ──────────────────────────────────────────────────────────────
    CategoryIcon("category", Icons.Filled.Category),
    CategoryIcon("auto_awesome", Icons.Filled.AutoAwesome),
    CategoryIcon("favorite", Icons.Filled.Favorite),
    CategoryIcon("star", Icons.Filled.Star),
    CategoryIcon("lightbulb", Icons.Filled.Lightbulb),
    CategoryIcon("more_horiz", Icons.Filled.MoreHoriz),
)

/** Internal lookup map built from [CATEGORY_ICONS] for O(1) access. */
private val iconMap: Map<String, ImageVector> =
    CATEGORY_ICONS.associate { it.name to it.icon }

/**
 * Resolves a category icon [name] (e.g. "restaurant") to its Material [ImageVector].
 *
 * Falls back to [Icons.Filled.Category] when the name is unknown or null.
 *
 * Usage:
 * ```kotlin
 * Icon(
 *     imageVector = iconNameToImageVector(category.icon),
 *     contentDescription = category.name,
 * )
 * ```
 */
fun iconNameToImageVector(name: String?): ImageVector =
    if (name != null) iconMap[name] ?: Icons.Filled.Category
    else Icons.Filled.Category
