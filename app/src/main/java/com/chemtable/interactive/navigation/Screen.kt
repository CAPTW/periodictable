package com.chemtable.interactive.navigation

sealed class Screen(val route: String) {
    object PeriodicTable : Screen("periodic_table")
    object Search : Screen("search")
    object Visualization : Screen("visualization")
    object Calculator : Screen("calculator")
    object Glossary : Screen("glossary")
    object ElementDetail : Screen("element/{atomicNumber}") {
        fun createRoute(atomicNumber: Int) = "element/$atomicNumber"
    }
    object GlossaryDetail : Screen("glossary/{termId}") {
        fun createRoute(termId: String) = "glossary/$termId"
    }
    object Notes : Screen("notes?elementId={elementId}") {
        fun createRoute(elementId: Int? = null): String =
            if (elementId == null || elementId <= 0) "notes" else "notes?elementId=$elementId"
    }
    object NoteEditor : Screen("note_editor?noteId={noteId}&elementId={elementId}") {
        fun createRoute(noteId: Long? = null, elementId: Int? = null): String {
            val safeElementId = elementId?.takeIf { it > 0 }
            return when {
                noteId == null && elementId == null -> "note_editor"
                noteId != null && noteId > 0 && safeElementId == null -> "note_editor?noteId=$noteId"
                noteId != null && noteId > 0 && safeElementId != null -> "note_editor?noteId=$noteId&elementId=$safeElementId"
                noteId == null && safeElementId != null -> "note_editor?elementId=$safeElementId"
                noteId != null && noteId <= 0 && safeElementId != null -> "note_editor?elementId=$safeElementId"
                else -> "note_editor"
            }
        }
    }

    object Settings : Screen("settings")
}
