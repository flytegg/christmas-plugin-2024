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
        player.inventory.storageContents = arrayOf()
        player.inventory.setItemInOffHand(null)

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
        eventController().serialisePoints()
        eventController().songPlayer?.isPlaying = true
        WorldNPC.refreshPodium()
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
        if (state == GameState.LIVE && gameConfig.eliminateOnLeave) eliminate(player, EliminationReason.EXPIRED_SESSION)
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
    fun remainingPlayers(): List<Player> =
        Util.runAction(PlayerType.PARTICIPANT) {}.filter { !(eliminatedPlayers.contains(it.uniqueId)) }

    /**
     * Renders a cinematic sequence of the game results with the temporary podium NPCs.
     */
    fun showGameResults() {
        // slide to post-game podium.
        CameraSlide(MapSinglePoint(519, 131, 559, 0, 0)) {
            // create podium NPCs
            val npcs = mutableListOf<WorldNPC>()
            val displays = mutableListOf<TextDisplay>()
            val descendingColour = listOf("a", "c", "9")
            formattedWinners.entries.take(3).reversed().forEachIndexed { index, keyValuePair ->
                val uniqueId = keyValuePair.key
                val value = keyValuePair.value
                val displayName = "§${descendingColour[index]}${Bukkit.getPlayer(uniqueId)!!.name}"
                val placeLocation = Util.getNPCSummaryLocation(index)
                val animationTasks = mutableListOf<TwilightRunnable>()

                WorldNPC.createFromUniqueId(displayName, uniqueId, placeLocation).also { npc ->
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
                MapSinglePoint(519, 131, 559, -144.63538F, -76.47707F),
                MapSinglePoint(519, 144, 559, -144.63538F, -76.47707F),
                MapSinglePoint(519, 162, 559, -136.78506F, -68.62731F),
                MapSinglePoint(519, 184, 559, -106.76083F, -49.52825F),
                MapSinglePoint(519, 201, 559, -92.1124F, -32.856964F),
                MapSinglePoint(523, 207, 559, -95.1069F, -10.844507F),
                MapSinglePoint(527, 212, 559, -101.17685F, 14.971579F),
                MapSinglePoint(529, 213, 559, -121.2471F, 26.220592F),
                MapSinglePoint(532, 213, 562, -165.83867F, 25.330376F),
                MapSinglePoint(537, 213, 558, 103.11706F, 24.278307F),
                MapSinglePoint(538, 209, 552, 51.32303F, 20.07004F),
                MapSinglePoint(529, 199, 540, 45.658024F, 16.023605F),
                MapSinglePoint(521, 209, 539, 37.32238F, 14.971543F),
                MapSinglePoint(517, 214, 539, 6.1650276F, 26.867983F),
                MapSinglePoint(512, 214, 540, -64.32342F, 30.26696F),
                MapSinglePoint(509, 211, 546, -112.96126F, 14.485972F),
                MapSinglePoint(512, 212, 552, -130.92738F, 12.300918F),
                MapSinglePoint(517, 217, 555, -134.73099F, 14.243189F),
                MapSinglePoint(521, 217, 552, -132.38396F, 10.520491F),
                MapSinglePoint(523, 217, 551, -134.4072F, 13.676693F),
                MapSinglePoint(523, 217.5, 551, -133.59787F, 18.45147F),
                MapSinglePoint(515, 217, 558, -135.37837F, 8.416364F),
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
         * 2. The player re-joined the server while a game was in progess **AND** [GameConfig.eliminateOnLeave] is `true`.
         */
        EXPIRED_SESSION,

        /**
         * Player was terrible and was eliminated by game logic.
         */
        ELIMINATED,
    }
}
