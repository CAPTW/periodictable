package com.chemtable.interactive.feature.minigame.model

import com.chemtable.interactive.core.model.ElementCategory

/**
 * 스폰 풀에 들어가는 원소 명세. 보드에 새 ElementBlock 을 생성할 때 사용한다.
 * (Phase 1B 에서 GetElementsUseCase 결과로 채운다.)
 */
data class SpawnableElement(
    val atomicNumber: Int,
    val symbol: String,
    val nameKo: String = "",
    val molarMass: Double,
    val category: ElementCategory? = null,
)
