package chess

import chess.Figure.*
import chess.Figure.DIRECTION_MOVE.*
import chess.Figure.RANK.*
import java.util.*

interface Figure {
    enum class RANK { PAWN, EMPTY }

    enum class COLOR(val printedName: String) { WHITE("white"), BLACK("black") }

    enum class DIRECTION_MOVE { FORWARD, BACK, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT }

    /**
     * Return all directions and distance which figure can move
     */
    fun getAvailableDirections(): List<Pair<DIRECTION_MOVE, Int>>

    /**
     * Return all directions and distance which figure can capture other figure
     */
    fun getAvailableDirCaptures(): List<Pair<DIRECTION_MOVE, Int>>

    fun getRank(): RANK

    fun getColor(): COLOR

    /**
     * Check is this figure empty(stub) or not
     */
    fun isEmpty(): Boolean

    /**
     * Check is this figure can move
     * @param boardModel model of board
     * @param oldPosition position of figure before move
     * @param newPosition position of figure after move
     * @param historyGame object with all moves of all figures
     */
    fun canMove(boardModel: BoardModel, oldPosition: Position, newPosition: Position, historyGame: History): Boolean

    /**
     * Get all available moves
     * @param boardModel model of board
     * @param curPosition position of figure before move
     */
    fun getAvailableMoves(boardModel: BoardModel, curPosition: Position): List<Position>

}

abstract class AbstractFigure(
    private val color: COLOR = COLOR.WHITE,
    private val rank: RANK = PAWN,
) : Figure {

    private val uid = UUID.randomUUID()

    override fun getRank(): RANK {
        return rank
    }

    override fun getColor(): COLOR {
        return color
    }

    override fun isEmpty(): Boolean {
        return rank == EMPTY
    }

    override fun canMove(
        boardModel: BoardModel,
        oldPosition: Position,
        newPosition: Position,
        historyGame: History,
    ): Boolean {
        val isCanMove = getAvailableDirections().map { direction ->
            canMove(boardModel, direction, oldPosition, newPosition)
        }.any { it }
        if (isCanMove) {
            return isCanMove
        }

        return getAvailableDirCaptures().map { direction ->
            canCapture(boardModel, direction, oldPosition, newPosition, historyGame)
        }.any { it }
    }

    override fun getAvailableMoves(boardModel: BoardModel, curPosition: Position): List<Position> {
        val positions = mutableListOf<Position>()
        for (direction in getAvailableDirections()) {
            val newPositions = getAvailableMoves(boardModel, curPosition, direction)
            positions.addAll(newPositions)
        }

        for (direction in getAvailableDirCaptures()) {
            val newPositions = getAvailableCaptures(boardModel, curPosition, direction)
            positions.addAll(newPositions)
        }
        return positions
    }

    abstract fun getAvailableCaptures(
        boardModel: BoardModel,
        curPosition: Position,
        direction: Pair<DIRECTION_MOVE, Int>,
    ): List<Position>

    abstract fun getAvailableMoves(
        boardModel: BoardModel,
        curPosition: Position,
        direction: Pair<DIRECTION_MOVE, Int>,
    ): List<Position>

    abstract fun canMove(
        boardModel: BoardModel, direction: Pair<DIRECTION_MOVE, Int>, oldPosition: Position, newPosition: Position,
    ): Boolean

    abstract fun canCapture(
        boardModel: BoardModel,
        direction: Pair<DIRECTION_MOVE, Int>,
        oldPosition: Position,
        newPosition: Position,
        historyGame: History,
    ): Boolean

    override fun toString(): String {
        return "Figure(color=$color, rank=$rank)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractFigure) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid?.hashCode() ?: 0
    }
}

class PawnFigure(color: COLOR = COLOR.WHITE) : AbstractFigure(color) {

    override fun canMove(
        boardModel: BoardModel,
        direction: Pair<DIRECTION_MOVE, Int>,
        oldPosition: Position,
        newPosition: Position,
    ): Boolean {
        if (direction.first != FORWARD)
            throw UnsupportedOperationException("Pawn cann move only forward. Current direction = ${direction.first}")

        val pawnTrackPosition =
            getPawnMoveTrack(oldPosition, newPosition).filterIndexed { index, _ -> index > 0 }
        if (pawnTrackPosition.isEmpty()) {
            return false
        }

        val pawnTrack = pawnTrackPosition.map { boardModel.getFigure(it) }
        if (pawnTrack.size > direction.second || pawnTrack.any { !it.isEmpty() }) {
            //pawn cannot move more 2 cell or jump not empty cell
            return false
        }

        if (pawnTrack.size == 2 && (oldPosition.row != 2 && oldPosition.row != 7)) {
            //pawn cannot move 2 cell when first move is done
            return false
        }

        return true
    }

    override fun canCapture(
        boardModel: BoardModel,
        direction: Pair<DIRECTION_MOVE, Int>,
        oldPosition: Position,
        newPosition: Position,
        historyGame: History,
    ): Boolean {
        if (direction.first != TOP_RIGHT && direction.first != TOP_LEFT)
            throw UnsupportedOperationException("Pawn cann capture with direction = ${direction.first}")

        val (inc, newRow) = if (getColor() == COLOR.WHITE)
            Pair(direction.second, oldPosition.row + direction.second) else
            Pair(-direction.second, oldPosition.row - direction.second)

        val newColumn = if (direction.first == TOP_RIGHT) oldPosition.column + inc else oldPosition.column - inc
        if (newColumn !in BoardModel.columnRow || newRow !in BoardModel.columnRow) {
            return false
        }
        val position = Position(newRow, newColumn)
        if (position != newPosition) {
            return false
        }
        val newFigure = boardModel.getFigure(position)

        return newFigure != BoardModel.emptyFigure && newFigure.getColor() != getColor() ||
                isCanEnPassant(boardModel, oldPosition, newPosition, historyGame)
    }

    fun isCanEnPassant(
        boardModel: BoardModel,
        oldPosition: Position,
        newPosition: Position,
        historyGame: History,
    ): Boolean {
        val (startPosition, inc) = if (getColor() == COLOR.WHITE) Pair(5, -1) else Pair(4, 1)
        if (oldPosition.row != startPosition) return false
        //5 or 4 row

        val nearFigure = boardModel.getFigure(Position(newPosition.row + inc, newPosition.column))
        if (nearFigure !is PawnFigure || nearFigure.getColor() == getColor()) return false
        //near figure is pawn other color

        val nearFigureMoves = historyGame.getAllMoves(nearFigure)
        val lastMove = nearFigureMoves.last()
        if (nearFigureMoves.size != 1 || historyGame.getMove(lastMove.moveId + 1) != null ||
            kotlin.math.abs(lastMove.oldPosition.row - lastMove.newPosition.row) != 2
        ) return false
        //near pawn made one move with 2 cell, and enemy will first move after that

        return true
    }

    private fun getPawnMoveTrack(oldPosition: Position, newPosition: Position): List<Position> {
        if (oldPosition.column != newPosition.column) {
            //pawn cannot move diagonally
            return emptyList()
        }
        val colRange =
            if (oldPosition.row < newPosition.row)
                oldPosition.row..newPosition.row
            else oldPosition.row downTo newPosition.row

        if (COLOR.WHITE == getColor() && oldPosition.row > newPosition.row ||
            COLOR.BLACK == getColor() && oldPosition.row < newPosition.row
        ) {
            //pawn cannot move back
            return emptyList()
        }
        return colRange.map { Position(it, newPosition.column) }
    }

    override fun getAvailableDirections(): List<Pair<DIRECTION_MOVE, Int>> {
        return listOf(Pair(FORWARD, 2))
    }

    override fun getAvailableMoves(
        boardModel: BoardModel,
        curPosition: Position,
        direction: Pair<DIRECTION_MOVE, Int>,
    ): List<Position> {
        if (FORWARD != direction.first) throw RuntimeException("Pawn can move only forward")
        val inc = if (COLOR.WHITE == getColor()) 1 else -1
        val res = mutableListOf<Position>()
        for (dir in 1..direction.second) {
            res += Position(curPosition.row + dir * inc, curPosition.column)
        }
        return res
    }

    override fun getAvailableCaptures(
        boardModel: BoardModel,
        curPosition: Position,
        direction: Pair<DIRECTION_MOVE, Int>,
    ): List<Position> {
        val inc = if (COLOR.WHITE == getColor()) 1 else -1
        var newRow = curPosition.row
        var newColumn = curPosition.column
        if (TOP_LEFT == direction.first) {
            newRow += direction.second * inc
            newColumn -= direction.second * inc
        } else if (TOP_RIGHT == direction.first) {
            newRow += direction.second * inc
            newColumn += direction.second * inc
        } else {
            throw UnsupportedOperationException("Pawn can capture only $TOP_LEFT or $TOP_RIGHT")
        }
        if (newRow !in BoardModel.columnRow || newColumn !in BoardModel.columnRow) {
            return emptyList()
        }

        return listOf(Position(newRow, newColumn))
    }

    override fun getAvailableDirCaptures(): List<Pair<DIRECTION_MOVE, Int>> {
        return listOf(Pair(TOP_LEFT, 1), Pair(TOP_RIGHT, 1))
    }
}

class EmptyFigure : AbstractFigure(rank = EMPTY) {
    override fun canMove(
        boardModel: BoardModel,
        direction: Pair<DIRECTION_MOVE, Int>,
        oldPosition: Position,
        newPosition: Position,
    ): Boolean {
        throw RuntimeException("Empty figure cannot move")
    }

    override fun canCapture(
        boardModel: BoardModel,
        direction: Pair<DIRECTION_MOVE, Int>,
        oldPosition: Position,
        newPosition: Position,
        historyGame: History,
    ): Boolean {
        throw RuntimeException("Empty figure cannot move")
    }

    override fun getAvailableDirections(): List<Pair<DIRECTION_MOVE, Int>> {
        throw RuntimeException("Empty figure cannot move")
    }

    override fun getAvailableMoves(
        boardModel: BoardModel,
        curPosition: Position,
        direction: Pair<DIRECTION_MOVE, Int>,
    ): List<Position> {
        throw RuntimeException("Empty figure cannot move")
    }

    override fun getAvailableCaptures(
        boardModel: BoardModel,
        curPosition: Position,
        direction: Pair<DIRECTION_MOVE, Int>,
    ): List<Position> {
        throw RuntimeException("Empty figure cannot capture")
    }

    override fun getAvailableDirCaptures(): List<Pair<DIRECTION_MOVE, Int>> {
        throw RuntimeException("Empty figure cannot capture")
    }
}