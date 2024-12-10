package gg.flyte.christmas.minigame.engine

enum class GameState {
    /**
     * The [GameConfig] (game type) has been selected, but has no requests to start.
     */
    IDLE,

    /**
     * The game is waiting for players to join.
     */
    WAITING_FOR_PLAYERS,

    /**
     * The game is counting down to start.
     */
    COUNTDOWN,

    /**
     * The game is overviewing/previewing the map and instructing players.
     */
    OVERVIEWING,

    /**
     * The game is running.
     */
    LIVE,
}
