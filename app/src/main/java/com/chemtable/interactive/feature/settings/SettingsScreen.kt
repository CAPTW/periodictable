package com.chemtable.interactive.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing

@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    val rows = listOf(
        "테마",
        "원소 색상 모드",
        "표시 속성 기본값",
        "오프라인 데이터 동기화",
        "오픈소스 라이선스"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(ChemTableSpacing.screenPadding)
    ) {
        Text("설정", style = MaterialTheme.typography.titleLarge)
        Text(
            "공통 설정 화면입니다. 추후 DataStore 기반 설정 토글을 연결하세요.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(rows) { row ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)) {
                    Text(
                        text = row,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

