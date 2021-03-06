package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.Adventurer.*
import com.grahamlea.forbiddenisland.Game.Companion.newRandomGameFor
import com.grahamlea.forbiddenisland.Location.*
import com.grahamlea.forbiddenisland.LocationFloodState.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

@Suppress("UsePropertyAccessSyntax")
@DisplayName("GameState available actions")
class GameStateAvailableActionsTest {

    private val random = Random()
    private val earth = TreasureCard(Treasure.EarthStone)
    private val ocean = TreasureCard(Treasure.OceansChalice)
    private val fire = TreasureCard(Treasure.CrystalOfFire)

    @Nested
    @DisplayName("Move actions")
    inner class MoveTests {

        @Nested
        inner class General {
            @Test
            fun `Engineer, Messenger, Diver and Pilot can walk to any adjacent positions`() {
                val game = game(Engineer, Messenger, Diver, Pilot).withLocationFloodStates(Unflooded, *Location.values())

                for (player in game.gameSetup.players) {
                    val availableMoves =
                        game.withPlayerPosition(player, Position(4, 4))
                            .withGamePhase(AwaitingPlayerAction(player, 3))
                            .availableMoves()

                    assertThat(availableMoves).containsOnlyElementsOf(positionsFromMap("""
                          ..
                         ....
                        ...o..
                        ..o.o.
                         ..o.
                          ..
                        """).map { Move(player, it) }
                    )
                }
            }

            @RunForEachAdventurer
            fun `Any player can move to flooded locations, but no player can move to sunken`(player1: Adventurer, player2: Adventurer) {
                val playerPosition = Position(4, 4)
                val floodedPosition = Position(3, 4)
                val sunkenPosition = Position(5, 4)
                val game = game(player1, player2)
                    .withPlayerPosition(player1, playerPosition)
                    .withPositionFloodStates(Flooded, floodedPosition)
                    .withPositionFloodStates(Sunken, sunkenPosition)

                assertThat(game.availableMoves())
                    .contains(Move(player1, floodedPosition))
                    .doesNotContain(Move(player1, sunkenPosition))
            }
        }

        @Nested
        @DisplayName("Explorer")
        inner class ExplorerTests {
            @Test
            fun `Explorer can walk to diagonal positions as well as adjacent`() {
                val game = game(Explorer, Pilot)
                    .withPlayerPosition(Explorer, Position(4, 4))

                assertThat(game.availableMoves()).containsOnlyElementsOf(positionsFromMap("""
                      ..
                     ....
                    ..ooo.
                    ..o.o.
                     .ooo
                      ..
                    """).map { Move(Explorer, it) }
                )
            }

            @Test
            fun `Explorer can move to flooded diagonal location but not sunken`() {
                val floodedPosition = Position(3, 5)
                val sunkenPosition = Position(5, 3)
                val game = game(Explorer, Messenger)
                    .withPlayerPosition(Explorer, Position(4, 4))
                    .withPositionFloodStates(Flooded, floodedPosition)
                    .withPositionFloodStates(Sunken, sunkenPosition)

                assertThat(game.gameState.availableActions)
                    .contains(Move(Explorer, floodedPosition))
                    .doesNotContain(Move(Explorer, sunkenPosition))
            }
        }

        @Nested
        @DisplayName("Navigator")
        inner class NavigatorTests {
            @Test
            fun `Navigator can move other players up to 2 tiles, including changing directions`() {
                val game = game(Navigator, Engineer)
                    .withPlayerPosition(Navigator, Position(4, 4))
                    .withPlayerPosition(Engineer, Position(3, 3))

                assertThat(game.availableMoves(Engineer)).containsOnlyElementsOf(positionsFromMap("""
                      o.
                     ooo.
                    oo.oo.
                    .ooo..
                     .o..
                      ..
                """)
                )
            }

            @Test
            fun `Navigator themselves can only move to adjacent tiles`() {
                val game = game(Navigator, Messenger)
                    .withPlayerPosition(Navigator, Position(4, 4))

                assertThat(game.availableMoves(Navigator)).containsOnlyElementsOf(positionsFromMap("""
                      ..
                     ....
                    ...o..
                    ..o.o.
                     ..o.
                      ..
                    """)
                )
            }

            @Test
            fun `Navigator can move the Explorer diagonally`() {
                val game = game(Navigator, Explorer)
                    .withPlayerPosition(Explorer, Position(3, 3))

                val expectedExplorerOptions = positionsFromMap("""
                      oo
                     oooo
                    oo.oo.
                    ooooo.
                     oooo
                      ..
                """)

                assertThat(game.availableMoves(Explorer)).containsOnlyElementsOf(expectedExplorerOptions)
            }

            @Test
            fun `Navigator can move the Diver through 1 sunken tile`() {
                val game = game(Navigator, Diver)
                    .withPlayerPosition(Diver, Position(3, 3))
                    .withPositionFloodStates(Sunken, Position(4, 3))

                assertThat(game.gameState.availableActions).contains(Move(Diver, Position(5, 3)))
            }

            @Test
            fun `Navigator cannot move the Diver through more than 1 sunken tile`() {
                val game = game(Navigator, Diver)
                    .withPlayerPosition(Diver, Position(3, 3))
                    .withPositionFloodStates(Sunken, Position(4, 3), Position(5, 3))

                assertThat(game.gameState.availableActions).doesNotContain(Move(Diver, Position(6, 3)))
            }

            @Test
            fun `Navigator cannot land the anyone (incl the Diver) on flooded tiles`() {
                val game = game(Navigator, Diver)
                    .withPlayerPosition(Diver, Position(3, 3))
                    .withPositionFloodStates(Sunken, Position(5, 3))

                assertThat(game.gameState.availableActions).doesNotContain(Move(Diver, Position(5, 3)))
            }

            @Test
            fun `Navigator cannot move non-Diver players through flooded tiles`() {
                val game = game(Navigator, Engineer)
                    .withPlayerPosition(Engineer, Position(3, 3))
                    .withPositionFloodStates(Sunken, Position(4, 3))

                assertThat(game.gameState.availableActions).doesNotContain(Move(Engineer, Position(5, 3)))
            }
        }

        @Nested
        @DisplayName("Diver")
        inner class DiverTests {
            @Test
            fun `Diver can move through adjacent flooded and sunken tiles in one action, but can't land on sunken tiles`() {

                val game = game(Diver, Explorer)
                    .withPlayerPosition(Diver, Position(1, 4))
                    .withPositionFloodStates(Unflooded, Position.allPositions)
                    .withPositionFloodStates(Sunken, Position(2, 4), Position(4, 4))
                    .withPositionFloodStates(Flooded, Position(3, 4), Position(5, 4))

                assertThat(game.availableMoves())
                    .contains(Move(Diver, Position(3, 4)))
                    .contains(Move(Diver, Position(5, 4)))
                    .contains(Move(Diver, Position(6, 4)))
                    .doesNotContain(Move(Diver, Position(2, 4)))
                    .doesNotContain(Move(Diver, Position(4, 4)))
                    .containsAll((1..5).map { Move(Diver, Position(it, 3)) }) // Almost everything in the row above/**/...
                    .containsAll((2..5).map { Move(Diver, Position(it, 5)) }) // ... and the row below
                    .doesNotContain(Move(Diver, Position(1, 4)))
            }

            @Test
            fun `Diver can change direction while moving through adjacent flooded and sunken tiles`() {

                val diverPosition = Position(1, 4) // bottom-left

                val floodedPositions = positionsFromMap("""
                      o.
                     oo..
                    oo....
                    ......
                     ....
                      ..
                """)

                val game = game(Diver, Explorer)
                    .withPlayerPosition(Diver, diverPosition)
                    .withPositionFloodStates(Flooded, floodedPositions)

                assertThat(game.availableMoves()).contains(Move(Diver, Position(4, 1))) // top-right
            }

            @Test
            fun `Diver is not given option to swim to current tile`() {

                val diverPosition = Position(4, 4)
                val game = game(Diver, Explorer)
                    .withPlayerPosition(Diver, diverPosition)
                    .withPositionFloodStates(Flooded, diverPosition)

                assertThat(game.availableMoves()).doesNotContain(Move(Diver, diverPosition))
            }
        }

        @ParameterizedTest
        @MethodSource("com.grahamlea.forbiddenisland.GameStateAvailableActionsTest#nonAwaitingPlayerActionPhases")
        fun `Move not available when not awaiting a player action`(phase: GamePhase) {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(phase)

            assertThat(game.availableActions<Move>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Fly actions")
    inner class FlyTests {
        @Test
        fun `Pilot can fly to any tile that's not adjacent`() {
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))

            val allNonAdjacentPositions = positionsFromMap("""
                  oo
                 oooo
                ooo.oo
                oo...o
                 oo.o
                  oo
            """)

            assertThat(game.availableActions<Fly>()).containsOnlyElementsOf(
                allNonAdjacentPositions.map { Fly(Pilot, it) }
            )
        }

        @Test
        fun `Pilot cannot fly to any tile a second time in the same turn`() {
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))
                .withPreviousActions(Fly(Pilot, Position(4, 4)))

            assertThat(game.availableActions<Fly>()).isEmpty()
        }

        @Test
        fun `Pilot can fly to any tile on the turn after having moved to any tile`() {
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))
                .withPreviousActions(
                    Fly(Pilot, Position(3, 4)), DrawFromTreasureDeck(Pilot), DrawFromFloodDeck(Pilot),
                    Move(Explorer, Position(4, 4)), DrawFromTreasureDeck(Explorer), DrawFromFloodDeck(Explorer),
                    Move(Pilot, Position(4, 4)))

            val allNonAdjacentPositions = positionsFromMap("""
                  oo
                 oooo
                ooo.oo
                oo...o
                 oo.o
                  oo
            """)

            assertThat(game.availableActions<Fly>()).containsOnlyElementsOf(
                allNonAdjacentPositions.map { Fly(Pilot, it) }
            )
        }

        @Test
        fun `pilot can't fly to positions that have sunk`() {
            val sunkenPosition = Position(3, 1)
            val game = game(Pilot, Explorer)
                .withPlayerPosition(Pilot, Position(4, 4))
                .withPositionFloodStates(Sunken, sunkenPosition)

            assertThat(game.availableActions<Fly>()).doesNotContain(Fly(Pilot, sunkenPosition))
        }

        @ParameterizedTest
        @MethodSource("com.grahamlea.forbiddenisland.GameStateAvailableActionsTest#nonAwaitingPlayerActionPhases")
        fun `Fly not available when not awaiting a player action`(phase: GamePhase) {
            val game = game(Pilot, Navigator, Diver)
                .withGamePhase(phase)

            assertThat(game.availableActions<Fly>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Shore Up actions")
    inner class ShoreUpTests {
        @Test
        fun `Navigator, Messenger, Diver and Pilot can shore up their own position or flooded positions adjacent to them`() {
            val game = game(Navigator, Messenger, Diver, Pilot)
                .withPositionFloodStates(Flooded, Position.allPositions)

            for (player in game.gameSetup.players) {
                val availableMoves =
                    game.withPlayerPosition(player, Position(4, 4))
                        .withGamePhase(AwaitingPlayerAction(player, 3))
                        .availableActions<ShoreUp>()

                assertThat(availableMoves).containsOnlyElementsOf(positionsFromMap("""
                  ..
                 ....
                ...o..
                ..ooo.
                 ..o.
                  ..
                """).map { ShoreUp(player, it) })
            }
        }

        @RunForEachAdventurer
        fun `Unflooded or Sunken positions cannot be shored up`(player1: Adventurer, player2: Adventurer) {
            val game = newRandomGameFor(immListOf(player1, player2))
                .withPlayerPosition(player1, Position(4, 4))
                .withPositionFloodStates(Unflooded, Position.allPositions)
                .withPositionFloodStates(Sunken, Position(3, 4), Position(4, 5))

            assertThat(game.availableActions<ShoreUp>()).isEmpty()
        }

        @Test
        fun `Explorer can shore up flooded positions adjacent and diagonal to them`() {
            val game = game(Explorer, Messenger)
                .withPositionFloodStates(Flooded, Position.allPositions)
                .withPlayerPosition(Explorer, Position(4, 4))

            assertThat(game.availableActions<ShoreUp>()).containsOnlyElementsOf(positionsFromMap("""
                  ..
                 ....
                ..ooo.
                ..ooo.
                 .ooo
                  ..
                """).map { ShoreUp(Explorer, it) })
        }

        @Test
        fun `Engineer can shore up one or two flooded positions`() {
            val game = game(Engineer, Messenger)
                .withPositionFloodStates(Flooded, Position.allPositions)
                .withPlayerPosition(Engineer, Position(4, 4))

            val validPositions = positionsFromMap("""
              ..
             ....
            ...o..
            ..ooo.
             ..o.
              ..
            """)

            val validCombinations = Pair(Engineer, validPositions).let { (p, vp) ->
                listOf(
                    ShoreUp(p, vp[0], vp[1]), ShoreUp(p, vp[0], vp[2]), ShoreUp(p, vp[0], vp[3]), ShoreUp(p, vp[0], vp[4]),
                    ShoreUp(p, vp[1], vp[2]), ShoreUp(p, vp[1], vp[3]), ShoreUp(p, vp[1], vp[4]),
                    ShoreUp(p, vp[2], vp[3]), ShoreUp(p, vp[2], vp[4]),
                    ShoreUp(p, vp[3], vp[4])
                )
            }

            assertThat(game.availableActions<ShoreUp>()).containsOnlyElementsOf(
                validPositions.map { ShoreUp(Engineer, it) } + validCombinations
            )
        }

        @ParameterizedTest
        @MethodSource("com.grahamlea.forbiddenisland.GameStateAvailableActionsTest#nonAwaitingPlayerActionPhases")
        fun `Shore Up not available when not awaiting a player action`(phase: GamePhase) {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(phase)

            assertThat(game.availableActions<ShoreUp>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Give Treasure Card actions")
    inner class GiveTreasureCardTests {

        @RunForEachAdventurer(except = [Messenger])
        fun `current player can give treasure cards to players on the same tile`(
            player1: Adventurer, player2: Adventurer, player3: Adventurer, player4: Adventurer) {

            val game = game(player1, player2, player3, player4)
                .withPlayerPosition(player1, Position(4, 4))
                .withPlayerPosition(player2, Position(4, 4))
                .withPlayerPosition(player3, Position(4, 4))
                .withPlayerPosition(player4, Position(3, 3)) // DIFFERENT!
                .withPlayerCards(
                    player1 to cards(earth, fire, fire),
                    player2 to cards(ocean),
                    player3 to cards(),
                    player4 to cards()
                )

            assertThat(game.availableActions<GiveTreasureCard>()).containsOnly(
                GiveTreasureCard(player1, player2, earth),
                GiveTreasureCard(player1, player2, fire),
                GiveTreasureCard(player1, player3, earth),
                GiveTreasureCard(player1, player3, fire)
            )
        }

        @Test
        fun `messenger can give treasure cards to any players`() {
            val game = game(Messenger, Diver)
                .withPlayerPosition(Messenger, Position(4, 4))
                .withPlayerPosition(Diver, Position(1, 3))
                .withPlayerCards(
                    Messenger to cards(earth, fire, fire),
                    Diver to cards(ocean)
                )

            assertThat(game.availableActions<GiveTreasureCard>()).containsOnly(
                GiveTreasureCard(Messenger, Diver, earth),
                GiveTreasureCard(Messenger, Diver, fire)
            )
        }


        @ParameterizedTest
        @MethodSource("com.grahamlea.forbiddenisland.GameStateAvailableActionsTest#nonAwaitingPlayerActionPhases")
        fun `GiveTreasureCard not available when not awaiting a player action`(phase: GamePhase) {
            val game = game(Messenger, Navigator, Diver)
                .withPlayerPosition(Messenger, Position(3, 3))
                .withPlayerPosition(Navigator, Position(3, 3))
                .withPlayerPosition(Diver, Position(3, 3))
                .withGamePhase(phase)

            assertThat(game.availableActions<GiveTreasureCard>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Capture Treasure actions")
    inner class CaptureTreasureTests {

        private fun Game.permittingTreasureCapture() = this.let {
            it.withPlayerLocation(it.gameSetup.players[0], TempleOfTheSun)
                .withPlayerCards(
                    it.gameSetup.players[0] to cards(earth, earth, earth, earth),
                    it.gameSetup.players[1] to cards()
                )
        }

        private val gamePermittingCapture = newRandomGameFor(2).permittingTreasureCapture()

        @Test
        fun `player on a treasure location with 4 matching treasure cards can capture the treasure`() {
            val testGame = gamePermittingCapture

            assertThat(testGame.availableActions<CaptureTreasure>()).containsOnly(
                CaptureTreasure(testGame.gameSetup.players[0], Treasure.EarthStone)
            )
        }

        @Test
        fun `player with 5 matching treasure cards can capture the treasure too`() {
            val testGame = gamePermittingCapture.let {
                it.withPlayerCards(
                    it.gameSetup.players[0] to cards(earth, earth, earth, earth, earth),
                    it.gameSetup.players[1] to cards()
                )
            }

            assertThat(testGame.availableActions<CaptureTreasure>()).containsOnly(
                CaptureTreasure(testGame.gameSetup.players[0], Treasure.EarthStone)
            )
        }

        @Test
        fun `player on a treasure location with less than 4 matching treasure cards CANNOT capture the treasure`() {
            val testGame = gamePermittingCapture.let {
                it.withPlayerCards(
                    it.gameSetup.players[0] to cards(earth, earth, earth),
                    it.gameSetup.players[1] to cards()
                )
            }

            assertThat(testGame.availableActions<CaptureTreasure>()).isEmpty()
        }

        @Test
        fun `player on a treasure location with 4 matching treasure cards of another treasure CANNOT capture the treasure`() {
            val testGame = gamePermittingCapture.let {
                it.withPlayerCards(
                    it.gameSetup.players[0] to cards(ocean, ocean, ocean, ocean),
                    it.gameSetup.players[1] to cards()
                )
            }

            assertThat(testGame.availableActions<CaptureTreasure>()).isEmpty()
        }

        @Test
        fun `player with 4 matching treasure cards not on a pickup location CANNOT capture the treasure`() {
            val testGame = gamePermittingCapture.let {
                it.withPlayerLocation(it.gameSetup.players[0], Observatory)
            }

            assertThat(testGame.availableActions<CaptureTreasure>()).isEmpty()
        }

        @Test
        fun `player can only capture a treasure when it's their turn`() {
            val testGame = gamePermittingCapture
                .withGamePhase(AwaitingPlayerAction(gamePermittingCapture.gameSetup.players[1], 3))

            assertThat(testGame.availableActions<CaptureTreasure>()).isEmpty()
        }

        @Test
        fun `player can not capture a treasure already captured`() {
            val testGame = gamePermittingCapture.withTreasuresCollected(Treasure.EarthStone)

            assertThat(testGame.availableActions<CaptureTreasure>()).isEmpty()
        }

        @ParameterizedTest
        @MethodSource("com.grahamlea.forbiddenisland.GameStateAvailableActionsTest#nonAwaitingPlayerActionPhases")
        fun `GiveTreasureCard not available when not awaiting a player action`(phase: GamePhase) {
            val game = game(Engineer, Navigator)
                .permittingTreasureCapture()
                .withGamePhase(phase)

            assertThat(game.availableActions<GiveTreasureCard>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Helicopter Lift actions")
    inner class HelicopterLiftTests {

        @RunForEachAdventurer
        fun `any player with a Helicopter Lift can move any Player anywhere on anyone's turn`(
            player1: Adventurer, player2: Adventurer, player3: Adventurer) {

            val player1Position = Position(3, 1)
            val player2Position = Position(4, 2)
            val player3Position = Position(5, 3)
            val game = game(player1, player2, player3)
                .withPlayerPosition(player1, player1Position)
                .withPlayerPosition(player2, player2Position)
                .withPlayerPosition(player3, player3Position)
                .withPlayerCards(
                    player1 to cards(),
                    player2 to cards(HelicopterLiftCard),
                    player3 to cards()
                )

            val player1FlightDestinations = positionsFromMap("""
                      .o
                     oooo
                    oooooo
                    oooooo
                     oooo
                      oo
                """)

            val player2FlightDestinations = positionsFromMap("""
                      oo
                     oo.o
                    oooooo
                    oooooo
                     oooo
                      oo
                """)

            val player3FlightDestinations = positionsFromMap("""
                      oo
                     oooo
                    oooo.o
                    oooooo
                     oooo
                      oo
                """)

            fun Pair<Adventurer, List<Position>>.toLiftActions() =
                this.second.map { HelicopterLift(player2, immSetOf(this.first), it) }

            assertThat(game.availableActions<HelicopterLift>()).containsOnlyElementsOf(
                Pair(player1, player1FlightDestinations).toLiftActions() +
                    Pair(player2, player2FlightDestinations).toLiftActions() +
                    Pair(player3, player3FlightDestinations).toLiftActions()
            )
        }

        @Test
        fun `cannot helicopter lift a player to a sunken tile`() {
            val location = Observatory
            val game = game(Messenger, Navigator)
                .withLocationFloodStates(Sunken, location)
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards()
                )
            val position = game.gameSetup.positionOf(location)

            assertThat(game.availableActions<HelicopterLift>()).doesNotContain(
                HelicopterLift(Messenger, immSetOf(Messenger), position),
                HelicopterLift(Messenger, immSetOf(Navigator), position)
            )
        }

        @Test
        fun `any combination of multiple players from the same tile can be helicopter lifted at the same time`() {
            val game = game(Diver, Explorer, Messenger, Navigator)
                .withPlayerPosition(Diver, Position(4, 4))
                .withPlayerPosition(Explorer, Position(4, 4))
                .withPlayerPosition(Messenger, Position(3, 3))
                .withPlayerPosition(Navigator, Position(4, 4))
                .withPlayerCards(
                    Diver to cards(),
                    Explorer to cards(),
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards()
                )

            assertThat(game.availableActions<HelicopterLift>()).contains(
                HelicopterLift(Messenger, immSetOf(Diver), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Explorer), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Navigator), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Diver, Explorer), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Diver, Navigator), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Explorer, Navigator), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Diver, Explorer, Navigator), Position(1, 3))
            ).doesNotContain(
                HelicopterLift(Messenger, immSetOf(Explorer, Messenger), Position(1, 3)),
                HelicopterLift(Messenger, immSetOf(Messenger, Navigator), Position(1, 3))
            )
        }

        @Test
        fun `helicopter lift can be used while awaiting a Treasure Deck Draw`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingTreasureDeckDraw(Navigator, 2))

            assertThat(game.availableActions<HelicopterLift>()).isNotEmpty()
        }

        @Test
        fun `helicopter lift can be used while awaiting a Flood Deck Draw`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingFloodDeckDraw(Navigator, 2))

            assertThat(game.availableActions<HelicopterLift>()).isNotEmpty()
        }

        @Test
        fun `helicopter lift can be used by discarding player when card needs to be discarded`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingTreasureDeckDraw(Messenger, 1)))

            assertThat(game.availableActions<HelicopterLift>()).isNotEmpty()
        }

        @Test
        fun `helicopter lift CANNOT be used by another player when card needs to be discarded`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(),
                    Navigator to cards(HelicopterLiftCard)
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingTreasureDeckDraw(Messenger, 1)))

            assertThat(game.availableActions<HelicopterLift>()).isEmpty()
        }

        @Test
        fun `helicopter lift cannot be used when a player needs to swim to safety`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingPlayerToSwimToSafety(Navigator, AwaitingFloodDeckDraw(Messenger, 1)))

            assertThat(game.availableActions<HelicopterLift>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Sandbag actions")
    inner class SandbagTests {

        @RunForEachAdventurer
        fun `any player with a Sandbag can use it on any flooded location on anyone's turn`(
            player1: Adventurer, player2: Adventurer, player3: Adventurer) {

            val floodedPositions = positionsFromMap("""
                  .o
                 oo..
                ......
                ...oo.
                 o..o
                  o.
            """)

            val game = game(player1, player2, player3)
                .withPositionFloodStates(Unflooded, Position.allPositions)
                .withPositionFloodStates(Flooded, floodedPositions)
                .withPlayerCards(
                    player1 to cards(),
                    player2 to cards(SandbagsCard),
                    player3 to cards()
                )

            assertThat(game.availableActions<Sandbag>()).containsOnlyElementsOf(
                floodedPositions.map { Sandbag(player2, it) }
            )
        }

        @Test
        fun `cannot sandbag a sunken location`() {
            val location = Observatory
            val game = game(Messenger, Navigator)
                .withLocationFloodStates(Sunken, location)
                .withPlayerCards(
                    Messenger to cards(SandbagsCard),
                    Navigator to cards()
                )
            val position = game.gameSetup.positionOf(location)

            assertThat(game.availableActions<Sandbag>()).doesNotContain(
                Sandbag(Messenger, position)
            )
        }

        @Test
        fun `sandbag can be used while awaiting a Treasure Deck Draw`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(SandbagsCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingTreasureDeckDraw(Navigator, 2))

            assertThat(game.availableActions<Sandbag>()).isNotEmpty()
        }

        @Test
        fun `sandbag can be used while awaiting a Flood Deck Draw`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(SandbagsCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingFloodDeckDraw(Navigator, 2))

            assertThat(game.availableActions<Sandbag>()).isNotEmpty()
        }

        @Test
        fun `sandbag can be used by discarding player when card needs to be discarded`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(SandbagsCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingTreasureDeckDraw(Messenger, 1)))

            assertThat(game.availableActions<Sandbag>()).isNotEmpty()
        }

        @Test
        fun `sandbag CANNOT be used by another player when card needs to be discarded`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(),
                    Navigator to cards(SandbagsCard)
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingTreasureDeckDraw(Messenger, 1)))

            assertThat(game.availableActions<Sandbag>()).isEmpty()
        }

        @Test
        fun `sandbag cannot be used when a player needs to swim to safety`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(SandbagsCard),
                    Navigator to cards()
                )
                .withGamePhase(AwaitingPlayerToSwimToSafety(Navigator, AwaitingFloodDeckDraw(Messenger, 1)))

            assertThat(game.availableActions<Sandbag>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Draw from Treasure Deck actions")
    inner class DrawFromTreasureDeckTests {

        @Test
        fun `draw from treasure deck allowed by player having turn when awaiting it`() {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(AwaitingTreasureDeckDraw(Navigator, 2))

            assertThat(game.availableActions<DrawFromTreasureDeck>()).containsOnly(
                DrawFromTreasureDeck(Navigator)
            )
        }

        @Test
        fun `draw from treasure deck allowed by player instead of using remaining actions in turn`() {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(AwaitingPlayerAction(Navigator, 2))

            assertThat(game.availableActions<DrawFromTreasureDeck>()).containsOnly(
                DrawFromTreasureDeck(Navigator)
            )
        }

        @ParameterizedTest
        @MethodSource("non-available phases")
        fun `draw from treasure deck not available in most other phases`(phase: GamePhase) {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(phase)

            assertThat(game.availableActions<DrawFromTreasureDeck>()).isEmpty()
        }

        @Suppress("unused")
        private fun `non-available phases`(): List<GamePhase> =
            listOf(
                AwaitingFloodDeckDraw(Navigator, 2),
                AwaitingPlayerToSwimToSafety(Navigator, AwaitingPlayerAction(Navigator, 1)),
                AwaitingPlayerToDiscardExtraCard(Navigator, AwaitingPlayerAction(Navigator, 1)),
                GameOver
            )
    }

    @Nested
    @DisplayName("Draw from Flood Deck actions")
    inner class DrawFromFloodDeckTests {

        @Test
        fun `draw from flood deck allowed by player having turn when awaiting it`() {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(AwaitingFloodDeckDraw(Navigator, 2))

            assertThat(game.availableActions<DrawFromFloodDeck>()).containsOnly(
                DrawFromFloodDeck(Navigator)
            )
        }

        @ParameterizedTest
        @MethodSource("non-DrawFromFloodDeck phases")
        fun `draw from flood deck not available in any other phase`(phase: GamePhase) {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(phase)

            assertThat(game.availableActions<DrawFromFloodDeck>()).isEmpty()
        }

        @Suppress("unused")
        private fun `non-DrawFromFloodDeck phases`(): List<GamePhase> =
            listOf(
                AwaitingPlayerAction(Navigator, 1),
                AwaitingTreasureDeckDraw(Navigator, 2),
                AwaitingPlayerToSwimToSafety(Navigator, AwaitingPlayerAction(Navigator, 1)),
                AwaitingPlayerToDiscardExtraCard(Navigator, AwaitingPlayerAction(Navigator, 1)),
                GameOver
            )
    }

    @Nested
    @DisplayName("Discard Card actions")
    inner class DiscardCardTests {

        @Test
        fun `discard card is the only action available when a player has 6 treasure cards`() {
            val game = game(Messenger, Navigator)
                .withPlayerCards(
                    Messenger to cards(),
                    Navigator to cards(earth, earth, earth, earth, earth, ocean)
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Navigator, AwaitingPlayerAction(Messenger, 2)))

            assertThat(game.availableActions<GameAction>()).containsOnly(
                DiscardCard(Navigator, earth),
                DiscardCard(Navigator, ocean)
            ).hasSize(2)
        }

        @ParameterizedTest
        @MethodSource("non-DiscardCard phases")
        fun `draw from flood deck not available in any other phase`(phase: GamePhase) {
            val game = game(Messenger, Navigator, Diver)
                .withGamePhase(phase)

            assertThat(game.availableActions<DiscardCard>()).isEmpty()
        }

        @Suppress("unused")
        private fun `non-DiscardCard phases`(): List<GamePhase> =
            listOf(
                AwaitingPlayerAction(Navigator, 1),
                AwaitingTreasureDeckDraw(Navigator, 2),
                AwaitingFloodDeckDraw(Navigator, 2),
                AwaitingPlayerToSwimToSafety(Navigator, AwaitingPlayerAction(Navigator, 1)),
                GameOver
            )
    }

    @Nested
    @DisplayName("Swim to Safety actions")
    inner class SwimToSafetyTests {

        @RunForEachAdventurer(except = [Explorer, Pilot])
        fun `Diver, Engineer, Messenger and Navigator can all swim to safety at any adjacent flooded or unflooded location`(
            player1: Adventurer, player2: Adventurer
        ) {
            val player1Position = Position(4, 4)

            val adjacentPositions = player1Position.adjacentPositions(false)
            val map = GameMap.newRandomMap().withLocationNotAtAnyOf(FoolsLanding, adjacentPositions)

            val unfloodedPosition = adjacentPositions.shuffled()[0]
            val sunkenPosition = (adjacentPositions - unfloodedPosition).shuffled()[0]
            val floodedPositions = adjacentPositions - unfloodedPosition - sunkenPosition

            val game = newRandomGameFor(immListOf(player1, player2), map)
                .withPlayerPosition(player1, player1Position)
                .withPlayerPosition(player2, Position(1, 3))
                .withPositionFloodStates(Unflooded, unfloodedPosition)
                .withPositionFloodStates(Sunken, sunkenPosition)
                .withPositionFloodStates(Flooded, floodedPositions)
                .withGamePhase(AwaitingPlayerToSwimToSafety(player1, AwaitingPlayerAction(player2, 2)))

            printGameOnFailure(game) {
                assertThat(game.availableActions<GameAction>()).containsOnly(
                    SwimToSafety(player1, unfloodedPosition),
                    SwimToSafety(player1, floodedPositions[0]),
                    SwimToSafety(player1, floodedPositions[1])
                )
            }
        }

        @Test
        fun `Explorer can swim to safety at any adjacent or diagonal flooded or unflooded location`() {
            val explorerPosition = Position(4, 4)

            val adjacentPositions = explorerPosition.adjacentPositions(false)
            val diagonalPositions = explorerPosition.adjacentPositions(true) - adjacentPositions
            val map = GameMap.newRandomMap().withLocationNotAtAnyOf(FoolsLanding, adjacentPositions + diagonalPositions)

            val unfloodedPositions = listOf(adjacentPositions.shuffled()[0], diagonalPositions.shuffled()[0])
            val sunkenPositions = listOf(
                (adjacentPositions - unfloodedPositions[0]).shuffled()[0],
                (diagonalPositions - unfloodedPositions[1]).shuffled()[0]
            )
            val floodedPositions = adjacentPositions + diagonalPositions - unfloodedPositions - sunkenPositions

            val game = newRandomGameFor(immListOf(Explorer, Engineer), map)
                .withPlayerPosition(Explorer, explorerPosition)
                .withPlayerPosition(Engineer, Position(1, 3))
                .withPositionFloodStates(Unflooded, unfloodedPositions)
                .withPositionFloodStates(Sunken, sunkenPositions)
                .withPositionFloodStates(Flooded, floodedPositions)
                .withGamePhase(AwaitingPlayerToSwimToSafety(Explorer, AwaitingPlayerAction(Engineer, 2)))

            printGameOnFailure(game) {
                assertThat(game.availableActions<GameAction>()).containsOnly(
                    SwimToSafety(Explorer, unfloodedPositions[0]),
                    SwimToSafety(Explorer, unfloodedPositions[1]),
                    SwimToSafety(Explorer, floodedPositions[0]),
                    SwimToSafety(Explorer, floodedPositions[1]),
                    SwimToSafety(Explorer, floodedPositions[2]),
                    SwimToSafety(Explorer, floodedPositions[3])
                )
            }
        }

        @Test
        fun `Pilot can "swim" to safety at any un-Sunken location`() {
            val pilotPosition = Position(4, 4)

            val map = GameMap.newRandomMap().withLocationNotAtAnyOf(FoolsLanding, listOf(pilotPosition))

            val foolsLandingPos = map.positionOf(FoolsLanding)
            val unfloodedPositions = (Position.allPositions - pilotPosition - foolsLandingPos).shuffled().subList(0, 7) + foolsLandingPos
            val sunkenPositions = (Position.allPositions - pilotPosition - unfloodedPositions).shuffled().subList(0, 7) + pilotPosition
            val floodedPositions = Position.allPositions - unfloodedPositions - sunkenPositions

            val game = newRandomGameFor(immListOf(Pilot, Engineer), map)
                .withPlayerPosition(Pilot, pilotPosition)
                .withPlayerPosition(Engineer, Position(1, 3))
                .withPositionFloodStates(Unflooded, unfloodedPositions)
                .withPositionFloodStates(Sunken, sunkenPositions)
                .withPositionFloodStates(Flooded, floodedPositions)
                .withGamePhase(AwaitingPlayerToSwimToSafety(Pilot, AwaitingPlayerAction(Engineer, 2)))

            printGameOnFailure(game) {
                assertThat(game.availableActions<GameAction>()).containsOnlyElementsOf(
                    (0..7).map { SwimToSafety(Pilot, unfloodedPositions[it]) } +
                    (0..7).map { SwimToSafety(Pilot, floodedPositions[it]) }
                )
            }
        }

        @Test
        fun `Diver can swim to safety at the closest unsunken location(s)`() {
            // "Closest" is determined by the number of *adjacent* moves (not diagonals) required to reach a position
            // In the map below, the equal closest unsunken positions are (5, 4), (2, 4), (5, 5) and (4, 6), each two moves away

            val diverPosition = Position(4, 4)

            val sunkenPositions = positionsFromMap("""
                  ..
                 ..o.
                ..oo..
                ..oooo
                 .oo.
                  ..
            """)

            val floodedPositions = positionsFromMap("""
                  ..
                 ....
                ......
                .o....
                 ...o
                  ..
            """)

            val map = GameMap.newRandomMap().withLocationNotAtAnyOf(FoolsLanding, sunkenPositions)

            val game = newRandomGameFor(immListOf(Diver, Messenger), map)
                .withPlayerPosition(Diver, diverPosition)
                .withPositionFloodStates(Unflooded, Position.allPositions)
                .withPositionFloodStates(Flooded, floodedPositions)
                .withPositionFloodStates(Sunken, sunkenPositions)
                .withGamePhase(AwaitingPlayerToSwimToSafety(Diver, AwaitingPlayerAction(Messenger, 2)))

            printGameOnFailure(game) {
                assertThat(game.availableActions<GameAction>()).containsOnly(
                    SwimToSafety(Diver, Position(5, 3)),
                    SwimToSafety(Diver, Position(2, 4)),
                    SwimToSafety(Diver, Position(5, 5)),
                    SwimToSafety(Diver, Position(4, 6))
                )
            }
        }
    }

    @Nested
    @DisplayName("Helicopter Lift Off Island action")
    inner class HelicopterLifOffIslandTests {

        @Suppress("unused")
        private fun allowedPhases(): List<GamePhase> = listOf(
            AwaitingPlayerAction(Navigator, 1),
            AwaitingFloodDeckDraw(Navigator, 1),
            AwaitingTreasureDeckDraw(Navigator, 2)
        )

        @Suppress("unused")
        private fun notAllowedPhases(): List<GamePhase> = listOf(
            AwaitingPlayerToSwimToSafety(Navigator, AwaitingPlayerAction(Navigator, 1)),
            GameOver
        )

        @ParameterizedTest
        @MethodSource("allowedPhases")
        fun `helicopter lift off island is available when all treasures are collected, all players are on Fool's Landing and someone has a HelicopterLiftCard`(phase: GamePhase) {
            val cards = mapOf(
                Messenger to cards(),
                Navigator to cards(),
                Diver to cards()
            )
            val savingPlayer = cards.keys.toList()[random.nextInt(3)]

            val game = game(Messenger, Navigator, Diver)
                .withPlayerLocation(Messenger, FoolsLanding)
                .withPlayerLocation(Navigator, FoolsLanding)
                .withPlayerLocation(Diver, FoolsLanding)
                .withPlayerCards(cards + (savingPlayer to cards(HelicopterLiftCard)))
                .withTreasuresCollected(*Treasure.values())
                .withGamePhase(phase)

            assertThat(game.availableActions<HelicopterLiftOffIsland>())
                .containsOnly(HelicopterLiftOffIsland(savingPlayer))
        }

        @Test
        fun `helicopter lift off island is NOT available when not all treasures are collected`() {

            val game = game(Messenger, Navigator, Diver)
                    .withPlayerLocation(Messenger, FoolsLanding)
                        .withPlayerLocation(Navigator, FoolsLanding)
                        .withPlayerLocation(Diver, FoolsLanding)
            .withTreasuresCollected(*Treasure.values().toList().shuffled().subList(0, 3).toTypedArray())
                .withPlayerCards(
                Messenger to cards(HelicopterLiftCard),
                Navigator to cards(),
                Diver to cards()
            )

            assertThat(game.availableActions<HelicopterLiftOffIsland>()).isEmpty()
        }

        @Test
        fun `helicopter lift off island is NOT available when not all players are on Fool's Landing`() {

            val game = game(Messenger, Navigator, Diver)
                .withPlayerLocation(Messenger, Observatory)
                .withPlayerLocation(Navigator, FoolsLanding)
                .withPlayerLocation(Diver, FoolsLanding)
                .withTreasuresCollected(*Treasure.values())
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards(),
                    Diver to cards()
                )

            assertThat(game.availableActions<HelicopterLiftOffIsland>()).isEmpty()
        }

        @Test
        fun `helicopter lift off island is NOT available when no one has a Helicopter Lift card`() {

            val game = game(Messenger, Navigator, Diver)
                .withPlayerLocation(Messenger, FoolsLanding)
                .withPlayerLocation(Navigator, FoolsLanding)
                .withPlayerLocation(Diver, FoolsLanding)
                .withTreasuresCollected(*Treasure.values())
                .withPlayerCards(
                    Messenger to cards(),
                    Navigator to cards(),
                    Diver to cards()
                )

            assertThat(game.availableActions<HelicopterLiftOffIsland>()).isEmpty()
        }

        @Test
        fun `helicopter lift off island is available when player with a Helicopter Lift card has to discard a card`() {

            val game = game(Messenger, Navigator, Diver)
                .withPlayerLocation(Messenger, FoolsLanding)
                .withPlayerLocation(Navigator, FoolsLanding)
                .withPlayerLocation(Diver, FoolsLanding)
                .withTreasuresCollected(*Treasure.values())
                .withPlayerCards(
                    Messenger to cards(ocean, ocean, ocean, ocean, ocean, HelicopterLiftCard),
                    Navigator to cards(),
                    Diver to cards()
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Navigator, 2)))

            assertThat(game.availableActions<HelicopterLiftOffIsland>()).isNotEmpty()
        }

        @Test
        fun `helicopter lift off island is NOT available when a player other than one with a Helicopter Lift card has to discard a card`() {

            val game = game(Messenger, Navigator, Diver)
                .withPlayerLocation(Messenger, FoolsLanding)
                .withPlayerLocation(Navigator, FoolsLanding)
                .withPlayerLocation(Diver, FoolsLanding)
                .withTreasuresCollected(*Treasure.values())
                .withPlayerCards(
                    Messenger to cards(ocean, ocean, ocean, ocean, ocean, earth),
                    Navigator to cards(HelicopterLiftCard),
                    Diver to cards()
                )
                .withGamePhase(AwaitingPlayerToDiscardExtraCard(Messenger, AwaitingPlayerAction(Navigator, 2)))

            assertThat(game.availableActions<HelicopterLiftOffIsland>()).isEmpty()
        }

        @ParameterizedTest
        @MethodSource("notAllowedPhases")
        fun `helicopter lift off island is NOT available when someone needs to swim to safety or the game is already over`(phase: GamePhase) {

            val game = game(Messenger, Navigator, Diver)
                .withPlayerLocation(Messenger, FoolsLanding)
                .withPlayerLocation(Navigator, FoolsLanding)
                .withPlayerLocation(Diver, FoolsLanding)
                .withTreasuresCollected(*Treasure.values())
                .withPlayerCards(
                    Messenger to cards(HelicopterLiftCard),
                    Navigator to cards(),
                    Diver to cards()
                )
                .withGamePhase(phase)

            assertThat(game.availableActions<HelicopterLiftOffIsland>()).isEmpty()
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused", "RedundantVisibilityModifier")
        public fun nonAwaitingPlayerActionPhases(): List<GamePhase> =
            listOf(
                AwaitingFloodDeckDraw(Navigator, 1),
                AwaitingTreasureDeckDraw(Navigator, 2),
                AwaitingPlayerToSwimToSafety(Navigator, AwaitingPlayerAction(Navigator, 1)),
                AwaitingPlayerToDiscardExtraCard(Navigator, AwaitingPlayerAction(Navigator, 1)),
                GameOver
            )
    }
}

private fun Game.availableMoves(player: Adventurer): List<Position> =
    availableMoves().filter { it.player == player }.map(Move::position)

private fun Game.availableMoves(): List<Move> = availableActions()

private inline fun <reified T: GameAction> Game.availableActions(): List<T> =
    gameState.availableActions.mapNotNull { it as? T }
