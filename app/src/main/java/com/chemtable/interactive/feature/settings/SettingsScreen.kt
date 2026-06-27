package com.chemtable.interactive.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing

@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    // These settings are not yet implemented. Until they are wired to a real (e.g. DataStore)
    // backend they are shown as "준비 중" (coming soon) so the UI never implies working behavior.
    val comingSoon = "준비 중"
    val rows = listOf(
        "테마",
        "원소 색상 모드",
        "표시 속성 기본값",
        "오프라인 데이터 동기화",
        "오픈소스 라이선스",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding)
    ) {
        Text("설정", style = MaterialTheme.typography.titleLarge)
        Text(
            "아래 설정은 준비 중입니다. 현재 버전에서는 동작하지 않으며 추후 업데이트에서 제공될 예정입니다.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(rows) { row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .semantics { contentDescription = "$row, $comingSoon" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = row, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = comingSoon,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
