package com.chemtable.interactive.feature.periodictable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.chemtable.interactive.core.designsystem.component.ElementCell
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.Element

/** Number of columns (groups) and rows (periods, incl. the detached f-block rows) in the layout. */
internal const val PERIODIC_TABLE_GROUPS = 18
internal const val PERIODIC_TABLE_PERIODS = 10

/**
 * Renders the periodic table as a fixed-size grid: every cell is exactly [cellSize] square.
 *
 * The grid is intentionally non-lazy (only ~180 slots) so its measured size is deterministic —
 * that lets callers either scale it to fit the screen or place it in a pan/zoom canvas, and avoids
 * the virtualization glitches a `LazyVerticalGrid` shows when wrapped in a `graphicsLayer` transform.
 */
@Composable
fun PeriodicTableGrid(
    elements: List<Element>,
    selectedAtomicNumber: Int?,
    cellSize: Dp,
    onElementClick: (Int) -> Unit,
    onElementLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = remember(elements) {
        (1..PERIODIC_TABLE_PERIODS).map { period ->
            (1..PERIODIC_TABLE_GROUPS).map { group ->
                elements.firstOrNull { it.period == period && it.group == group }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ChemTableSpacing.elementCellGap)
    ) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(ChemTableSpacing.elementCellGap)) {
                for (element in row) {
                    if (element == null) {
                        Spacer(Modifier.size(cellSize))
                    } else {
                        ElementCell(
                            element = element,
                            isCompact = true,
                            isSelected = selectedAtomicNumber == element.atomicNumber,
                            onClick = { onElementClick(element.atomicNumber) },
                            onLongClick = { onElementLongClick(element.atomicNumber) },
                            modifier = Modifier.size(cellSize)
                        )
                    }
                }
            }
        }
    }
}
