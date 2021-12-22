package chess

class PawnsOnlyChessGame {
    private val board = ConsoleBoard({
        val figures = mutableMapOf<Position, Figure>()
        for (column in BoardModel.columnRow) {
            var pos = Position(2, column)
            figures[pos] = PawnFigure()
            pos = Position(7, column)
            figures[pos] = PawnFigure(Figure.COLOR.BLACK)
        }
        figures
    })

    fun play() {
        println("Pawns-Only Chess")
        println("First Player's name:")
        val player1 = HumanPlayer(readLine()!!.trim())

        println("Second Player's name:")
        val player2 = HumanPlayer(readLine()!!.trim(), Figure.COLOR.BLACK)
        var curPlayer = player2
        do {
            board.show()
            if (checkWin() || checkOnePlayerLost()) {
                println("${curPlayer.getColor().printedName.firstUpper()} Wins!")
                break
            }

            curPlayer = if (curPlayer == player1) player2 else player1
            if (checkStalemate(curPlayer.getColor())) {
                println("Stalemate!")
                break
            }

            val (oldPosition, newPosition) = move(curPlayer) ?: break
            board.move(oldPosition, newPosition)
        } while (true)

        println("Bye!")
    }

    private fun String.firstUpper(): String {
        if (isBlank()) return ""
        if (length == 1) return uppercase()

        return substring(0, 1).uppercase() + substring(1)
    }

    private fun checkWin(): Boolean {
        val rows = mutableListOf<Pair<Position, Figure>>()
        rows.addAll(board.getRow(BoardModel.columnRow.first))
        rows.addAll(board.getRow(BoardModel.columnRow.last))
        return rows.any { it.second != BoardModel.emptyFigure }
    }

    private fun checkOnePlayerLost(): Boolean {
        return board.getAllFigures().groupBy { it.getColor() }.size == 1
    }

    private fun checkStalemate(color: Figure.COLOR): Boolean {
        for (figure in board.getAllFigures().filter { color == it.getColor() }) {
            val curPosition = board.getPosition(figure)
            val positions = figure.getAvailableMoves(board, curPosition)
            if (positions.isEmpty()) return false
            for (position in positions) {
                if(board.canMove(figure, curPosition, position)) {
                    return false
                }
            }
        }
        return true
    }

    private fun move(player: Player): Pair<Position, Position>? {
        do {
            val positions = player.move() ?: break
            val error = checkValidFigureNewPosition(positions, player)
            if ("" == error) {
                return positions
            } else {
                println(error)
            }
        } while (true)
        return null
    }

    private fun checkValidFigureNewPosition(positions: Pair<Position, Position>, player: Player): String {
        val figure = board.getFigure(positions.first)
        if (figure == BoardModel.emptyFigure || figure.getColor() != player.getColor()) {
            return "No ${player.getColor().printedName} pawn at ${positions.first}"
        }
        if (!board.canMove(figure, positions.first, positions.second)) {
            return "Invalid Input"
        }
        return ""
    }
}