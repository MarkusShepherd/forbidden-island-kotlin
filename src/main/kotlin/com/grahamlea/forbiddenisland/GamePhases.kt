package com.grahamlea.forbiddenisland

sealed class GamePhase {

    fun phaseAfter(event: GameEvent, nextGameState: GameState): GamePhase =
        if (event is OutOfTurnEvent) this
        else calculateNextPhase(event, nextGameState)

    protected abstract fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase

    protected fun invalidEventInPhase(event: GameEvent): Nothing {
        throw IllegalStateException("Not expecting event '$event' during phase '$this'")
    }

    companion object {
        const val maxActionsPerPlayerTurn = 3
        const val treasureDeckCardsDrawnPerTurn = 2
    }
}

data class AwaitingPlayerAction(val player: Adventurer, val actionsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        return when (event) {
            is PlayerActionEvent -> when (actionsRemaining) {
                1 -> AwaitingTreasureDeckDraw(player, treasureDeckCardsDrawnPerTurn)
                else -> AwaitingPlayerAction(player, actionsRemaining - 1) // TODO: GiveTreasureCard could result in needing to discard a card
            }
            is DrawFromTreasureDeck -> AwaitingTreasureDeckDraw(player, treasureDeckCardsDrawnPerTurn - 1)
            else -> invalidEventInPhase(event)
        }
    }
}

data class AwaitingPlayerToDiscardExtraCards(val player: Adventurer, val cardsRemainingToBeDiscarded: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        return this // TODO: Implement properly
    }
}

data class AwaitingTreasureDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        return when (event) {
            is DrawFromTreasureDeck -> when {
                drawsRemaining > 1 -> AwaitingTreasureDeckDraw(player, drawsRemaining - 1)
                else -> AwaitingFloodDeckDraw(player, nextGameState.floodLevel.tilesFloodingPerTurn)
            }
            else -> invalidEventInPhase(event)
        }
    }
}

data class AwaitingFloodDeckDraw(val player: Adventurer, val drawsRemaining: Int): GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        return this // TODO: Implement properly
    }
}

object GameOver: GamePhase() {
    override fun calculateNextPhase(event: GameEvent, nextGameState: GameState): GamePhase {
        return this // TODO: Implement properly
    }
}
