package com.chemtable.interactive.feature.minigame.engine

import com.chemtable.interactive.feature.minigame.model.GameBlock
import com.chemtable.interactive.feature.minigame.model.Recipe

/**
 * 조합 규칙 사전. 두 블록의 composition multiset 을 합쳐 정확히 일치하는 Recipe 를 찾는다.
 *
 * 매칭은 화학식 문자열 파싱(FormulaParser)을 쓰지 않고 원소 multiset(Map) 동등성으로만 판정한다.
 * 정의되지 않은 조합은 null 을 반환해 "임의 결합"으로 가짜 분자가 생기는 것을 막는다.
 */
class RecipeBook(private val recipes: List<Recipe> = DEFAULT_RECIPES) {

    /** 두 입력 블록이 조합 가능한지 검사. 가능하면 결과 Recipe, 아니면 null. */
    fun match(first: GameBlock, second: GameBlock): Recipe? =
        match(combine(first.composition, second.composition))

    /** 합쳐진 원소 multiset 으로 직접 매칭. */
    fun match(combined: Map<String, Int>): Recipe? =
        recipes.firstOrNull { it.inputs == combined }

    /** 생성된 formula 에 대응하는 레시피 composition. 결과 표시/링크용 조회이며 매칭 규칙은 바꾸지 않는다. */
    fun compositionForProduct(formula: String): Map<String, Int>? =
        recipes.firstOrNull { it.productFormula == formula }?.inputs

    private fun combine(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> {
        val out = LinkedHashMap<String, Int>()
        for ((symbol, count) in a) out[symbol] = (out[symbol] ?: 0) + count
        for ((symbol, count) in b) out[symbol] = (out[symbol] ?: 0) + count
        return out.filterValues { it > 0 }
    }

    companion object {
        /**
         * MVP 확정 조합 6종(코드 상수). assets/recipes.json 은 MVP 에서 만들지 않는다.
         * 화학 반응식이 아니라 게임 내 조합 규칙이다.
         */
        val DEFAULT_RECIPES: List<Recipe> = listOf(
            Recipe(mapOf("H" to 2), "H2", "수소 분자"),
            Recipe(mapOf("O" to 2), "O2", "산소 분자"),
            Recipe(mapOf("N" to 2), "N2", "질소 분자"),
            Recipe(mapOf("H" to 2, "O" to 1), "H2O", "물"),
            Recipe(mapOf("C" to 1, "O" to 2), "CO2", "이산화탄소"),
            Recipe(mapOf("Na" to 1, "Cl" to 1), "NaCl", "염화 소듐"),
        )
    }
}
