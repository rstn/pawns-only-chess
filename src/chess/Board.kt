package chess

import chess.BoardModel.CONST.columnRow
import chess.BoardModel.CONST.emptyFigure
import chess.Figure.*
import chess.Figure.RANK.*
import java.lang.IllegalArgumentException
import kotlin.collections.HashMap

interface BoardView {

    fun show()

    fun move(oldPosition: Position, newPosition: Position)

    fun canMove(figure: Figure, oldPosition: Position, newPosition: Position): Boolean

}

interface BoardModel {

    companion object CONST {
        val columnRow = 1..8
        val columnHeader = 'a'..'h'
        val emptyFigure = EmptyFigure()
    }

    fun isEmpty(position: Position): Boolean

    fun getFigure(position: Position): Figure
    fun setFigure(figure: Figure, position: Position)

    fun getPosition(figure: Figure): Position

    fun getAllFigures(): Set<Figure>

    fun getRow(row: Int): List<Pair<Position, Figure>>
}

class ConsoleBoard(
    private val initializer: () -> Map<Position, Figure>,
    private val boardModel: BoardModel = BoardModelImpl(initializer),
) :
    BoardView, BoardModel by boardModel {

    private val history = HistoryGame()

    override fun show() {
        val res = StringBuilder()
        for (row in columnRow.last downTo 1) {
            res.addRow(row)
        }
        print(res.toString())
    }

    override fun move(oldPosition: Position, newPosition: Position) {
        val figure = boardModel.getFigure(oldPosition)
        boardModel.setFigure(figure, newPosition)
        boardModel.setFigure(emptyFigure, oldPosition)

        if (figure is PawnFigure && figure.isCanEnPassant(this, oldPosition, newPosition, history)) {
            //Pawn en passant
            val row = if (COLOR.WHITE == figure.getColor()) newPosition.row - 1 else newPosition.row + 1
            boardModel.setFigure(emptyFigure, Position(row, newPosition.column))
        }

        history.add(figure, oldPosition, newPosition)
    }


    override fun canMove(figure: Figure, oldPosition: Position, newPosition: Position): Boolean {
        return figure.canMove(this, oldPosition, newPosition, history)
    }

    private fun StringBuilder.addRow(row: Int) {
        addTopBottomRowBorder()
        append("$row ")
        getRow(row).forEach {
            addCellValue(it.first, it.second)
        }
        if (row == columnRow.first) {
            addTopBottomRowBorder()
            addBottomHeader()
        }
    }

    private fun StringBuilder.addBottomHeader() {
        append("    ")
        for (header in BoardModel.columnHeader) {
            append("$header   ")
        }
        trimEnd()
        append("\n")
    }

    private fun StringBuilder.addTopBottomRowBorder() {
        this.append("  ")
        repeat(columnRow.last) {
            this.append("+---")
        }
        this.append("+\n")
    }

    private fun StringBuilder.addCellValue(position: Position, figure: Figure) {
        append("| ${figure.convertToString()} ")
        if (position.column == columnRow.last) append("|\n")
    }

    private fun Figure.convertToString(): String {
        if (isEmpty()) return " "

        if (getRank() == PAWN) {
            return if (getColor() == COLOR.BLACK) "B" else "W"
        }
        throw UnsupportedOperationException("Implements for others figures")
    }

}

class BoardModelImpl(initializer: () -> Map<Position, Figure>) : BoardModel {
    private val figures = HashMap<Position, Figure>(initializer())

    init {
        for (row in columnRow) {
            for (column in columnRow) {
                val position = Position(row, column)
                if (!figures.containsKey(position)) {
                    figures[position] = emptyFigure
                }
            }
        }
    }

    override fun isEmpty(position: Position): Boolean {
        return !figures.containsKey(position)
    }

    override fun getFigure(position: Position): Figure {
        if (isEmpty(position)) throw IllegalArgumentException("$position is not contained any figure")
        return figures[position]!!
    }

    override fun setFigure(figure: Figure, position: Position) {
        figures[position] = figure
    }

    override fun getPosition(figure: Figure): Position {
        return figures.firstNotNullOf { entry ->
            var res: Position? = null
            if (entry.value == figure) {
                res = entry.key
            }
            res
        }
    }

    override fun getAllFigures(): Set<Figure> {
        return figures.values.filter { it != emptyFigure }.toSet()
    }

    override fun getRow(row: Int): List<Pair<Position, Figure>> {
        val rowFigures = mutableListOf<Pair<Position, Figure>>()
        for (column in columnRow) {
            val position = Position(row, column)
            rowFigures += Pair(position, figures[position]!!)
        }
        return rowFigures
    }
}

data class Position(val row: Int, val column: Int) {

    companion object Convertor {
        private val checkRegex = "[a-h][1-8]".toRegex()
        private const val beforeA = 'a' - 1
        fun isPosition(strPosition: String): Boolean {
            return checkRegex.matches(strPosition)
        }

        fun isTwoPositions(strPosition: String): Boolean {
            return isPosition(strPosition.substring(0, 2)) && isPosition(strPosition.substring(2))
        }

        fun toPosition(strPosition: String): Position {
            if (!isPosition(strPosition)) throw IllegalArgumentException("Position string is incorrect. Position has format ${checkRegex.pattern}")
            return Position(strPosition[1].digitToInt(), strPosition[0] - beforeA)
        }

        fun toTwoPosition(strPosition: String): Pair<Position, Position> {
            return Pair(toPosition(strPosition.substring(0, 2)), toPosition(strPosition.substring(2)))
        }
    }

    override fun toString(): String {
        return "${beforeA + column}$row"
    }
}


interface History {

    class HistoryItem(val moveId: Int, val figure: Figure, val oldPosition: Position, val newPosition: Position)

    fun add(figure: Figure, oldPosition: Position, newPosition: Position)

    fun getAllMoves(figure: Figure): List<HistoryItem>

    fun getMove(moveId: Int): HistoryItem?
}

class HistoryGame : History {
    private val moves = mutableListOf<History.HistoryItem>()

    override fun add(figure: Figure, oldPosition: Position, newPosition: Position) {
        val moveId = if (moves.isEmpty()) 1 else moves.size + 1
        moves += History.HistoryItem(moveId, figure, oldPosition, newPosition)
    }

    override fun getAllMoves(figure: Figure): List<History.HistoryItem> {
        return moves.filter { it.figure == figure }
    }

    override fun getMove(moveId: Int): History.HistoryItem? {
        return moves.find { it.moveId == moveId }
    }
}

