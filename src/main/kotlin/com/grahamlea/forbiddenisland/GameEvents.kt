package com.grahamlea.forbiddenisland

sealed class GameEvent

sealed class PlayerActionEvent: GameEvent()

data class Move(val player: Adventurer, val mapSite: MapSite): PlayerActionEvent() {
    override fun toString() = "$player moves to $mapSite"
}

data class ShoreUp(val player: Adventurer, val mapSite: MapSite, val mapSite2: MapSite? = null): PlayerActionEvent() {
    override fun toString() =
        "$mapSite ${if (mapSite2 == null) "is" else "and $mapSite2 are "} shored up by $player"
}

data class GiveTreasureCard(val player: Adventurer, val receiver: Adventurer, val cards: ImmutableList<TreasureCard>): PlayerActionEvent() {
    override fun toString() = "$player gives $cards to $receiver"
}

data class CaptureTreasure(val player:Adventurer, val treasure: Treasure): PlayerActionEvent() {
    override fun toString() = "$treasure is captured by $player"
}

sealed class PlayerSpecialActionEvent: GameEvent()

data class HelicopterLift(val playerWithCard: Adventurer, val playerBeingMoved: Adventurer, val mapSite: MapSite): PlayerSpecialActionEvent() {
    override fun toString() = "$playerBeingMoved is helicopter lifted to $mapSite by $playerWithCard"
}

data class Sandbag(val player: Adventurer, val mapSite: MapSite): PlayerSpecialActionEvent() {
    override fun toString() = "$mapSite is sand bagged by $player"
}

data class HelicopterLiftOffIsland(val player: Adventurer): PlayerSpecialActionEvent() {
    override fun toString() = "All players are lifted off the island by $player"
}

sealed class PlayerObligationEvent: GameEvent()

data class DrawFromTreasureDeck(val player: Adventurer): PlayerObligationEvent() {
    override fun toString() = "$player draws from the Treasure Deck"
}

data class DrawFromFloodDeck(val player: Adventurer): PlayerObligationEvent() {
    override fun toString() = "$player draws from the Flood Deck"
}

data class Swim(val strandedPlayer: Adventurer, val mapSite: MapSite): PlayerObligationEvent()
