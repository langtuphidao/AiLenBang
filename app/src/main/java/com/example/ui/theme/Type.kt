package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val BalooDisplayFont = FontFamily(
    Font(R.font.baloo_2, FontWeight.Normal),
    Font(R.font.baloo_2, FontWeight.Bold)
)

val BeVietnamBodyFont = FontFamily(
    Font(R.font.be_vietnam_pro, FontWeight.Normal),
    Font(R.font.be_vietnam_pro, FontWeight.Medium),
    Font(R.font.be_vietnam_pro, FontWeight.Bold)
)

fun buildAppTypography(scaleMultiplier: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = BalooDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (40 * scaleMultiplier).sp,
            lineHeight = (46 * scaleMultiplier).sp
        ),
        displayMedium = TextStyle(
            fontFamily = BalooDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (32 * scaleMultiplier).sp,
            lineHeight = (38 * scaleMultiplier).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = BalooDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (26 * scaleMultiplier).sp,
            lineHeight = (32 * scaleMultiplier).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = BalooDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scaleMultiplier).sp,
            lineHeight = (28 * scaleMultiplier).sp
        ),
        titleLarge = TextStyle(
            fontFamily = BalooDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = (19 * scaleMultiplier).sp,
            lineHeight = (25 * scaleMultiplier).sp
        ),
        titleMedium = TextStyle(
            fontFamily = BeVietnamBodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = (16 * scaleMultiplier).sp,
            lineHeight = (22 * scaleMultiplier).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = BeVietnamBodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * scaleMultiplier).sp,
            lineHeight = (22 * scaleMultiplier).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = BeVietnamBodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scaleMultiplier).sp,
            lineHeight = (20 * scaleMultiplier).sp
        ),
        labelLarge = TextStyle(
            fontFamily = BeVietnamBodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = (14 * scaleMultiplier).sp,
            lineHeight = (20 * scaleMultiplier).sp
        )
    )
}
