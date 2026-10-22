package com.sanjay.tracewise.ui.theme

import androidx.compose.material3.Typography

// Preserve Material type sizes and font scaling. Keep live durations stable with tabular digits.
private val defaults = Typography()
val Typography = Typography(
    displayMedium = defaults.displayMedium.copy(fontFeatureSettings = "tnum"),
    displaySmall = defaults.displaySmall.copy(fontFeatureSettings = "tnum"),
    headlineSmall = defaults.headlineSmall.copy(fontFeatureSettings = "tnum")
)
