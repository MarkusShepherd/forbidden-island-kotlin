package com.grahamlea.forbiddenisland.play.markusshepherd

import java.util.Random

abstract class QLearningAgent<State, Action>(
    val alpha: Double = 0.25,
    var epsilon: Double = 0.2,
    val gamma: Double = 0.99,
    val random: Random = Random()
) {
    val qValues = mutableMapOf<Pair<State, Action>, Double>().withDefault { 0.0 }

    abstract fun getLegalActions(state: State): List<Action>

    fun qValue(state: State, action: Action) = qValues[state to action] ?: 0.0

    fun setQValue(state: State, action: Action, value: Double) {
        qValues[state to action] = value
    }

    open fun value(state: State) = getLegalActions(state).map { qValue(state, it) }.max() ?: 0.0

    open fun update(state: State, action: Action, reward: Double, nextState: State) {
        val value = ((1 - alpha) * qValue(state, action) + alpha * (reward + gamma * value(nextState)))
        setQValue(state, action, value)
    }

    open fun bestAction(state: State) = getLegalActions(state).maxBy { qValue(state, it) }

    open fun action(state: State) = if (random.nextDouble() <= epsilon)
        choice(getLegalActions(state), random)
    else
        bestAction(state)
}

abstract class EVSarsaAgent<State, Action>(
    alpha: Double = 0.25,
    epsilon: Double = 0.2,
    gamma: Double = 0.99,
    random: Random = Random()
) : QLearningAgent<State, Action>(alpha, epsilon, gamma, random) {
    override fun value(state: State) = action(state)?.let { qValue(state, it) } ?: 0.0
}