package com.chemtable.interactive.feature.minigame.engine

/**
 * 분자 화학식 -> 몰질량 변환 포트.
 *
 * 엔진을 순수 Kotlin 으로 유지하기 위한 추상화. 테스트에서는 fake 구현을 주입하고,
 * Phase 1B 에서는 기존 core/util/MolarMassCalculator + 원소 목록을 감싸는 어댑터로 구현한다.
 * (엔진은 MolarMassCalculator 나 FormulaParser 에 직접 의존하지 않는다.)
 */
fun interface FormulaMassResolver {
    fun molarMassOf(formula: String): Double
}
