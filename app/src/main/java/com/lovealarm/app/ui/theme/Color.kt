package com.lovealarm.app.ui.theme

import androidx.compose.ui.graphics.Color

// Soft pastel palette. Light theme only.
val Cream = Color(0xFFFFF8F0)
val Ink = Color(0xFF3A2E39)
val InkSoft = Color(0xFF6E5F6D)

val Blush = Color(0xFFFFD6E0)
val Lavender = Color(0xFFE3D5FF)
val Mint = Color(0xFFD4F5E1)
val Peach = Color(0xFFFFE0C7)
val Sky = Color(0xFFD6ECFF)

val BlushDeep = Color(0xFFE8788F)
val LavenderDeep = Color(0xFF9D86D9)
val MintDeep = Color(0xFF5DBB8A)

/** Tile backgrounds cycle through these in order. */
val TileColors = listOf(Blush, Lavender, Mint, Peach, Sky)

fun tileColor(index: Int): Color = TileColors[((index % TileColors.size) + TileColors.size) % TileColors.size]
