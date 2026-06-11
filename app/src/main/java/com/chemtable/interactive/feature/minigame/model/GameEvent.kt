package com.chemtable.interactive.feature.minigame.model

/** View -> ViewModel 단일 진입점 이벤트. */
sealed interface GameEvent {
    data object StartGame : GameEvent
    data class Swipe(val direction: Direction) : GameEvent
    data object Pause : GameEvent
    data object Resume : GameEvent
    data object Restart : GameEvent
    data object Exit : GameEvent
    data object SkipTutorial : GameEvent
    data object ShowTutorial : GameEvent

    /** 만든 분자를 계산기로 보내 몰질량을 확인. */
    data class OpenCalculator(val formula: String) : GameEvent

    /** 구성 원소 상세 화면으로 이동. */
    data class OpenElement(val atomicNumber: Int) : GameEvent

    /** 관련 용어 상세 화면으로 이동. */
    data class OpenGlossary(val termId: String) : GameEvent

    /** 인게임에서 분자 블록을 탭하여 Action Sheet를 트리거. */
    data class BlockTapped(val blockId: Long) : GameEvent

    /** 인게임 분자 Action Sheet 닫기. */
    data object CloseMoleculeSheet : GameEvent
}
