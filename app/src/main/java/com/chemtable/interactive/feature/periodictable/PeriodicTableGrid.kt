package com.chemtable.interactive.feature.periodictable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.core.designsystem.component.ElementCell
import com.chemtable.interactive.core.model.Element

private data class GridSlot(val element: Element?)

@Composable
fun PeriodicTableGrid(
    elements: List<Element>,
    onElementClick: (Int) -> Unit,
    onElementLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val slots = remember(elements) {
        buildList {
            for (period in 1..10) {
                for (group in 1..18) {
                    add(GridSlot(elements.firstOrNull { it.period == period && it.group == group }))
                }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(18),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding = PaddingValues(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(slots) { slot ->
            if (slot.element == null) {
                Box(Modifier.sizeIn(minWidth = 30.dp, minHeight = 30.dp))
            } else {
                ElementCell(
                    element = slot.element,
                    isCompact = true,
                    onClick = { onElementClick(slot.element.atomicNumber) },
                    onLongClick = { onElementLongClick(slot.element.atomicNumber) }
                )
            }
        }
    }
}
