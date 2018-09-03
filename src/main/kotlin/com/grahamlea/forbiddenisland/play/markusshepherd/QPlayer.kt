package com.grahamlea.forbiddenisland.play.markusshepherd

import com.github.chen0040.rl.learning.qlearn.QLearner
import com.grahamlea.forbiddenisland.AdventurersWon
import com.grahamlea.forbiddenisland.Game
import com.grahamlea.forbiddenisland.GameAction
import com.grahamlea.forbiddenisland.play.GamePlayer
import com.grahamlea.forbiddenisland.play.printGamePlayerTestResult
import com.grahamlea.forbiddenisland.play.testGamePlayer
import java.util.Random

class QPlayer(val learner: QLearner) : GamePlayer {
    override fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random) =
        Context(game, deterministicRandomForGamePlayerDecisions)

    inner class Context(private val game: Game, private val random: Random) : GamePlayer.GamePlayContext {
        var prevState: Int? = null
        var prevAction: Int? = null
        var prevAvailableActions: Set<Int>? = null

        fun update(state: Int, reward: Double) {
            if (prevState == null || prevAction == null || prevAvailableActions == null)
                return

            learner.update(prevState!!, prevAction!!, state, prevAvailableActions, reward)
        }

        override fun selectNextAction(): GameAction {
            val state = game.gameState.hashCode()

            update(state, 0.0)

            val availableActions = game.gameState.availableActions.map { actionIds[it]!! }.toSet()
            val actionValue = learner.selectAction(state, availableActions)
            val action = actionValue?.index ?: availableActions.toList()[random.nextInt(availableActions.size)]

            prevState = state
            prevAction = action
            prevAvailableActions = availableActions

            return GameAction.ALL_POSSIBLE_ACTIONS[action]
        }

        override fun finished() {
            assert(game.gameState.result != null)
            println(game.gameState.result)
            val reward = if (game.gameState.result == AdventurersWon) +1000.0 else -1000.0
            update(game.gameState.hashCode(), reward)
        }
    }

    companion object {
        private val actionIds = GameAction.ALL_POSSIBLE_ACTIONS
            .mapIndexed { index, gameAction -> gameAction to index }
            .toMap()

        @JvmStatic
        fun main(args: Array<String>) {
            println(GameAction.ALL_POSSIBLE_ACTIONS.size)
            printGamePlayerTestResult(testGamePlayer(QPlayer(QLearner())))
        }
    }
}