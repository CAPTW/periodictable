package com.chemtable.interactive.feature.minigame.model

/**
 * ViewModel -> View 일회성 효과.
 * (계산기/사전/원소상세 딥링크 효과는 Phase 2 에서 추가한다.)
 */
sealed interface GameEffect {
    /** 분자 생성 성공 — 토스트/플래시/진동 등 피드백용. label 은 이번 move 에서 생성된 분자 표기. */
    data class MergeSuccess(val label: String, val gained: Int) : GameEffect

    /** 유효하지 않은 이동(아무 일도 일어나지 않음) — 흔들림 피드백용. */
    data object MergeRejected : GameEffect

    /** 게임 종료/나가기 — 화면이 받아서 popBackStack 등을 수행. */
    data object NavigateBack : GameEffect

    /** 만든 분자를 계산기에서 보기 — 화면이 받아서 계산기 라우트로 formula 를 프리필 전달. */
    data class NavigateToCalculator(val formula: String) : GameEffect

    /** 만든 분자의 구성 원소 상세 보기 — 화면이 받아서 ElementDetail 라우트로 이동. */
    data class NavigateToElement(val atomicNumber: Int) : GameEffect

    /** 만든 분자와 관련된 용어 상세 보기 — 화면이 받아서 GlossaryDetail 라우트로 이동. */
    data class NavigateToGlossary(val termId: String) : GameEffect
}
