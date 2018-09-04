package com.grahamlea.forbiddenisland.play.markusshepherd

import com.grahamlea.forbiddenisland.AdventurersWon
import com.grahamlea.forbiddenisland.Game
import com.grahamlea.forbiddenisland.GameAction
import com.grahamlea.forbiddenisland.GameState
import com.grahamlea.forbiddenisland.immListOf
import com.grahamlea.forbiddenisland.play.GamePlayer
import com.grahamlea.forbiddenisland.play.printGamePlayerTestResult
import com.grahamlea.forbiddenisland.play.testGamePlayer
import java.util.Random

class ForbiddenQLearner(
    alpha: Double = 0.25,
    epsilon: Double = 0.2,
    gamma: Double = 0.99,
    random: Random = Random()
) : QLearningAgent<GameState, GameAction>(alpha, epsilon, gamma, random) {
    override fun getLegalActions(state: GameState) = state.availableActions
}

class ForbiddenEVSarsa(
    alpha: Double = 0.25,
    epsilon: Double = 0.2,
    gamma: Double = 0.99,
    random: Random = Random()
) : EVSarsaAgent<GameState, GameAction>(alpha, epsilon, gamma, random) {
    override fun getLegalActions(state: GameState) = state.availableActions
}

class QPlayer(val learner: QLearningAgent<GameState, GameAction>) : GamePlayer {
    override fun newContext(game: Game, deterministicRandomForGamePlayerDecisions: Random) =
        Context(game)

    inner class Context(private val game: Game) : GamePlayer.GamePlayContext {
        var prevState: GameState? = null
        var prevAction: GameAction? = null

        fun update(state: GameState, reward: Double) {
            if (prevState == null || prevAction == null)
                return

            learner.update(prevState!!, prevAction!!, reward, state)
        }

        override fun selectNextAction(): GameAction {
            if (game.gameState.availableActions.size <= 1)
                return game.gameState.availableActions.first()

            val state = game.gameState.copy(previousActions = immListOf())

            update(state, 1.0)

            val action = learner.action(state)
                ?.takeIf { it in game.gameState.availableActions }
                ?: choice(game.gameState.availableActions, learner.random)
                ?: throw IllegalStateException()

            prevState = state
            prevAction = action

            return action
        }

        override fun finished() {
            val state = game.gameState.copy(previousActions = immListOf())
            assert(state.result != null)
            val reward = if (state.result == AdventurersWon) +1000.0 else -1000.0
            update(state, reward)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val learner = ForbiddenEVSarsa(epsilon = .1)
            printGamePlayerTestResult(testGamePlayer(QPlayer(learner), gamesPerCategory = 2000))
            println(learner.qValues.size)
        }
    }
}