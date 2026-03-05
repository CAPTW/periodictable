package com.chemtable.interactive.feature.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chemtable.interactive.core.model.ElementNote
import com.chemtable.interactive.core.designsystem.theme.ChemTableSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    innerPadding: PaddingValues,
    elementId: Int,
    viewModel: NotesViewModel = hiltViewModel(),
    onOpenEditor: (noteId: Long, elementId: Int?) -> Unit,
    onOpenNote: (noteId: Long, elementId: Int) -> Unit
) {
    val targetElementId = if (elementId > 0) elementId else null
    val notes by viewModel.notesUiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(targetElementId) {
        viewModel.setElementFilter(targetElementId)
    }
    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = if (targetElementId == null) "전체 메모" else "원소 #$targetElementId 메모"
                    Text(title)
                },
                actions = {
                    if (targetElementId != null) {
                        Text(
                            "총 ${notes.size}건",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .width(72.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenEditor(0L, targetElementId) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "메모 추가")
            }
        }
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(ChemTableSpacing.screenPadding)
                .padding(paddings),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("메모 검색") },
                modifier = Modifier.fillMaxWidth()
            )

            if (notes.isEmpty()) {
                Text(
                    text = "저장된 메모가 없습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteListItem(
                            note = note,
                            onClick = { onOpenNote(note.id, note.elementAtomicNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    note: ElementNote,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = { Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.tags.isNotEmpty()) {
                    Text(
                        text = note.tags.joinToString(prefix = "#", separator = " #"),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        trailingContent = { Text("id: ${note.id}") }
    )
}
