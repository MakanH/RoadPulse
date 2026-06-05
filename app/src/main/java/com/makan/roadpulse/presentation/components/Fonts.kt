package com.makan.roadpulse.presentation.components

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.makan.roadpulse.R

/**
 * Roboto Condensed Bold (700) — used for every numeric value across the Board and
 * Profile tabs (podium scores, leaderboard values, rank numbers, pedestal numbers,
 * friend km, group km). The condensed glyphs read narrower than regular bold Roboto,
 * which keeps large numbers compact and competitive-looking.
 */
val RobotoCondensedBold = FontFamily(
    Font(R.font.roboto_condensed_bold, FontWeight.Bold)
)
