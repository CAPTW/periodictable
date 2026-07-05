package com.chemtable.interactive.feature.periodictable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.TableViewMode
import com.chemtable.interactive.feature.elementdetail.ElementQuickPreview
import kotlin.math.min

/** Cell size (per side) used in pan/zoom mode before the pinch scale is applied. */
private val ZOOM_BASE_CELL = 48.dp
private const val ZOOM_MIN_SCALE = 0.5f
private const val ZOOM_MAX_SCALE = 3f

/** Saves an [Offset] to the instance-state bundle as an [x, y] pair. */
private val OffsetSaver = listSaver<Offset, Float>(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) }
)

@Composable
fun PeriodicTableScreen(
    innerPadding: PaddingValues,
    onElementSelected: (Int) -> Unit,
    viewModel: PeriodicTableViewModel = hiltViewModel()
) {
    val elements by viewModel.elements.collectAsState()
    val viewMode by viewModel.tableViewMode.collectAsState()

    // Transient UI state is hoisted here (the always-composed screen root) and made saveable so it
    // survives the FIT<->ZOOM toggle, tab/back navigation, rotation and process death.
    var selectedAtomicNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var quickPreviewAtomicNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var zoomScale by rememberSaveable { mutableStateOf(1f) }
    var zoomOffset by rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }

    val selectedElement = selectedAtomicNumber?.let { target ->
        elements.firstOrNull { it.atomicNumber == target }
    }
    val quickPreview = quickPreviewAtomicNumber?.let { target ->
        elements.firstOrNull { it.atomicNumber == target }
    }

    Column(
        Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(ChemTableSpacing.screenPadding)
    ) {
        Text(
            text = "가로: 족(Group) · 세로: 주기(Period)",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(8.dp))
        TableViewModeToggle(
            selected = viewMode,
            onSelect = viewModel::setTableViewMode
        )
        Text(
            text = when (viewMode) {
                TableViewMode.FIT -> "표 전체가 한 화면에 표시됩니다."
                TableViewMode.ZOOM -> "손가락으로 끌어 이동하고, 두 손가락으로 확대·축소하세요."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (selectedElement != null) {
            Text(
                text = "선택: ${selectedElement.nameKo} (${selectedElement.symbol}) - ${selectedElement.period}주기 ${selectedElement.group}족",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val onElementClick: (Int) -> Unit = { atomic ->
            quickPreviewAtomicNumber = null
            selectedAtomicNumber = atomic
            onElementSelected(atomic)
        }
        val onElementLongClick: (Int) -> Unit = { atomic ->
            selectedAtomicNumber = atomic
            quickPreviewAtomicNumber = atomic
        }

        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxSize()
        ) {
            when (viewMode) {
                TableViewMode.FIT -> FitPeriodicTable(
                    elements = elements,
                    selectedAtomicNumber = selectedAtomicNumber,
                    onElementClick = onElementClick,
                    onElementLongClick = onElementLongClick
                )

                TableViewMode.ZOOM -> ZoomablePeriodicTable(
                    elements = elements,
                    selectedAtomicNumber = selectedAtomicNumber,
                    scale = zoomScale,
                    offset = zoomOffset,
                    onTransformChange = { newScale, newOffset ->
                        zoomScale = newScale
                        zoomOffset = newOffset
                    },
                    onElementClick = onElementClick,
                    onElementLongClick = onElementLongClick
                )
            }

            // Quick preview floats above the (possibly transformed) table and dismisses on tap.
            quickPreview?.let { preview ->
                ElementQuickPreview(
                    element = preview,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable { quickPreviewAtomicNumber = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableViewModeToggle(
    selected: TableViewMode,
    onSelect: (TableViewMode) -> Unit
) {
    val options = listOf(
        TableViewMode.FIT to "전체 보기",
        TableViewMode.ZOOM to "확대 보기"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.semantics { contentDescription = "$label 모드" }
            ) {
                Text(label)
            }
        }
    }
}

/** Scales the entire table down so all 18×10 cells fit within the available space at once. */
@Composable
private fun FitPeriodicTable(
    elements: List<Element>,
    selectedAtomicNumber: Int?,
    onElementClick: (Int) -> Unit,
    onElementLongClick: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val gapPx = with(density) { ChemTableSpacing.elementCellGap.toPx() }
        val cellPx = min(
            (constraints.maxWidth - (PERIODIC_TABLE_GROUPS - 1) * gapPx) / PERIODIC_TABLE_GROUPS,
            (constraints.maxHeight - (PERIODIC_TABLE_PERIODS - 1) * gapPx) / PERIODIC_TABLE_PERIODS
        ).coerceAtLeast(0f)
        val cellDp = with(density) { cellPx.toDp() }

        PeriodicTableGrid(
            elements = elements,
            selectedAtomicNumber = selectedAtomicNumber,
            cellSize = cellDp,
            onElementClick = onElementClick,
            onElementLongClick = onElementLongClick
        )
    }
}

/**
 * Draws the table at a readable fixed cell size inside a bounded pan + pinch-zoom canvas.
 * [scale]/[offset] are owned by the caller (hoisted + saveable); this composable reports changes
 * back through [onTransformChange] so the position survives recomposition and mode toggles.
 */
@Composable
private fun ZoomablePeriodicTable(
    elements: List<Element>,
    selectedAtomicNumber: Int?,
    scale: Float,
    offset: Offset,
    onTransformChange: (Float, Offset) -> Unit,
    onElementClick: (Int) -> Unit,
    onElementLongClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val gapPx = with(density) { ChemTableSpacing.elementCellGap.toPx() }
        val cellPx = with(density) { ZOOM_BASE_CELL.toPx() }
        val contentW = PERIODIC_TABLE_GROUPS * cellPx + (PERIODIC_TABLE_GROUPS - 1) * gapPx
        val contentH = PERIODIC_TABLE_PERIODS * cellPx + (PERIODIC_TABLE_PERIODS - 1) * gapPx
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()

        // The gesture lambda outlives individual recompositions (its pointerInput is keyed only on
        // the viewport), so it reads scale/offset/callback through updated-state refs to stay live.
        val currentScale by rememberUpdatedState(scale)
        val currentOffset by rememberUpdatedState(offset)
        val currentOnTransformChange by rememberUpdatedState(onTransformChange)

        // Keeps the table anchored to its edges: pan is allowed only while content overflows the
        // viewport, otherwise the axis is centered. The grid uses a top-left transform origin so
        // this math maps directly to translation.
        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val scaledW = contentW * atScale
            val scaledH = contentH * atScale
            val x = if (scaledW <= viewportW) (viewportW - scaledW) / 2f
            else candidate.x.coerceIn(viewportW - scaledW, 0f)
            val y = if (scaledH <= viewportH) (viewportH - scaledH) / 2f
            else candidate.y.coerceIn(viewportH - scaledH, 0f)
            return Offset(x, y)
        }

        // Re-clamp when the viewport size becomes known or changes (e.g. rotation) so a restored
        // offset stays within the new bounds.
        LaunchedEffect(viewportW, viewportH) {
            currentOnTransformChange(currentScale, clampOffset(currentOffset, currentScale))
        }

        // Gesture lives on the untransformed viewport box, so centroid/pan are in viewport
        // coordinates; the grid is measured unbounded so its full width is drawn and pannable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(viewportW, viewportH) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = currentScale
                        val newScale = (oldScale * zoom).coerceIn(ZOOM_MIN_SCALE, ZOOM_MAX_SCALE)
                        // Keep the point under the pinch centroid stationary while zooming.
                        val focusAdjusted =
                            centroid - (centroid - currentOffset) * (newScale / oldScale)
                        currentOnTransformChange(newScale, clampOffset(focusAdjusted + pan, newScale))
                    }
                }
        ) {
            PeriodicTableGrid(
                elements = elements,
                selectedAtomicNumber = selectedAtomicNumber,
                cellSize = ZOOM_BASE_CELL,
                onElementClick = onElementClick,
                onElementLongClick = onElementLongClick,
                modifier = Modifier
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            )
        }
    }
}
