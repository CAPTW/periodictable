package com.chemtable.interactive.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ChemTableShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
)

object CustomShapes {
    val elementCell = RoundedCornerShape(6.dp)
    val card = RoundedCornerShape(12.dp)
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val chip = RoundedCornerShape(8.dp)
}
