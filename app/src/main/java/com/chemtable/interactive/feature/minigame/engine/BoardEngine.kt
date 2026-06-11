package com.chemtable.interactive.feature.minigame.engine

import com.chemtable.interactive.feature.minigame.model.BoardState
import com.chemtable.interactive.feature.minigame.model.Direction
import com.chemtable.interactive.feature.minigame.model.ElementBlock
import com.chemtable.interactive.feature.minigame.model.GameBlock
import com.chemtable.interactive.feature.minigame.model.MoleculeBlock
import com.chemtable.interactive.feature.minigame.model.MoveResult
import com.chemtable.interactive.feature.minigame.model.Position
import com.chemtable.interactive.feature.minigame.model.SpawnableElement
import kotlin.math.round
import kotlin.random.Random

/**
 * 순수 Kotlin 보드 엔진. Android/Compose/Hilt 의존성이 없다.
 *
 * move 파이프라인: 압착+조합(swipe 방향) -> 중력 정렬(열 단위) -> 유효 move 면 스폰 -> game over 판정.
 * Random 과 IdGenerator 를 주입받아 테스트가 결정적으로 동작한다.
 */
class BoardEngine(
    private val resolver: FormulaMassResolver,
    private val recipeBook: RecipeBook = RecipeBook(),
    private val spawnPool: List<SpawnableElement> = emptyList(),
    private val random: Random = Random.Default,
    private val idGenerator: IdGenerator = IdGenerator(),
) {

    /** 압착+조합 단계 결과(중력/스폰 전). */
    data class SlideOutcome(
        val board: BoardState,
        val moved: Boolean,
        val mergedBlocks: List<MoleculeBlock>,
    )

    /** 전체 move 처리. */
    fun move(board: BoardState, direction: Direction): MoveResult {
        val slide = compressAndMerge(board, direction)
        if (!slide.moved) {
            return MoveResult(
                board = board,
                moved = false,
                mergedFormulas = emptyList(),
                gainedScore = 0,
                spawned = false,
                spawnedPosition = null,
                isGameOver = isGameOver(board),
            )
        }
        val settled = applyGravity(slide.board)
        val (spawnedBoard, spawnPos) = spawn(settled)
        val gained = slide.mergedBlocks.sumOf { round(it.massScore).toInt() }
        return MoveResult(
            board = spawnedBoard,
            moved = true,
            mergedFormulas = slide.mergedBlocks.map { it.formula },
            gainedScore = gained,
            spawned = spawnPos != null,
            spawnedPosition = spawnPos,
            isGameOver = isGameOver(spawnedBoard),
        )
    }

    /**
     * swipe 방향으로 각 라인을 벽 쪽으로 압착하고, 인접한 조합 가능 블록을 병합한다.
     * 한 move 에서 같은 블록은 1회만 병합된다(소비된 블록/생성된 분자는 재병합되지 않음).
     * 중력/스폰은 적용하지 않는다.
     */
    fun compressAndMerge(board: BoardState, direction: Direction): SlideOutcome {
        val size = board.size
        val newGrid = MutableList(size) { MutableList<GameBlock?>(size) { null } }
        val merged = mutableListOf<MoleculeBlock>()

        when (direction) {
            Direction.LEFT, Direction.RIGHT -> {
                for (r in 0 until size) {
                    val line = (0 until size).map { c -> board.grid[r][c] }
                    val oriented = if (direction == Direction.LEFT) line else line.reversed()
                    val slid = slideLine(oriented, merged)
                    val restored = if (direction == Direction.LEFT) slid else slid.reversed()
                    for (c in 0 until size) newGrid[r][c] = restored[c]
                }
            }
            Direction.UP, Direction.DOWN -> {
                for (c in 0 until size) {
                    val line = (0 until size).map { r -> board.grid[r][c] }
                    val oriented = if (direction == Direction.UP) line else line.reversed()
                    val slid = slideLine(oriented, merged)
                    val restored = if (direction == Direction.UP) slid else slid.reversed()
                    for (r in 0 until size) newGrid[r][c] = restored[r]
                }
            }
        }

        val result = BoardState(size, newGrid.map { it.toList() })
        val moved = result.grid != board.grid
        return SlideOutcome(result, moved, merged)
    }

    /** 라인의 index 0 을 벽으로 보고 압착+조합. */
    private fun slideLine(line: List<GameBlock?>, mergedSink: MutableList<MoleculeBlock>): List<GameBlock?> {
        val blocks = line.filterNotNull()
        val out = ArrayList<GameBlock>(line.size)
        var i = 0
        while (i < blocks.size) {
            val current = blocks[i]
            val next = blocks.getOrNull(i + 1)
            val recipe = if (next != null) recipeBook.match(current, next) else null
            if (recipe != null) {
                val molecule = MoleculeBlock(
                    id = idGenerator.next(),
                    formula = recipe.productFormula,
                    massScore = resolver.molarMassOf(recipe.productFormula),
                    composition = recipe.inputs,
                    displayKo = recipe.displayKo,
                )
                out.add(molecule)
                mergedSink.add(molecule)
                i += 2
            } else {
                out.add(current)
                i += 1
            }
        }
        val padded = ArrayList<GameBlock?>(line.size)
        padded.addAll(out)
        while (padded.size < line.size) padded.add(null)
        return padded
    }

    /**
     * 열 단위 중력 정렬: 각 열에서 블록을 바닥(큰 row)으로 모으고 massScore 가 클수록 더 아래에 둔다.
     * (무거운 블록이 가라앉는다 — 원자량/분자량 기준.)
     */
    fun applyGravity(board: BoardState): BoardState {
        val size = board.size
        val newGrid = MutableList(size) { MutableList<GameBlock?>(size) { null } }
        for (c in 0 until size) {
            val column = (0 until size).mapNotNull { r -> board.grid[r][c] }
            val heavyFirst = column.sortedByDescending { it.massScore }
            for ((index, block) in heavyFirst.withIndex()) {
                val row = size - 1 - index
                newGrid[row][c] = block
            }
        }
        return BoardState(size, newGrid.map { it.toList() })
    }

    /** 빈 칸 하나에 스폰 풀에서 무작위 원소를 1개 생성. 빈 칸/풀이 없으면 그대로 반환. */
    fun spawn(board: BoardState): Pair<BoardState, Position?> {
        val empties = board.emptyPositions()
        if (empties.isEmpty() || spawnPool.isEmpty()) return board to null
        val pos = empties[random.nextInt(empties.size)]
        val spec = spawnPool[random.nextInt(spawnPool.size)]
        val block = ElementBlock(
            id = idGenerator.next(),
            atomicNumber = spec.atomicNumber,
            symbol = spec.symbol,
            nameKo = spec.nameKo,
            molarMass = spec.molarMass,
            category = spec.category,
        )
        val newGrid = board.grid.map { it.toMutableList() }
        newGrid[pos.row][pos.col] = block
        return BoardState(board.size, newGrid.map { it.toList() }) to pos
    }

    /** 보드가 가득 차고 어떤 방향으로도 압착/조합이 불가능하면 game over. */
    fun isGameOver(board: BoardState): Boolean {
        if (!board.isFull()) return false
        return Direction.values().none { compressAndMerge(board, it).moved }
    }

    /** 빈 보드에서 count 개 원소를 스폰하고 중력 정렬한 초기 보드 생성. startElementSpec 이 제공되면 1개 보장. */
    fun seedBoard(count: Int, startElementSpec: SpawnableElement? = null, size: Int = 4): BoardState {
        var board = BoardState.empty(size)
        val numRandom = if (startElementSpec != null) count - 1 else count

        if (startElementSpec != null) {
            val empties = board.emptyPositions()
            if (empties.isNotEmpty()) {
                val pos = empties[random.nextInt(empties.size)]
                val block = ElementBlock(
                    id = idGenerator.next(),
                    atomicNumber = startElementSpec.atomicNumber,
                    symbol = startElementSpec.symbol,
                    nameKo = startElementSpec.nameKo,
                    molarMass = startElementSpec.molarMass,
                    category = startElementSpec.category,
                )
                val newGrid = board.grid.map { it.toMutableList() }
                newGrid[pos.row][pos.col] = block
                board = BoardState(size, newGrid.map { it.toList() })
            }
        }

        repeat(numRandom) {
            val (next, _) = spawn(board)
            board = next
        }
        return applyGravity(board)
    }
}
