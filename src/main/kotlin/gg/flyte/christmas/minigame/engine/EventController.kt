package gg.flyte.christmas.minigame.engine

import com.xxmicloxx.NoteBlockAPI.model.Playlist
import com.xxmicloxx.NoteBlockAPI.model.SoundCategory
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donation.DonateEvent
import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.util.*
import gg.flyte.christmas.visual.SidebarManager
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.TwilightRunnable
import gg.flyte.twilight.scheduler.async
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Sound
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import java.time.Duration
import java.util.*
import kotlin.reflect.full.primaryConstructor

/**
 * The controller for the event, handling the current game (and its state), countdown, player management,
 * and other UI and event-related elements.
 */
class EventController() {
    var currentGame: EventMiniGame? = null
    var countdownTask: TwilightRunnable? = null
    val countdownMap = mapOf(
        10 to "<green>➓".style(),
        5 to "<green>➎".style(),
        4 to "<gold>➍".style(),
        3 to "<gold>➌".style(),
        2 to "<red>➋".style(),
        1 to "<dark_red>➊".style()
    )
    val optOut = mutableSetOf<UUID>()
    val points = mutableMapOf<UUID, Int>()
    var songPlayer: RadioSongPlayer? = null
    val sidebarManager = SidebarManager().also { it.dataSupplier = points }
    val donors = mutableSetOf<UUID>()
    var totalDonations = 0
    var donationGoal = 10000
    var donationBossBar = BossBar.bossBar(
        "<colour:#1DF1BC>ᴅᴏɴᴀᴛɪᴏɴ ɢᴏᴀʟ: <white>$<grey>${totalDonations}/${donationGoal}".style(),
        0F,
        BossBar.Color.GREEN,
        BossBar.Overlay.PROGRESS
    )

    /**
     * Sets the current game to the provided game configuration.
     * **Note:** This does not start the game, only sets the game to be played.
     *
     * @see prepareStart
     */
    fun setMiniGame(gameConfig: GameConfig) {
        currentGame = gameConfig.gameClass.primaryConstructor?.call()
    }

    /**
     * Prepares the game to start by setting the state to `WAITING_FOR_PLAYERS` and checking if there are enough players.
     */
    fun prepareStart() {
        // Note: currentGame asserted to not-null due to previous checks.
        currentGame!!.state = GameState.WAITING_FOR_PLAYERS

        if (enoughPlayers()) {
            currentGame!!.state = GameState.COUNTDOWN
            countdown()
            return
        }
    }

    /**
     * Starts the countdown for the game to begin. If there are enough players to start the game
     * by the end of the countdown, [EventMiniGame.startGameOverview] is invoked.
     *
     * Players leaving during the countdown is handled through [onPlayerQuit]. That cancels the
     * countdown task if there are not enough players to start the game.
     */
    private fun countdown() {
        var seconds = 10
        countdownTask = repeatingTask(1, TimeUnit.SECONDS) {
            when (seconds) {
                0 -> {
                    currentGame!!.startGameOverview()
                    currentGame!!.state = GameState.LIVE
                    cancel()
                }

                else -> {
                    Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
                        countdownMap[seconds]?.let { number ->
                            it.title(number, Component.empty(), titleTimes(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO))
                            it.playSound(Sound.UI_BUTTON_CLICK)
                            it.sendMessage("<grey>Game starting in <0> seconds...".style(number))
                        }
                    }
                    seconds--
                }
            }
        }
    }

    /**
     * @return `true` if there are enough players to start the game, dictated by the minimum player count
     * through [GameConfig.minPlayers].
     */
    private fun enoughPlayers(): Boolean {
        return Util.runAction(PlayerType.PARTICIPANT) {}.size >= currentGame!!.gameConfig.minPlayers
    }

    /**
     * Handles the player joining the server.
     *
     * @param player The player that joined the server.
     */
    fun onPlayerJoin(player: Player) {
        if (currentGame == null) {
            player.showBossBar(eventController().donationBossBar)
            player.teleport(ChristmasEventPlugin.instance.lobbySpawn)
        } else {
            if (currentGame!!.state == GameState.WAITING_FOR_PLAYERS) {
                if (enoughPlayers()) {
                    currentGame!!.state = GameState.COUNTDOWN
                    countdown()
                }
            }

            if (currentGame!!.state == GameState.LIVE) currentGame!!.onPlayerJoin(player)
        }
    }

    /**
     * Handles the player quitting the server, and potential implications depending on the current game state.
     * @param player The player that quit the server.
     */
    fun onPlayerQuit(player: Player) {
        sidebarManager.remove(player)

        when (currentGame?.state) {

            GameState.COUNTDOWN -> {
                delay(1) { // getOnlinePlayers [called through enoughPlayers()] does not update until the next tick
                    if (!enoughPlayers()) {
                        countdownTask?.cancel()
                        currentGame!!.state = GameState.WAITING_FOR_PLAYERS

                        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
                            it.title(
                                "<dark_red>⦅x⦆".style(), "<red>Waiting for more players...".style(),
                                titleTimes(Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(1))
                            )
                            it.playSound(Sound.BLOCK_NOTE_BLOCK_BASS)
                        }
                    }
                }
            }

            GameState.LIVE -> currentGame!!.onPlayerQuit(player)

            else -> return
        }
    }

    /**
     * Starts the playlist of songs for the event.
     *
     * @param avoid The song to avoid playing, if any.
     */
    fun startPlaylist(avoid: SongReference? = null) {
        songPlayer?.destroy()

        songPlayer = RadioSongPlayer(Playlist(*SongReference.entries.map { it.song }.toTypedArray()), SoundCategory.RECORDS)
        songPlayer!!.isRandom = true
        songPlayer!!.isPlaying = true

        fun checkAndSkip() {
            if (avoid == null) return

            if (songPlayer!!.song.path.path == avoid.name + ".nbs") {
                songPlayer!!.playNextSong()
                delay(1) { checkAndSkip() }
            }
        }

        checkAndSkip()
        Bukkit.getOnlinePlayers().forEach(songPlayer!!::addPlayer)
    }

    /**
     * Adds points to the player with the provided UUID.
     *
     * @param uuid The UUID of the player to add points to.
     * @param amount The amount of points to add.
     */
    fun addPoints(uuid: UUID, amount: Int) {
        points[uuid] = points.getOrDefault(uuid, 0) + amount // will never default to zero. PlayerJoinEvent puts 0 points
    }

    /**
     * Serialises the points to the `config.yml` file. Called after each game ends.
     */
    fun serialisePoints() {
        async {
            ChristmasEventPlugin.instance.config.set("points", null)
            points.forEach { (uuid, points) -> ChristmasEventPlugin.instance.config.set("points.$uuid", points) }

            ChristmasEventPlugin.instance.saveConfig()
        }
    }

    /**
     * Parses a [DonateEvent] and handles UI and game-related elements. Each implementation of
     * [EventMiniGame] will have different actions depending on the tier of the donation.
     *
     * @param event The donation event to handle.
     * @see DonationTier
     * @see EventMiniGame.handleDonation
     */
    fun handleDonation(event: DonateEvent) {
        if (event.amount == null) return // no clue why this would happen, but just in case
        var value = event.amount.toDouble()
        if (value < 0) return // no negative donations (don't think this is possible)

        updateDonationBar()

        Bukkit.getOnlinePlayers().forEach {
            // spawn firework
            ChristmasEventPlugin.instance.serverWorld.spawn(it.location, Firework::class.java) {
                it.fireworkMeta = it.fireworkMeta.apply {
                    addEffect(
                        FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(Color.RED, Color.LIME, Color.GREEN)
                            .withFlicker()
                            .withTrail()
                            .build()
                    )
                    power = 1
                }
            }

            // announce donation
            val charitableDonor = event.donorName ?: "mysterious donor"
            val numberValue = event.amount
            it.sendMessage("<grey><gradient:#A3ADFF:#00FFF4>DONATION MADE ––> Thank you,</gradient><#FF72A6> $charitableDonor<gradient:#00FFF4:#00FFF4>, <gradient:#00FFF4:#A3ADFF>for donating $$numberValue.</gradient>".style())
        }

        async {
            event.donorName?.let {
                Bukkit.getOfflinePlayer(it).let { donors.add(it.uniqueId) }
            }
        }

        currentGame?.handleDonation(DonationTier.getTier(value))
    }

    /**
     * Updates the donation bar with the current total donations and donation goal.
     */
    fun updateDonationBar() {
        donationBossBar.name("<colour:#1DF1BC>ᴅᴏɴᴀᴛɪᴏɴ ɢᴏᴀʟ: <white>$<green>${totalDonations}/${donationGoal}".style())
        var progress = (totalDonations.toFloat() / donationGoal)
        donationBossBar.progress(progress)
    }
}