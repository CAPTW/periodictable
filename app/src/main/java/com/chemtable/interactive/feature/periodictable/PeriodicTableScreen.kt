package com.chemtable.interactive.feature.periodictable

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.feature.elementdetail.ElementQuickPreview

@Composable
fun PeriodicTableScreen(
    innerPadding: PaddingValues,
    onElementSelected: (Int) -> Unit,
    viewModel: PeriodicTableViewModel = hiltViewModel()
) {
    val elements by viewModel.elements.collectAsState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var quickPreview by remember { mutableStateOf<Element?>(null) }

    Box(
        Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(ChemTableSpacing.screenPadding)
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
        PeriodicTableGrid(
            elements = elements,
            onElementClick = onElementSelected,
            onElementLongClick = { atomic ->
                quickPreview = elements.firstOrNull { it.atomicNumber == atomic }
            }
        )
        if (quickPreview != null) {
            ElementQuickPreview(element = quickPreview!!)
        }
    }
}
