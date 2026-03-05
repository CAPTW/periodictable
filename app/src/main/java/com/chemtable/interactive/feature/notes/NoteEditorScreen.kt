package com.chemtable.interactive.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    innerPadding: PaddingValues,
    noteId: Long?,
    elementId: Int?,
    viewModel: NoteEditorViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.noteUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.noteId == null) "메모 작성" else "메모 수정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveNote(onSaved) }) {
                Text("저장")
            }
        }
    ) { paddings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(ChemTableSpacing.screenPadding)
                    .padding(paddings),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            if (state.elementId != null) {
                Text("원자번호: ${state.elementId}", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("전체 메모", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("제목") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("메모 내용") },
                minLines = 6
            )
            OutlinedTextField(
                value = state.tagsText,
                onValueChange = viewModel::onTagsChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("태그: 쉼표 구분") }
            )
            if (state.noteId != null) {
                TextButton(
                    onClick = {
                        // 향후 삭제 기능 추가 예정
                    }
                ) {
                    Text("삭제")
                }
            }
        }
    }
}
