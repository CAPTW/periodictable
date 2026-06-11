package com.chemtable.interactive.feature.minigame.model

/**
 * 게임 내 분자 생성 조합 규칙.
 *
 * 화학 반응식이 아니라 "두 블록을 합쳤을 때 어떤 분자가 만들어지는지"를 정의하는 MVP 조합 규칙이다.
 *
 * - inputs: 두 입력 블록의 composition 을 합친 원소 multiset.
 * - productFormula: 생성되는 분자 화학식(예: "H2O").
 */
data class Recipe(
    val inputs: Map<String, Int>,
    val productFormula: String,
    val displayKo: String = "",
)
