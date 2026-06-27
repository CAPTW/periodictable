package com.chemtable.interactive.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing
import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("설정", style = MaterialTheme.typography.titleLarge)

        // --- Theme ---
        Spacer(Modifier.height(20.dp))
        Text("테마", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val themeOptions = listOf(
            ThemeMode.LIGHT to "라이트",
            ThemeMode.DARK to "다크",
            ThemeMode.SOLARIZED to "솔라라이즈드",
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            themeOptions.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                    modifier = Modifier.semantics { contentDescription = "$label 테마" },
                ) {
                    Text(label)
                }
            }
        }

        // --- Font size ---
        Spacer(Modifier.height(24.dp))
        Text("글자 크기", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // Local slider position for smooth dragging; persist on release to avoid write spam.
        var sliderValue by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("작게", style = MaterialTheme.typography.labelMedium)
            Text("${(sliderValue * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            Text("크게", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { viewModel.setFontScale(sliderValue) },
            valueRange = AppSettings.MIN_FONT_SCALE..AppSettings.MAX_FONT_SCALE,
            steps = 8,
            modifier = Modifier.semantics { contentDescription = "글자 크기 조절 슬라이더" },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "미리보기 — 원소 주기율표 · H₂O 18.015 g/mol",
            style = MaterialTheme.typography.bodyLarge,
        )

        // --- Other settings (planned) ---
        Spacer(Modifier.height(28.dp))
        Text(
            "원소 색상 모드 · 표시 속성 기본값 · 오프라인 데이터 동기화 · 오픈소스 라이선스는 준비 중입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
