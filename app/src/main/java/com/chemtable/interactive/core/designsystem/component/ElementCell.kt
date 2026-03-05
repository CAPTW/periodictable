package com.chemtable.interactive.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = element.category.toCategoryColor()
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "elementCellColor")

    Card(
        modifier = modifier.combinedClickable(
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = element.atomicNumber.toString(),
                style = if (isCompact) ChemTableTypography.labelSmall else ChemTableTypography.bodySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = element.symbol,
                style = CustomTypography.elementSymbol
            )
            if (!isCompact) {
                Text(
                    text = element.nameKo,
                    style = CustomTypography.elementName,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
