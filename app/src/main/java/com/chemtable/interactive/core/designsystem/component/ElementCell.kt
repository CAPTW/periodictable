package com.chemtable.interactive.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chemtable.interactive.core.designsystem.theme.ChemTableColors
import com.chemtable.interactive.core.designsystem.theme.CustomShapes
import com.chemtable.interactive.core.designsystem.theme.ChemTableTypography
import com.chemtable.interactive.core.designsystem.theme.CustomTypography
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.ElementCategory

private fun ElementCategory.toCategoryColor(): Color = when (this) {
    ElementCategory.ALKALI_METAL -> ChemTableColors.alkaliMetal
    ElementCategory.ALKALINE_EARTH -> ChemTableColors.alkalineEarth
    ElementCategory.TRANSITION_METAL -> ChemTableColors.transitionMetal
    ElementCategory.POST_TRANSITION_METAL -> ChemTableColors.postTransitionMetal
    ElementCategory.METALLOID -> ChemTableColors.metalloid
    ElementCategory.NONMETAL -> ChemTableColors.nonmetal
    ElementCategory.HALOGEN -> ChemTableColors.halogen
    ElementCategory.NOBLE_GAS -> ChemTableColors.nobleGas
    ElementCategory.LANTHANIDE -> ChemTableColors.lanthanide
    ElementCategory.ACTINIDE -> ChemTableColors.actinide
    ElementCategory.UNKNOWN -> ChemTableColors.primary
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElementCell(
    element: Element,
    isCompact: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = element.category.toCategoryColor()
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "elementCellColor")
    val numberStyle = if (isCompact) {
        ChemTableTypography.labelSmall.copy(fontSize = 6.sp, lineHeight = 6.sp)
    } else {
        ChemTableTypography.bodySmall
    }
    val symbolStyle = if (isCompact) {
        CustomTypography.elementSymbol.copy(fontSize = 10.sp, lineHeight = 10.sp)
    } else {
        CustomTypography.elementSymbol
    }
    val nameStyle = if (isCompact) {
        CustomTypography.elementName.copy(fontSize = 5.sp, lineHeight = 5.sp)
    } else {
        CustomTypography.elementName
    }

    Card(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "${element.nameKo}, ${element.symbol}, 원자번호 ${element.atomicNumber}"
            }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CustomShapes.elementCell
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = CustomShapes.elementCell,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = if (isCompact) Arrangement.SpaceEvenly else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = element.atomicNumber.toString(),
                style = numberStyle,
                textAlign = TextAlign.Center
            )
            Text(
                text = element.symbol,
                style = symbolStyle
            )
            Text(
                text = element.nameKo,
                style = nameStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
