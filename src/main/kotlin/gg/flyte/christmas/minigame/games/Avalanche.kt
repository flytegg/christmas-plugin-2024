package gg.flyte.christmas.minigame.games

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.engine.EventMiniGame
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.engine.PlayerType
import gg.flyte.christmas.minigame.world.MapRegion
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.npc.WorldNPC
import gg.flyte.christmas.util.*
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.hidePlayer
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.extension.showPlayer
import gg.flyte.twilight.scheduler.TwilightRunnable
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.*
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import kotlin.math.ceil
import kotlin.random.Random

@Suppress("DuplicatedCode") // nature of this game is very similar to other music-controlled games.
class Avalanche : EventMiniGame(GameConfig.AVALANCHE) {
    private lateinit var overviewTask: TwilightRunnable

    private val floorRegion = MapRegion(MapSinglePoint(600, 110, 784), MapSinglePoint(632, 110, 816))
    private var roundNumber = 0
    private var secondsForRound = 9
    private var currentBossBar: BossBar? = null
    private var bossBarTask: TwilightRunnable? = null
    private var gameLogicTask: TwilightRunnable? = null
    private var isCountdownActive = false
    private var harder = false
    private var safePointsByRound = mapOf(
        1 to 4,
        2 to 4,
        3 to 4,
        4 to 4,
        5 to 4,
        6 to 3,
        7 to 3,
        8 to 2,
        9 to 2,
        10 to 1
    )
    private val safePoints = mutableListOf<Location>()

    override fun startGameOverview() {
        super.startGameOverview()

        ChristmasEventPlugin.instance.serverWorld.time = 12750

        floorRegion.toSingleBlockLocations().forEach { it.block.type = Material.BLUE_ICE }

        overviewTask = repeatingTask(10) {
            removeSafePoints()

            repeat(100) {
                ChristmasEventPlugin.instance.serverWorld.spawn(
                    floorRegion.randomLocation().clone().add(0.0, (25..30).random().toDouble(), 0.0),
                    Snowball::class.java
                )
            }

            floorRegion.randomLocation().apply {
                add(0.0, 3.0, 0.0)
                block.type = Material.CUT_COPPER
                safePoints += block.location
                block.world.spawnParticle(Particle.FLASH, block.location.toCenterLocation(), 1000)
            }
        }
    }

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())
    }

    override fun startGame() {
        overviewTask.cancel()
        ChristmasEventPlugin.instance.serverWorld.entities.forEach { if (it is Snowball) it.remove() }
        simpleCountdown { newRound() }
    }

    private fun newRound() {
        roundNumber++
        if (secondsForRound > 2) secondsForRound--

        if (roundNumber == 10) {
            harder = true

            Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL) }
            Util.runAction(PlayerType.PARTICIPANT) {
                it.title(
                    "<game_colour>Hard Mode!".style(), "<bold><red>PVP <game_colour>is now enabled!".style(),
                    titleTimes(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(300))
                )
            }
            Util.runAction(PlayerType.OPTED_OUT) { it.sendMessage("<game_colour>The game is getting harder!".style()) }
        }

        if (roundNumber % 3 == 0) {
            eventController().startPlaylist(SongReference.ALL_I_WANT_FOR_CHRISTMAS_IS_YOU) // beginning makes it hard to differentiate when it has stopped.
        } else {
            eventController().songPlayer?.isPlaying = true
        }

        removeSafePoints()
        val delayBeforeSpawnRoof = (6..10).random()
        tasks += delay(delayBeforeSpawnRoof, TimeUnit.SECONDS) {
            if (!isCountdownActive) { // prevent double-prepare due to random condition
                prepareAvalanche()
            }
        }

    }

    private fun prepareAvalanche() {
        isCountdownActive = true

        eventController().songPlayer?.isPlaying = false

        val timerBar: BossBar = BossBar.bossBar(
            "<game_colour><b>Avalanche In: $secondsForRound".style(), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS
        ).also { currentBossBar = it }

        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.showBossBar(timerBar) }

        repeat(safePointsByRound[roundNumber] ?: 1) {
            floorRegion.randomLocation().apply {
                add(0.0, 3.0, 0.0)
                block.type = Material.CUT_COPPER
                safePoints += block.location
                block.world.spawnParticle(Particle.FLASH, block.location.toCenterLocation(), 1000)
            }
        }

        val totalTicks = secondsForRound * 20
        var remainingTicks = totalTicks

        // game logic timer
        gameLogicTask = repeatingTask(5, 5) {
            if (remainingTicks <= 0) {
                this.cancel()
                gameLogicTask = null

                // spawn da snowballs from da sky
                var maxPoint = floorRegion.maxPoint
                var minPoint = floorRegion.minPoint
                var slightlyExpandedRegion = MapRegion(
                    MapSinglePoint(minPoint.x.toInt() - 3, minPoint.y, minPoint.z.toInt() - 3),
                    MapSinglePoint(maxPoint.x.toInt() + 3, maxPoint.y, maxPoint.z.toInt() + 3)
                )

                repeat(1200) {
                    ChristmasEventPlugin.instance.serverWorld.spawn(
                        slightlyExpandedRegion.randomLocation().clone().add(0.0, (25..30).random().toDouble(), 0.0),
                        Snowball::class.java
                    ) {
                        it.velocity = it.velocity.setX(0).setZ(0)
                    }
                }

                isCountdownActive = false

                tasks += delay(80) { newRound() }
            } else {
                remainingPlayers().forEach { it.playSound(Sound.BLOCK_NOTE_BLOCK_BASEDRUM) }
            }
        }

        // BossBar ticker
        bossBarTask = repeatingTask(1, 1) {
            if (remainingTicks <= 0) {
                this.cancel()

                Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.hideBossBar(timerBar) }
                bossBarTask = null
                currentBossBar = null
            } else {
                val progress = remainingTicks.toDouble() / totalTicks
                timerBar.progress(progress.toFloat())

                val secondsRemaining = ceil(remainingTicks / 20.0).toInt()
                timerBar.name("<game_colour><b>Time left: $secondsRemaining".style())
                remainingTicks--
            }
        }

        tasks += bossBarTask
        tasks += gameLogicTask
    }

    override fun eliminate(player: Player, reason: EliminationReason) {
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
            it.sendMessage("<red>${player.name} <grey>has been eliminated!".style())
            it.playSound(Sound.ENTITY_PLAYER_HURT)
        }

        if (reason == EliminationReason.ELIMINATED) {
            player.apply {
                currentBossBar?.let { hideBossBar(it)}
                player.world.spawnParticle(Particle.BLOCK, player.location, 100, 0.5, 0.5, 0.5, Bukkit.createBlockData(Material.SNOW_BLOCK))

                val itemDisplay = world.spawn(location, ItemDisplay::class.java) {
                    it.setItemStack(ItemStack(Material.AIR))
                    it.teleportDuration = 59 // max (minecraft limitation)
                }

                addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1, false, false, false))

                delay(1) {
                    val randomSpecLocation = gameConfig.spectatorSpawnLocations.random()
                    itemDisplay.teleport(randomSpecLocation)
                    itemDisplay.addPassenger(player)
                    player.hidePlayer()

                    delay(59) {
                        itemDisplay.remove()
                        player.teleport(randomSpecLocation)
                        player.showPlayer()
                    }
                }
            }
        }

        super.eliminate(player, reason)

        val value = "$roundNumber Round${if (roundNumber > 1) "s" else ""}"
        when (remainingPlayers().size) {
            1 -> {
                formattedWinners.put(player.uniqueId, value)
                formattedWinners.put(remainingPlayers().first().uniqueId, "$value (1st Place!)")
                endGame()
            }

            2 -> formattedWinners.put(player.uniqueId, value)
        }
    }

    override fun endGame() {
        tasks.forEach { it?.cancel() }
        removeSafePoints()
        ChristmasEventPlugin.instance.serverWorld.time = 6000

        Util.runAction(
            PlayerType.PARTICIPANT,
            PlayerType.OPTED_OUT
        ) { it.hideBossBar(if (currentBossBar != null) currentBossBar!! else return@runAction) }

        val winnerNPCs = mutableListOf<WorldNPC>()
        val winnerPlayer = remainingPlayers().first()
        eventController().addPoints(winnerPlayer.uniqueId, 15)

        tasks += repeatingTask(10) {
            repeat(10) {
                ChristmasEventPlugin.instance.serverWorld.spawn(
                    floorRegion.randomLocation().clone().add(0.0, (25..30).random().toDouble(), 0.0),
                    Snowball::class.java
                ) { snowBall ->
                    var randomColour: String = listOf("4", "c", "6", "2", "a", "9").random()
                    val displayName: String = "§$randomColour${winnerPlayer.name}".colourise()

                    WorldNPC.createFromLive(displayName, winnerPlayer, snowBall.location).also {
                        winnerNPCs.add(it)
                        it.spawnForAll()
                        var passengerPacket = WrapperPlayServerSetPassengers(snowBall.entityId, intArrayOf(it.npc.id))

                        var yaw = (0..360).random().toFloat()
                        var lookPacket = WrapperPlayServerEntityHeadLook(it.npc.id, yaw)
                        var rotationPacket = WrapperPlayServerEntityRotation(it.npc.id, yaw, 0F, true)

                        delay(1) {
                            Bukkit.getOnlinePlayers().forEach {
                                passengerPacket.sendPacket(it)
                                lookPacket.sendPacket(it)
                                rotationPacket.sendPacket(it)
                            }
                        }
                    }
                }
            }
        }

        delay(15, TimeUnit.SECONDS) {
            winnerNPCs.forEach { it.despawnForAll() }
            floorRegion.toSingleBlockLocations().forEach { it.block.type = Material.AIR }
            super.endGame()
        }
    }

    private fun removeSafePoints() {
        safePoints.forEach {
            it.block.type = Material.AIR
            it.world.spawnParticle(Particle.BLOCK, it.toCenterLocation(), 200, 0.5, 0.5, 0.5, Bukkit.createBlockData(Material.WAXED_CUT_COPPER))
        }.also { safePoints.clear() }
    }

    override fun handleGameEvents() {
        listeners += event<EntityDamageEvent>(priority = EventPriority.HIGHEST) {
            // return@event -> already cancelled by lower priority [HousekeepingEventListener]

            entity as? Player ?: return@event
            if ((this as? EntityDamageByEntityEvent)?.damager !is Player) {
                isCancelled = true
                return@event
            }

            if (harder) isCancelled = false
            damage = 0.0
        }

        listeners += event<ProjectileHitEvent> {
            hitEntity as? Player ?: return@event
            if (remainingPlayers().contains(hitEntity as Player) && Random.nextBoolean()) { // 50% chance of elimination
                eliminate(hitEntity as Player, EliminationReason.ELIMINATED)
            }
        }

        listeners += event<PlayerMoveEvent> {
            if (player.location.blockY < 104) {
                if (remainingPlayers().contains(player)) eliminate(player, EliminationReason.ELIMINATED)
            }
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> TODO()
            DonationTier.MEDIUM -> TODO()
            DonationTier.HIGH -> TODO()
        }
    }
}