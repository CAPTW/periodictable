package com.chemtable.interactive.feature.periodictable

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.util.StartupTrace
import com.chemtable.interactive.feature.elementdetail.ElementQuickPreview

@Composable
fun PeriodicTableRoute(
    innerPadding: PaddingValues,
    onElementSelected: (Int) -> Unit
) {
    // Thin instrumentation wrapper around PeriodicTableScreen. The screen renders its own
    // loading pane while element data is still being collected, so no artificial gating
    // delay is needed here (the earlier delay(500) was a profiling-only artifact).
    LaunchedEffect(Unit) {
        StartupTrace.mark("PeriodicTableRoute composed")
    }
    PeriodicTableScreen(
        innerPadding = innerPadding,
        onElementSelected = onElementSelected
    )
}

@Composable
fun PeriodicTableScreen(
    innerPadding: PaddingValues,
    onElementSelected: (Int) -> Unit,
    viewModel: PeriodicTableViewModel = hiltViewModel()
) {
    val elements by viewModel.elements.collectAsState()
    LaunchedEffect(Unit) {
        StartupTrace.mark("PeriodicTableScreen entered")
    }
    LaunchedEffect(elements.size) {
        if (elements.isNotEmpty()) {
            StartupTrace.mark("PeriodicTableScreen elements loaded count=${elements.size}")
        }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var quickPreview by remember { mutableStateOf<Element?>(null) }
    var selectedAtomicNumber by remember { mutableStateOf<Int?>(null) }
    val selectedElement = selectedAtomicNumber?.let { target ->
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
        if (selectedElement != null) {
            Text(
                text = "선택: ${selectedElement.nameKo} (${selectedElement.symbol}) - ${selectedElement.period}주기 ${selectedElement.group}족",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 2.5f)
                    offset += pan
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
        ) {
            if (elements.isEmpty()) {
                PeriodicTableLoadingPane(modifier = Modifier.fillMaxSize())
            } else {
                PeriodicTableGrid(
                    elements = elements,
                    selectedAtomicNumber = selectedAtomicNumber,
                    onElementClick = { atomic ->
                        selectedAtomicNumber = atomic
                        onElementSelected(atomic)
                    },
                    onElementLongClick = { atomic ->
                        selectedAtomicNumber = atomic
                        quickPreview = elements.firstOrNull { it.atomicNumber == atomic }
                    }
                )
            }
            if (quickPreview != null) {
                ElementQuickPreview(element = quickPreview!!)
            }
        }
    }
}

@Composable
private fun PeriodicTableLoadingPane(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "원소 데이터를 준비하는 중",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
