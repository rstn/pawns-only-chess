package chess

interface Player {
    /**
     * Return name of Player.
     */
    fun getName(): String

    /**
     * Return color of player's figures.
     */
    fun getColor(): Figure.COLOR

    /**
     * Execute move.
     * Return position's figure which want to move and new position else
     * null if player want to exit game
     */
    fun move(): Pair<Position, Position>?
}

class HumanPlayer(
    private val name: String,
    private val color: Figure.COLOR = Figure.COLOR.WHITE,
) : Player {
    override fun getName(): String {
        return name
    }

    override fun getColor(): Figure.COLOR {
        return color
    }

    override fun move(): Pair<Position, Position>? {
        var userChoice: String
        do {
            println("$name's turn:")
            userChoice = readLine()!!.trim().lowercase()
            if ("exit" == userChoice) return null
            if (Position.isTwoPositions(userChoice)) {
                return Position.toTwoPosition(userChoice)
            } else {
                println("Invalid Input")
            }
        } while (true)

    }

}

