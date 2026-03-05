package com.chemtable.interactive.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ChemTableTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    bodySmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 8.sp,
        fontWeight = FontWeight.Normal
    )
)

object CustomTypography {
    val elementSymbol = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
    val elementNumber = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
    val elementName = TextStyle(
        fontSize = 8.sp,
        fontWeight = FontWeight.Normal
    )
    val propertyValue = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp
    )
}
