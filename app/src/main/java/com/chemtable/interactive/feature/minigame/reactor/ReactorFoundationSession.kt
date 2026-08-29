package com.chemtable.interactive.feature.minigame.reactor

import com.chemtable.interactive.feature.minigame.reactor.adapter.ClassicRecipeBookReactorAdapter
import com.chemtable.interactive.feature.minigame.reactor.engine.DeterministicReactorEntityIdFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorBoardEngine
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorDirection
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorElementCatalog
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorEventReplayer
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorMassAuthority
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSampleBoardFactory
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorSettlingProfile
import com.chemtable.interactive.feature.minigame.reactor.engine.ReactorTurnEvent
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorBoardState
import com.chemtable.interactive.feature.minigame.reactor.model.ReactorEntityId

data class ReactorFoundationSessionState(
    val board: ReactorBoardState,
    val latestEvents: List<ReactorTurnEvent> = emptyList(),
    val selectedEntityId: ReactorEntityId? = null,
    val lastReplayVerified: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * In-memory-only P2 Reactor session. It owns no repository, Room, DataStore, score, or Dex port.
 * A turn is published only after the independent event replayer reconstructs its exact result.
 */
class ReactorFoundationSession(
    elementCatalog: ReactorElementCatalog,
    massAuthority: ReactorMassAuthority,
    settlingProfile: ReactorSettlingProfile,
) {
    private val sampleFactory = ReactorSampleBoardFactory(elementCatalog, settlingProfile)
    private val engine = ReactorBoardEngine(
        reactionCatalog = ClassicRecipeBookReactorAdapter(),
        massAuthority = massAuthority,
        settlingProfile = settlingProfile,
        idFactory = DeterministicReactorEntityIdFactory(),
    )
    private val eventReplayer = ReactorEventReplayer()

    var state: ReactorFoundationSessionState = ReactorFoundationSessionState(
        board = sampleFactory.create(),
    )
        private set

    fun swipe(direction: ReactorDirection) {
        val initial = state.board
        val result = runCatching { engine.resolveTurn(initial, direction) }
            .getOrElse { error ->
                state = state.copy(
                    lastReplayVerified = false,
                    errorMessage = error.message ?: "반응조 턴을 계산하지 못했습니다.",
                )
                return
            }
        val replay = eventReplayer.validate(initial, result)
        if (!replay.isValid || replay.replayedBoard != result.board) {
            state = state.copy(
                lastReplayVerified = false,
                errorMessage = buildString {
                    append("반응조 이벤트 재생 검증에 실패했습니다.")
                    if (replay.errors.isNotEmpty()) {
                        append(' ')
                        append(replay.errors.joinToString())
                    }
                },
            )
            return
        }
        state = ReactorFoundationSessionState(
            board = result.board,
            latestEvents = result.events,
            selectedEntityId = state.selectedEntityId?.takeIf { result.board.entityStore[it] != null },
            lastReplayVerified = true,
            errorMessage = null,
        )
    }

    fun reset() {
        state = ReactorFoundationSessionState(board = sampleFactory.create())
    }

    fun selectEntity(entityId: ReactorEntityId?) {
        state = state.copy(
            selectedEntityId = entityId?.takeIf { state.board.entityStore[it] != null },
        )
    }
}
