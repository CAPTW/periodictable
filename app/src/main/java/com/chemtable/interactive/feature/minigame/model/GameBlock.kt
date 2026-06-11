package com.chemtable.interactive.feature.minigame.model

import com.chemtable.interactive.core.model.ElementCategory

/**
 * 보드 위에 올라가는 블록. 원소 블록(ElementBlock) 또는 분자 블록(MoleculeBlock).
 *
 * - massScore: 중력 정렬 기준값. 클수록 아래로 가라앉는다. (원자량/분자량 기반)
 * - composition: 구성 원소 multiset. 조합 규칙(Recipe) 매칭의 키로 사용된다.
 *   화학식 문자열 파싱이 아니라 이 multiset 비교로 분자 생성 여부를 판정한다.
 */
sealed interface GameBlock {
    val id: Long
    val massScore: Double
    val composition: Map<String, Int>
    val label: String
}

/** 단일 원소 블록. */
data class ElementBlock(
    override val id: Long,
    val atomicNumber: Int,
    val symbol: String,
    val nameKo: String = "",
    val molarMass: Double,
    val category: ElementCategory? = null,
) : GameBlock {
    override val massScore: Double get() = molarMass
    override val composition: Map<String, Int> get() = mapOf(symbol to 1)
    override val label: String get() = symbol
}

/**
 * 조합 규칙으로 생성된 분자 블록.
 * massScore 는 결과 화학식의 몰질량(생성 시점에 resolver 로 계산해 주입)을 사용한다.
 */
data class MoleculeBlock(
    override val id: Long,
    val formula: String,
    override val massScore: Double,
    override val composition: Map<String, Int>,
    val displayKo: String = "",
) : GameBlock {
    override val label: String get() = formula
}
