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
import org.bukkit.attribute.Attribute
import org.bukkit.entity.BlockDisplay
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
import org.bukkit.util.Vector
import java.time.Duration
import kotlin.math.ceil
import kotlin.random.Random

@Suppress("DuplicatedCode") // nature of this game is very similar to other music-controlled games.
class Avalanche : EventMiniGame(GameConfig.AVALANCHE) {
    private lateinit var overviewTask: TwilightRunnable

    private val floorRegion = MapRegion(MapSinglePoint(600, 110, 784), MapSinglePoint(632, 110, 816))
    private var started = false
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
    private val phantomSafePoints = mutableListOf<BlockDisplay>()

    // ticks left | total ticks
    private var whirlwindTickData: Pair<Int, Int> = 0 to 0
    private var whirlwindTask: TwilightRunnable? = null

    // lateinit since <game_colour> is not mapped yet at time of init
    private lateinit var whirlwindBossBar: BossBar

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
        player.getAttribute(Attribute.SCALE)?.baseValue = 0.85
    }

    override fun startGame() {
        overviewTask.cancel()
        ChristmasEventPlugin.instance.serverWorld.entities.forEach { if (it is Snowball) it.remove() }
        simpleCountdown {
            newRound()
            started = true
            donationEventsEnabled = true
            whirlwindBossBar = BossBar.bossBar("<game_colour><b>ᴡɪɴᴛᴇʀ ᴡʜɪʀʟᴡɪɴᴅ!".style(), 1.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
        }
    }

    private fun newRound() {
        roundNumber++
        if (secondsForRound > 2) secondsForRound--

        if (roundNumber == 10) {
            harder = true

            Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL) }
            Util.runAction(PlayerType.PARTICIPANT) {
                it.title(
                    "<game_colour>ʜᴀʀᴅ ᴍᴏᴅᴇ!".style(), "<bold><red>ᴘᴠᴘ <game_colour>ɪѕ ɴᴏᴡ ᴇɴᴀʙʟᴇᴅ!".style(),
                    titleTimes(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(300))
                )
            }
            Util.runAction(PlayerType.OPTED_OUT) { it.sendMessage("<game_colour>ᴛʜᴇ ɢᴀᴍᴇ ɪѕ ɢᴇᴛᴛɪɴɢ ʜᴀʀᴅᴇʀ!".style()) }
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
            "<game_colour><b>ᴀᴠᴀʟᴀɴᴄʜᴇ ɪɴ: $secondsForRound".style(), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS
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
                val maxPoint = floorRegion.maxPoint
                val minPoint = floorRegion.minPoint
                val slightlyExpandedRegion = MapRegion(
                    MapSinglePoint(minPoint.x.toInt() - 3, minPoint.y, minPoint.z.toInt() - 3),
                    MapSinglePoint(maxPoint.x.toInt() + 3, maxPoint.y, maxPoint.z.toInt() + 3)
                )
                phantomSafePoints.forEach { it.remove() } // remove phantom safe points lol

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
                timerBar.name("<game_colour><b>ᴛɪᴍᴇ ʟᴇꜰᴛ: $secondsRemaining".style())
                remainingTicks--
            }
        }

        tasks += bossBarTask
        tasks += gameLogicTask
    }

    override fun eliminate(player: Player, reason: EliminationReason) {
        if (!started) return
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
            it.sendMessage("<red>${player.name} <grey>ʜᴀѕ ʙᴇᴇɴ ᴇʟɪᴍɪɴᴀᴛᴇᴅ!".style())
            it.playSound(Sound.ENTITY_PLAYER_HURT)
        }

        currentBossBar?.let { player.hideBossBar(it) }
        player.world.spawnParticle(Particle.BLOCK, player.location, 100, 0.5, 0.5, 0.5, Bukkit.createBlockData(Material.SNOW_BLOCK))
        player.hideBossBar(whirlwindBossBar)
        player.getAttribute(Attribute.SCALE)?.baseValue = 1.0

        // animate death
        if (reason == EliminationReason.ELIMINATED) {
            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1, false, false, false))

            val itemDisplay = player.world.spawn(player.location, ItemDisplay::class.java) {
                it.setItemStack(ItemStack(Material.AIR))
                it.teleportDuration = 59 // max (minecraft limitation)
            }
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

        super.eliminate(player, reason)

        val value = "$roundNumber ʀᴏᴜɴᴅ${if (roundNumber > 1) "ѕ" else ""}"
        when (remainingPlayers().size) {
            1 -> {
                formattedWinners[player.uniqueId] = value
                formattedWinners[remainingPlayers().first().uniqueId] = "$value (1ѕᴛ ᴘʟᴀᴄᴇ!)"

                // formattedWinners currently have keys in order of elimination, reverse it to get actual winners.
                LinkedHashMap(formattedWinners.toList().asReversed().toMap()).apply {
                    formattedWinners.clear()
                    formattedWinners.putAll(this)
                }
                endGame()
            }

            2 -> formattedWinners[player.uniqueId] = value
        }
    }

    override fun endGame() {
        tasks.forEach { it?.cancel() }.also { tasks.clear() }
        donationEventsEnabled = false
        started = false
        removeSafePoints()
        ChristmasEventPlugin.instance.serverWorld.time = 6000

        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { viewer ->
            currentBossBar?.let { viewer.hideBossBar(it) }
            viewer.hideBossBar(whirlwindBossBar)
        }
        Util.runAction(PlayerType.PARTICIPANT) {
            it.getAttribute(Attribute.SCALE)!!.baseValue = 1.0
            it.formatInventory() // in-case pumpkin placed
        }

        val winnerNPCs = mutableListOf<WorldNPC>()
        val winnerPlayer = remainingPlayers().first()
        eventController().addPoints(winnerPlayer.uniqueId, 15)

        tasks += repeatingTask(10) {
            repeat(10) {
                ChristmasEventPlugin.instance.serverWorld.spawn(
                    floorRegion.randomLocation().clone().add(0.0, (25..30).random().toDouble(), 0.0),
                    Snowball::class.java
                ) { snowBall ->
                    val randomColour = listOf(
                        "<colour:#fcba03>",
                        "<colour:#b7ffab>",
                        "<colour:#0098b3>",
                        "<colour:#3d3dff>",
                        "<colour:#ebadff>",
                        "<colour:#ff3333>",
                        "<colour:#50e669>",
                        "<game_colour>"
                    ).random()
                    val displayName = "$randomColour${winnerPlayer.name}".style()

                    WorldNPC.createFromLive(displayName, winnerPlayer, snowBall.location).also {
                        winnerNPCs.add(it)
                        it.spawnForAll()
                        val passengerPacket = WrapperPlayServerSetPassengers(snowBall.entityId, intArrayOf(it.npc.id))

                        val yaw = (0..360).random().toFloat()
                        val lookPacket = WrapperPlayServerEntityHeadLook(it.npc.id, yaw)
                        val rotationPacket = WrapperPlayServerEntityRotation(it.npc.id, yaw, 0F, true)

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
        phantomSafePoints.forEach { it.remove() }.also { phantomSafePoints.clear() }
    }

    private fun whirlwindActive() = whirlwindTickData.first > 0

    override fun handleGameEvents() {
        listeners += event<EntityDamageEvent>(priority = EventPriority.HIGHEST) {
            // return@event -> already cancelled by lower priority [HousekeepingEventListener]

            if (entity !is Player) return@event
            if ((this as? EntityDamageByEntityEvent)?.damager !is Player) {
                isCancelled = true
                return@event
            }

            if (!(remainingPlayers().contains(damager))) return@event

            if (harder) isCancelled = false
            damage = 0.0
        }

        listeners += event<ProjectileHitEvent> {
            if (hitEntity !is Player) return@event

            if (remainingPlayers().contains(hitEntity as Player)) {
                var player = hitEntity as Player
                if (player.equipment.helmet.type == Material.DIAMOND_HELMET) {
                    player.formatInventory()
                    player.sendMessage("<game_colour>ʏᴏᴜʀ <game_colour>ʜᴀʀᴅ-ʜᴀᴛ <grey>ᴘʀᴏᴛᴇᴄᴛᴇᴅ ʏᴏᴜ!".style())
                    player.playSound(Sound.ITEM_TOTEM_USE)
                    return@event
                }

                eliminate(player, EliminationReason.ELIMINATED)
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
            DonationTier.LOW -> {
                remainingPlayers().random().apply {
                    equipment.helmet = ItemStack(Material.DIAMOND_HELMET).apply {
                        itemMeta = itemMeta.apply {
                            displayName("<game_colour>ʜᴀʀᴅ-ʜᴀᴛ".style())
                            lore(
                                listOf(
                                    "<grey>ᴛʜɪѕ ʜᴀᴛ ᴡɪʟʟ ᴘʀᴏᴛᴇᴄᴛ ʏᴏᴜ ᴡɪᴛʜ".style(),
                                    "<grey>ɪᴛ'ѕ ᴅɪᴀᴍᴏɴᴅʏ ᴘᴏᴡᴇʀ! ɴᴏ ɴᴇᴇᴅ".style(),
                                    "<grey>ᴛᴏ sᴛᴀɴᴅ ᴜɴᴅᴇʀ ᴀ sᴀꜰᴇ-ᴘᴏɪɴᴛ".style()
                                )
                            )
                        }
                    }

                    delay(20) {
                        title(
                            "<game_colour>ʜᴀʀᴅ-ʜᴀᴛ ᴇqᴜɪᴘᴘᴇᴅ!".style(), "<grey>ʏᴏᴜ ᴅᴏ ɴᴏᴛ ɴᴇᴇᴅ ᴄᴏᴠᴇʀ ᴛʜɪs ʀᴏᴜɴᴅ!".style(),
                            titleTimes(Duration.ZERO, Duration.ofSeconds(4), Duration.ZERO)
                        )
                    }
                    sendMessage("<game_colour>ʏᴏᴜ ʜᴀᴠᴇ ʙᴇᴇɴ ɢɪᴠᴇɴ ᴀ <game_colour>ʜᴀʀᴅ-ʜᴀᴛ".style())
                }

                val message =
                    "<game_colour>sᴏᴍᴇᴏɴᴇ ʜᴀs ʙᴇᴇɴ ɢɪᴠᴇɴ ᴀ ᴘʀᴏᴛᴇᴄᴛɪᴠᴇ ʜᴇʟᴍᴇᴛ (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                announceDonationEvent(message.style())
            }

            DonationTier.MEDIUM -> {
                if (Random.nextBoolean()) {
                    ChristmasEventPlugin.instance.serverWorld.spawn(
                        floorRegion.randomLocation().clone().toBlockLocation().add(0.0, 3.0, 0.0),
                        BlockDisplay::class.java
                    ) {
                        it.block = Bukkit.createBlockData(Material.CUT_COPPER)
                        phantomSafePoints.add(it)
                    }

                    val message =
                        "<game_colour>ᴀ ᴘʜᴀɴᴛᴏᴍ sᴀꜰᴇ-ᴘᴏɪɴᴛ ʜᴀs ʙᴇᴇɴ ᴘʟᴀᴄᴇᴅ... ʙᴇ ᴄᴀʀᴇꜰᴜʟ﹗ (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())
                } else {
                    if (whirlwindTask == null) {
                        whirlwindTask = repeatingTask(25) {
                            remainingPlayers().forEach {
                                it.velocity = Vector(
                                    Random.nextDouble(-1.0, 1.0),
                                    Random.nextDouble(-0.3, 0.5),
                                    Random.nextDouble(-1.0, 1.0)
                                )
                                it.playSound(Sound.ENTITY_BREEZE_IDLE_AIR)
                            }
                        }.also { tasks += it }
                    }

                    remainingPlayers().forEach { it.showBossBar(whirlwindBossBar) }

                    if (whirlwindActive()) {
                        // extend duration if already active
                        whirlwindTickData = whirlwindTickData.let { it.first + (5 * 20) to it.second + (5 * 20) }
                    } else {
                        // set initial duration
                        whirlwindTickData = 5 * 20 to 5 * 20

                        tasks += repeatingTask(1) {
                            val (ticksLeft, totalTicks) = whirlwindTickData
                            whirlwindBossBar.progress(Math.clamp(ticksLeft / totalTicks.toFloat(), 0.0F, 1.0F))

                            if (ticksLeft == 0) {
                                whirlwindTask?.cancel()
                                whirlwindTask = null
                                this.cancel()
                                whirlwindTickData = 0 to 0
                                remainingPlayers().forEach {
                                    it.sendMessage("<game_colour>ᴡɪɴᴛᴇʀ ᴡʜɪʀʟᴡɪɴᴅ ʜᴀѕ ᴇɴᴅᴇᴅ!".style())
                                    it.hideBossBar(whirlwindBossBar)
                                }
                            } else {
                                whirlwindTickData = ticksLeft - 1 to totalTicks
                            }
                        }
                    }

                    val message =
                        "<game_colour>ᴛʜᴇ ᴡɪɴᴛᴇʀ ᴡʜɪʀʟᴡɪɴᴅ ɪs ᴜᴘᴏɴ ᴜs... ᴅᴏɴ'ᴛ ɢᴇᴛ ʙʟᴏᴡɴ ᴀᴡᴀʏ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())
                }
            }

            DonationTier.HIGH -> {
                if (!harder) {
                    val message =
                        "<game_colour>ʜᴀʀᴅ-ᴍᴏᴅᴇ ʜᴀѕ ʙᴇᴇɴ ᴀᴄᴛɪᴠᴀᴛᴇᴅ <b>ᴇᴀʀʟʏ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())

                    delay(50) {
                        harder = true
                        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL) }
                        Util.runAction(PlayerType.PARTICIPANT) {
                            it.title(
                                "<game_colour>ʜᴀʀᴅ ᴍᴏᴅᴇ!".style(), "<bold><red>ᴘᴠᴘ <game_colour>ɪѕ ɴᴏᴡ ᴇɴᴀʙʟᴇᴅ!".style(),
                                titleTimes(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(300))
                            )
                        }
                        Util.runAction(PlayerType.OPTED_OUT) { it.sendMessage("<game_colour>ᴛʜᴇ ɢᴀᴍᴇ ɪѕ ɢᴇᴛᴛɪɴɢ ʜᴀʀᴅᴇʀ!".style()) }
                    }
                } else {
                    remainingPlayers().forEach { it.equipment.helmet = ItemStack(Material.CARVED_PUMPKIN) }
                    val message =
                        "<game_colour>ʏᴏᴜʀ ᴠɪsɪʙɪʟɪᴛʏ ʜᴀs ʙᴇᴇɴ ʀᴇᴅᴜᴄᴇᴅ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())
                }
            }
        }
    }
}