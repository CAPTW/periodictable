package com.chemtable.interactive.feature.minigame.reactor.items

import java.util.Collections
import kotlin.math.abs

/** Abstract game substrates, deliberately not real biochemical families. */
internal typealias ExperimentSubstrate = com.chemtable.interactive.feature.minigame.reactor.model.ReactorSubstrate

internal data class PolymerBundle(val substrate: ExperimentSubstrate, val units: Int) {
    init { require(units in 1..4) }
}

internal class ItemExperimentState(cells: List<PolymerBundle?>, val remainingActions: Int = 6) {
    val cells: List<PolymerBundle?> = Collections.unmodifiableList(cells.toList())
    init {
        require(cells.size == 25)
        require(remainingActions in 0..6)
    }

    companion object {
        fun sample(): ItemExperimentState {
            val cells = MutableList<PolymerBundle?>(25) { null }
            cells[0] = PolymerBundle(ExperimentSubstrate.A, 1)
            cells[2] = PolymerBundle(ExperimentSubstrate.A, 1)
            cells[20] = PolymerBundle(ExperimentSubstrate.B, 1)
            cells[22] = PolymerBundle(ExperimentSubstrate.B, 1)
            cells[24] = PolymerBundle(ExperimentSubstrate.SYNTHETIC, 2)
            return ItemExperimentState(cells)
        }
    }
}

internal sealed interface ItemExperimentAction {
    data class Link(val anchor: Int, val partner: Int) : ItemExperimentAction
    data class Cleave(val target: Int, val destination: Int, val enzyme: ExperimentSubstrate) : ItemExperimentAction
}

internal enum class ItemEffectStatus(val message: String) {
    LINKED("연결 완료: 첫 칸에 묶음, 두 번째 칸은 비었습니다. 실험 자원 1 사용."),
    CLEAVED("부분 분해 완료: 두 번째 칸에 재사용 조각 1개를 놓았습니다. 실험 자원 1 사용."),
    EXHAUSTED("실험 자원이 없습니다. 무료로 실험을 초기화할 수 있습니다."),
    INVALID_TARGET("서로 다른 유효한 두 칸을 선택하세요."),
    OUT_OF_RANGE("같은 행 또는 열에서 두 칸 거리 이내만 연결·분해할 수 있습니다."),
    INCOMPATIBLE("같은 기질의 두 조각을 선택하세요."),
    TOO_LARGE("한 묶음은 최대 4조각까지 연결할 수 있습니다."),
    WRONG_ENZYME("효소와 기질이 맞지 않습니다. 합성 기질에는 효소를 사용할 수 없습니다."),
    NOT_A_BUNDLE("부분 분해에는 2조각 이상의 묶음이 필요합니다."),
    NO_SPACE("분해 조각을 놓을 두 번째 칸이 비어 있어야 합니다."),
}

internal data class ItemEffectResult(val state: ItemExperimentState, val status: ItemEffectStatus) {
    val applied: Boolean get() = status == ItemEffectStatus.LINKED || status == ItemEffectStatus.CLEAVED
}

/** Pure, atomic resolution: rejected operations return the exact original state. */
internal object ReactorItemExperiment {
    fun resolve(state: ItemExperimentState, action: ItemExperimentAction): ItemEffectResult {
        fun reject(status: ItemEffectStatus) = ItemEffectResult(state, status)
        val first: Int
        val second: Int
        when (action) {
            is ItemExperimentAction.Link -> { first = action.anchor; second = action.partner }
            is ItemExperimentAction.Cleave -> { first = action.target; second = action.destination }
        }
        if (first !in 0..24 || second !in 0..24 || first == second) return reject(ItemEffectStatus.INVALID_TARGET)
        val dr = abs(first / 5 - second / 5)
        val dc = abs(first % 5 - second % 5)
        if ((dr != 0 && dc != 0) || dr + dc > 2) return reject(ItemEffectStatus.OUT_OF_RANGE)
        if (state.remainingActions == 0) return reject(ItemEffectStatus.EXHAUSTED)
        val target = state.cells[first] ?: return reject(ItemEffectStatus.INVALID_TARGET)
        val cells = state.cells.toMutableList()
        val status = when (action) {
            is ItemExperimentAction.Link -> {
                val partner = cells[second] ?: return reject(ItemEffectStatus.INVALID_TARGET)
                if (target.substrate != partner.substrate) return reject(ItemEffectStatus.INCOMPATIBLE)
                if (target.units + partner.units > 4) return reject(ItemEffectStatus.TOO_LARGE)
                cells[first] = target.copy(units = target.units + partner.units)
                cells[second] = null
                ItemEffectStatus.LINKED
            }
            is ItemExperimentAction.Cleave -> {
                if (target.substrate == ExperimentSubstrate.SYNTHETIC || target.substrate != action.enzyme) {
                    return reject(ItemEffectStatus.WRONG_ENZYME)
                }
                if (target.units < 2) return reject(ItemEffectStatus.NOT_A_BUNDLE)
                if (cells[second] != null) return reject(ItemEffectStatus.NO_SPACE)
                cells[first] = target.copy(units = target.units - 1)
                cells[second] = target.copy(units = 1)
                ItemEffectStatus.CLEAVED
            }
        }
        return ItemEffectResult(ItemExperimentState(cells, state.remainingActions - 1), status)
    }
}
