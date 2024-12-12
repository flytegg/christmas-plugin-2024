package gg.flyte.christmas.minigame.engine

import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.npc.WorldNPC
import gg.flyte.christmas.util.*
import gg.flyte.christmas.visual.CameraSequence
import gg.flyte.christmas.visual.CameraSlide
import gg.flyte.twilight.event.TwilightListener
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.TwilightRunnable
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.util.*
import kotlin.random.Random

/**
 * Abstract class representing a base for mini-games in the event.
 * This class provides the core game logic and management of players,
 * such as handling elimination, preparing players, and tracking game state.
 *
 * @param gameConfig Configuration for the mini-game, which includes settings like spawn points,
 * game name, spectator camera locations, and instructions for players.
 */
abstract class EventMiniGame(val gameConfig: GameConfig) {
    protected val eliminatedPlayers = mutableListOf<UUID>()
    protected val listeners = mutableListOf<TwilightListener>()
    protected val tasks = mutableListOf<TwilightRunnable?>()
    protected val formattedWinners = linkedMapOf<UUID, String>()
    var donationEventsEnabled = false
    val spectateEntities = mutableMapOf<Int, Entity>()
    var state: GameState = GameState.IDLE

    /**
     * Initialises the game's spectator entities, which are used to allow players to spectate the game.
     */
    init {
        for ((index, point) in gameConfig.spectatorCameraLocations.withIndex()) {
            spectateEntities[index] = ChristmasEventPlugin.instance.serverWorld.spawn(point, ItemDisplay::class.java) {
                it.setItemStack(ItemStack(Material.AIR))
            }
        }
    }

    /**
     * Starts the game's map overview. This is done through smoothened camera
     * interpolation from [CameraSequence]. As well as sending instructions ([GameConfig.instructions]),
     * and preparing the player for the game.
     *
     * **Note:** Implementing classes may override (but **must** call super) to add additional cosmetic
     * functionality as the CameraSequence is running.
     */
    open fun startGameOverview() {
        Bukkit.getOnlinePlayers().forEach {
            it.hideBossBar(eventController().donationBossBar)
            eventController().sidebarManager.remove(it) // hide for now
        }
        CameraSlide(gameConfig) {
            // send BEFORE textDisplay has rendered in.
            Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
                it.title(
                    gameConfig.displayName, "<game_colour>Instructions:".style(),
                    titleTimes(Duration.ofMillis(1250), Duration.ofMillis(3500), Duration.ofMillis(750))
                )
            }

            val displayComponent = Component.empty()
                .append("<st>\n   ".style())
                .append("> ".style())
                .append("<b><0>".style(gameConfig.displayName))
                .append("\n\n".style())
                .append(gameConfig.instructions.style())
                .append("\n".style())
                .color(gameConfig.colour)

            CameraSequence(gameConfig.overviewLocations, Bukkit.getOnlinePlayers(), displayComponent) {
                // when sequence finished:
                handleGameEvents()

                Util.runAction(PlayerType.PARTICIPANT) {
                    // if player was eliminated during the sequence (left server), don't prepare them.
                    if (!(remainingPlayers().map { it.uniqueId }.contains(it.uniqueId))) return@runAction

                    preparePlayer(it)
                    it.sendMessage(
                        "<game_colour>\n------------------[INSTRUCTIONS]------------------\n".style()
                            .append("<white>${gameConfig.instructions}".style())
                            .append("<game_colour>\n-------------------------------------------------\n".style())
                    )
                }
                Util.runAction(PlayerType.OPTED_OUT) { it.teleport(gameConfig.spectatorSpawnLocations.random()) }

                eventController().sidebarManager.update() // enable again after sequence ended.
                startGame()
                state = GameState.LIVE
            }
        }
    }

    /**
     * Prepares the player for the game; state, inventory, location, and other relevant configurations.
     *
     * @param player The player to be prepared for the game.
     */
    abstract fun preparePlayer(player: Player)

    /**
     * Called when a game is ready to start.
     * Exclusive of the countdown or any pre-game setup.
     */
    abstract fun startGame()

    /**
     * As the super-class, this only handles game logic for player elimination.
     * Subclasses should override for cosmetic functionality
     * and call `super.eliminate()` to ensure engine logic is maintained.
     * @param player The player to eliminate.
     * @param reason The reason for elimination.
     */
    open fun eliminate(player: Player, reason: EliminationReason) {
        eliminatedPlayers.add(player.uniqueId)
        player.teleport(gameConfig.spectatorSpawnLocations.random())
        player.clearActivePotionEffects()
        player.formatInventory()

        if (reason == EliminationReason.EXPIRED_SESSION) {
            player.teleport(gameConfig.spectatorSpawnLocations.random())
        } else {
            // spectate item
            ItemStack(Material.RECOVERY_COMPASS).apply {
                itemMeta = itemMeta.apply {
                    displayName("<!i><white>ѕᴘᴇᴄᴛᴀᴛᴇ".style())
                    editMeta {
                        lore(listOf("<grey>ᴄʟɪᴄᴋ ᴛᴏ ѕᴘᴇᴄᴛᴀᴛᴇ!".style()))
                    }
                }
            }.also { player.inventory.setItem(8, it) }
        }
    }

    /**
     * Ends the game, performing cleanup operations such as cancelling tasks,
     * unregistering listeners, and removing spectator entities. Can be
     * overridden by subclasses for custom end-game behavior.
     */
    open fun endGame() {
        tasks.forEach { it?.cancel() }.also { tasks.clear() }
        listeners.forEach { it.unregister() }.also { listeners.clear() }
        spectateEntities.values.forEach { it.remove() }.also { spectateEntities.clear() }
        eliminatedPlayers.clear()
        eventController().currentGame = null
        eventController().sidebarManager.dataSupplier = eventController().points
        eventController().songPlayer?.isPlaying = true
        eventController().serialisePoints()
        showGameResults()
    }

    /**
     * Handles game events
     *
     * **Note**: Registered events in subclasses (preferably through Twilight) should be added to [listeners]
     *
     * ```
     *  override fun handleGameEvents() {
     *    listeners += event<BlockPlaceEvent> {...}
     *  }
     *  ```
     */
    abstract fun handleGameEvents()

    /**
     * Runs a particular action depending on a donation tier
     * @param tier The donation tier to handle
     * @see [DonationTier]
     */
    open fun handleDonation(tier: DonationTier) {}

    /**
     * Handles the provided player reference (directly from [org.bukkit.event.player.PlayerJoinEvent]) when they join **this** game.
     *
     * **Note**: If implementing classes wish for joining players to participate in the game (even if it has already started),
     * they should set its [GameConfig.eliminateOnLeave] to `false`. If `false` [preparePlayer] will be called to set up the player
     * to integrate into the game.
     *
     * @param player The player who has joined the game.
     */
    fun onPlayerJoin(player: Player) {
        if (eliminatedPlayers.contains(player.uniqueId)) return  // game logic has already handled their elimination.
        else preparePlayer(player)
    }

    /**
     * Handles the provided player reference (directly from [org.bukkit.event.player.PlayerQuitEvent]) when they leave **this** game.
     *
     * The player will internally be marked eliminated if the current game's configuration specifies that players should be eliminated
     * @see [GameConfig.eliminateOnLeave]
     */
    fun onPlayerQuit(player: Player) {
        if (state == GameState.OVERVIEWING) {
            player.teleport(gameConfig.spectatorSpawnLocations.random()) // left while overviewing, temporarily hold them away from playspace.
        } else if (state == GameState.LIVE) {
            if (gameConfig.eliminateOnLeave) eliminate(player, EliminationReason.EXPIRED_SESSION)
        }
    }

    /**
     * Starts a simple countdown before the game begins.
     * Displays countdown messages to all players and triggers [onCountdownEnd] when the countdown finishes.
     *
     * @param onCountdownEnd The action to perform when the countdown ends.
     */
    fun simpleCountdown(onCountdownEnd: () -> Unit) {
        var seconds = 5
        repeatingTask(1, TimeUnit.SECONDS) {
            when (seconds) {
                0 -> {
                    cancel()
                    onCountdownEnd()
                }

                else -> {
                    val times = titleTimes(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)

                    Util.runAction(PlayerType.PARTICIPANT) {
                        eventController().countdownMap[seconds]?.let { titleText ->
                            it.title(titleText, Component.empty(), times)
                            it.playSound(Sound.UI_BUTTON_CLICK)
                        }
                    }
                    seconds--
                }
            }
        }
    }

    /**
     * @return A list of players who have not been eliminated from the game.
     */
    fun remainingPlayers(): List<Player> = Util.runAction(PlayerType.PARTICIPANT) {}.filter { !(eliminatedPlayers.contains(it.uniqueId)) }

    /**
     * Renders a cinematic sequence of the game results with the temporary podium NPCs.
     */
    fun showGameResults() {
        // slide to post-game podium.
        Bukkit.getOnlinePlayers().forEach {
            it.hideBossBar(eventController().donationBossBar)
            eventController().sidebarManager.remove(it) // hide for now
        }

        CameraSlide(MapSinglePoint(527, 202, 562, 20.373718F, 7.030075F)) {
            // create podium NPCs
            val npcs = mutableListOf<WorldNPC>()
            val displays = mutableListOf<TextDisplay>()
            val descendingColour = listOf(
                "<colour:#ffcb1a>➊ ",
                "<colour:#d0d0d0>➋ ",
                "<colour:#a39341>➌ "
            )

            formattedWinners.entries.take(3).reversed().forEachIndexed { index, keyValuePair ->
                val uniqueId = keyValuePair.key
                val value = keyValuePair.value
                val displayName = "${descendingColour[index]}${Bukkit.getOfflinePlayer(uniqueId).name}"
                val placeLocation = Util.getNPCSummaryLocation(index)
                val animationTasks = mutableListOf<TwilightRunnable>()

                WorldNPC.createFromUniqueId(displayName.style(), uniqueId, placeLocation).also { npc ->
                    npc.spawnForAll()

                    placeLocation.world.spawn(placeLocation.add(0.0, 2.5, 0.0), TextDisplay::class.java).apply {
                        text("<colour:#ffc4ff>$value".style())
                        backgroundColor = Color.fromRGB(84, 72, 84)
                        billboard = Display.Billboard.CENTER

                        displays.add(this)
                    }

                    animationTasks += repeatingTask(1) {
                        val nearestItem = placeLocation.getNearbyEntitiesByType(Player::class.java, 200.0).firstOrNull()
                        if (nearestItem == null) {
                            cancel()
                            return@repeatingTask
                        }

                        val playersLocation = nearestItem.location.clone().subtract(0.0, 1.0, 0.0)

                        val npcLocation = npc.npc.location.bukkit()
                        if (npcLocation.distance(playersLocation) <= 25) {
                            val lookVector = npcLocation.apply { setDirection(playersLocation.toVector().subtract(toVector())) }

                            Bukkit.getOnlinePlayers().forEach {
                                WrapperPlayServerEntityHeadLook(npc.npc.id, lookVector.yaw).sendPacket(it)
                                WrapperPlayServerEntityRotation(npc.npc.id, lookVector.yaw, lookVector.pitch, false).sendPacket(it)
                            }
                        }
                    } // NPC Look
                    animationTasks += repeatingTask(3) {
                        delay((0..5).random()) {
                            val packetToSend = when (Random.nextBoolean()) {
                                true -> {
                                    WrapperPlayServerEntityAnimation(npc.npc.id, WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM)
                                }

                                false -> {
                                    WrapperPlayServerEntityMetadata(
                                        npc.npc.id, listOf(
                                            EntityData(
                                                6,
                                                EntityDataTypes.ENTITY_POSE,
                                                (if (Random.nextBoolean()) EntityPose.CROUCHING else EntityPose.STANDING)
                                            )
                                        )
                                    )
                                }
                            }

                            Bukkit.getOnlinePlayers().forEach { packetToSend.sendPacket(it) }
                        }
                    } // NPC Animation

                    npcs.add(npc)
                }
            } // Spawn NPCs
            formattedWinners.clear()

            // cinematic camera sequence
            val summaryLocations = listOf(
                MapSinglePoint(527, 202, 562, 20.373718F, 7.030075F),
                MapSinglePoint(528, 205, 559, -48.90033F, 4.0357285F),
                MapSinglePoint(528, 208, 558, -65.81525F, 5.9780083F),
                MapSinglePoint(529, 211, 557, -65.32965F, 8.891423F),
                MapSinglePoint(530, 213, 555, -44.6109F, 24.834274F),
                MapSinglePoint(532, 214, 553, -7.1394653F, 33.81733F),
                MapSinglePoint(536, 211, 553, 27.337158F, 17.146109F),
                MapSinglePoint(530, 200, 547, 101.06506F, 18.76465F),
                MapSinglePoint(517, 215, 546, 168.72296F, 22.16365F),
                MapSinglePoint(517, 215, 546, -168.53601F, 26.77655F),
                MapSinglePoint(517, 215, 549, -159.95715F, 28.071411F),
                MapSinglePoint(517, 215, 551, -138.59094F, 27.42398F),
                MapSinglePoint(522, 215, 551, -134.54431F, 28.88068F),
                MapSinglePoint(523, 219, 550, -134.62524F, 48.384426F),
                MapSinglePoint(523, 219, 550, -134.22058F, 47.170494F),
                MapSinglePoint(521, 217, 552, -132.44006F, 30.58016F),
                MapSinglePoint(519, 214, 554, -132.2782F, 8.243982F),
            )

            CameraSequence(summaryLocations, Bukkit.getOnlinePlayers(), null, 8) {
                Bukkit.getOnlinePlayers().forEach { loopedPlayer ->
                    loopedPlayer.gameMode = GameMode.ADVENTURE
                    loopedPlayer.formatInventory()
                    loopedPlayer.teleport(ChristmasEventPlugin.instance.lobbySpawn)
                    loopedPlayer.clearActivePotionEffects()
                    loopedPlayer.showBossBar(eventController().donationBossBar)
                    npcs.forEach { it.despawnFor(loopedPlayer) }
                    displays.forEach { it.remove() }
                }

                WorldNPC.refreshPodium()
                eventController().sidebarManager.update()
            }
        }
    }

    /**
     * Used to distinguish between different reasons for elimination.
     *
     * Cosmetic effects can only be applied if the eliminated player is still on the server.
     */
    enum class EliminationReason {
        /**
         * The player's session is considered to be 'expired' and is subsequently eliminated.
         *
         * This can be because:
         * 1. The player disconnected from the server during an active game.
         * 2. The player joined the server while a game was in progress **AND** [GameConfig.eliminateOnLeave] is `true`.
         */
        EXPIRED_SESSION,

        /**
         * Player was terrible and was eliminated by game logic.
         */
        ELIMINATED,
    }
}
