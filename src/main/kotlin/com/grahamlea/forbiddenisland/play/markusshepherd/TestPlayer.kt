package com.grahamlea.forbiddenisland.play.markusshepherd

import com.grahamlea.forbiddenisland.Game
import com.grahamlea.forbiddenisland.GameAction
import com.grahamlea.forbiddenisland.play.GamePlayer
import com.grahamlea.forbiddenisland.play.printGamePlayerTestResult
import com.grahamlea.forbiddenisland.play.testGamePlayer
import java.util.Random

class TestPlayer : GamePlayer {
    override fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random) =
        Context(game, deterministicRandomForGamePlayerDecisions)

    inner class Context(private val game: Game, private val random: Random) : GamePlayer.GamePlayContext {
        override fun selectNextAction(): GameAction =
            game.gameState.availableActions.let {
                it[random.nextInt(it.size)]
            }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            printGamePlayerTestResult(testGamePlayer(TestPlayer()))
        }
    }
}