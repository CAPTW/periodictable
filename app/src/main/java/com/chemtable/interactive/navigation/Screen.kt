package com.chemtable.interactive.navigation

import android.net.Uri
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object PeriodicTable : Screen("periodic_table")
    object Search : Screen("search")
    object Visualization : Screen("visualization")

    /**
     * 몰질량 계산기. formula 는 optional query 인자.
     * - 하단 탭/빈 진입: createRoute() == "calculator" (기존과 동일하게 빈 입력).
     * - 미니게임 등에서 식 프리필: createRoute("H2O") == "calculator?formula=H2O".
     *
     * 향후 괄호/점/전하 등 특수문자를 고려해 안전하게 percent-encoding 한다.
     * (android.net.Uri 대신 순수 JVM URLEncoder/Decoder 를 써서 단위 테스트에서도 검증 가능하게 한다.)
     */
    object Calculator : Screen("calculator?formula={formula}") {
        const val ARG_FORMULA = "formula"

        fun createRoute(formula: String? = null): String {
            val trimmed = formula?.trim()
            return if (trimmed.isNullOrEmpty()) {
                "calculator"
            } else {
                "calculator?formula=${encodeArg(trimmed)}"
            }
        }

        /**
         * java.net.URLEncoder.encode는 application/x-www-form-urlencoded 표준에 따라 공백을 '+'로,
         * 실제 '+'는 '%2B'로 인코딩합니다. 하지만 화학식에서는 '+' 자체가 중요한 의미(이온 전하 Na+, 복합 반응식 등)를
         * 가지므로, 공백이 '+'로 바뀌어 화학식과 혼동되는 것을 막기 위해 '+'를 다시 '%20'으로 치환하여 보관합니다.
         */
        private fun encodeArg(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")

        /**
         * percent-encoding된 formula 인자를 디코딩합니다.
         *
         * java.net.URLDecoder.decode는 '+'를 공백(space)으로 치환합니다.
         * 화학식 내의 '+' 문자(이온 전하 등)가 공백으로 변질되는 것을 방지하기 위해,
         * 디코딩을 수행하기 전 문자열 내의 모든 '+'를 '%2B'로 명시적 치환하여 '+'를 보존합니다.
         */
        fun decodeArg(value: String): String {
            val prepared = value.replace("+", "%2B")
            return URLDecoder.decode(prepared, "UTF-8")
        }
    }

    object Glossary : Screen("glossary")
    object ElementDetail : Screen("element/{atomicNumber}") {
        fun createRoute(atomicNumber: Int) = "element/$atomicNumber"
    }
    object GlossaryDetail : Screen("glossary/{termId}") {
        fun createRoute(termId: String) = "glossary/${Uri.encode(termId)}"
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

    object MoleculeDex : Screen("lab/molecule-dex")

    object MoleculeGame : Screen("game/molecule?startElement={atomicNumber}") {
        fun createRoute(atomicNumber: Int? = null): String {
            val safeId = atomicNumber?.takeIf { it > 0 }
            return if (safeId == null) "game/molecule" else "game/molecule?startElement=$safeId"
        }
    }
}
