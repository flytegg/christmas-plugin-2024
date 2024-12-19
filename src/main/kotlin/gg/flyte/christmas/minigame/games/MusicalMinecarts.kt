package gg.flyte.christmas.minigame.games

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
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
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.Rail
import org.bukkit.block.data.Rail.Shape
import org.bukkit.entity.*
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.time.Duration
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random

@Suppress("DuplicatedCode") // nature of this game is very similar to other music-controlled games.
class MusicalMinecarts : EventMiniGame(GameConfig.MUSICAL_MINECARTS) {
    private var overviewTasks = mutableListOf<TwilightRunnable>()
    private var floorBlocks = Util.fillArena(110, Material.SNOW_BLOCK)
    private var roundNumber = 0
    private var minecarts = mutableListOf<Minecart>()
    private var secondsForRound = 12
    private var powerUpLocation: MapSinglePoint? = null
    private var currentBossBar: BossBar? = null
    private var bossBarTask: TwilightRunnable? = null
    private var gameLogicTask: TwilightRunnable? = null
    private var isCountdownActive = false
    private var harder = false
    private var hasEnded = false
    private var stunnedPlayers = mutableSetOf<UUID>()
    private var canEnter = false

    override fun startGameOverview() {
        super.startGameOverview()

        repeat(25) {
            summonMinecart().also {
                overviewTasks += repeatingTask((0..8).random(), (2..6).random()) {
                    if (!(it.isOnGround)) return@repeatingTask

                    it.velocity = it.velocity.add(
                        Vector(
                            Random.nextDouble(-0.35, 0.35),
                            Random.nextDouble(0.5, 1.5),
                            Random.nextDouble(-0.35, 0.35)
                        )
                    )
                }
            }
        }
    }

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())
    }

    override fun startGame() {
        overviewTasks.forEach { it.cancel() }
        minecarts.forEach { it.remove() }.also { minecarts.clear() }

        simpleCountdown {
            newRound()
            Util.runAction(PlayerType.PARTICIPANT) { it.sendMessage("<game_colour>ʀᴇᴍᴇᴍʙᴇʀ, <b>ᴅᴏ ɴᴏᴛ ᴄʟɪᴄᴋ ᴛʜᴇ ᴍɪɴᴇᴄᴀʀᴛѕ</b> ʙᴇꜰᴏʀᴇ ᴛʜᴇ ᴍᴜѕɪᴄ ʜᴀѕ ѕᴛᴏᴘᴘᴇᴅ... ʏᴏᴜ ᴡɪʟʟ ʙᴇ ѕᴛᴜɴɴᴇᴅ!".style()) }
            donationEventsEnabled = true

            tasks += repeatingTask(40) {
                remainingPlayers().forEach {
                    if (it.allowFlight) {
                        it.sendActionBar("<game_colour>ʏᴏᴜ ʜᴀᴠᴇ ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs ᴀᴠᴀɪʟᴀʙʟᴇ!".style())
                    }
                }
            }

            tasks += repeatingTask((0..8).random(), (4..8).random()) {
                delay((0..8).random()) {
                    minecarts.forEach {
                        if (it.passengers.isNotEmpty()) return@forEach
                        it.velocity = it.velocity.add(
                            Vector(
                                Random.nextDouble(-7.5, 7.5),
                                0.1,
                                Random.nextDouble(-7.5, 7.5),
                            )
                        )
                    }
                }
            } // move minecarts aroundz
        }
    }

    private fun newRound() {
        if (hasEnded) return

        roundNumber++
        if (secondsForRound > 2) secondsForRound--

        minecarts.forEach { it.remove() }.also { minecarts.clear() }

        when {
            remainingPlayers().size == 20 && !harder -> {
                harder = true

                Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL) }
                Util.runAction(PlayerType.PARTICIPANT) {
                    it.title(
                        "<game_colour>ʜᴀʀᴅ ᴍᴏᴅᴇ!".style(), Component.empty(),
                        titleTimes(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(300))
                    )
                    it.sendMessage("<red><b>ᴛʜᴇ ᴍɪɴᴇᴄᴀʀᴛѕ ᴡɪʟʟ ᴏɴʟʏ ѕᴘᴀᴡɴ ᴡʜᴇɴ ᴛʜᴇ ᴍᴜѕɪᴄ ѕᴛᴏᴘѕ!".style())
                }
                Util.runAction(PlayerType.OPTED_OUT) { it.sendMessage("<game_colour>ᴛʜᴇ ɢᴀᴍᴇ ɪѕ ɢᴇᴛᴛɪɴɢ ʜᴀʀᴅᴇʀ!".style()) }
            }
        }

        // let song play for a few rounds
        if (roundNumber % 3 == 0) {
            eventController().startPlaylist(SongReference.ALL_I_WANT_FOR_CHRISTMAS_IS_YOU) // beginning makes it hard to differentiate when it has stopped.
        } else {
            eventController().songPlayer?.isPlaying = true
        }

        if (!harder) {
            canEnter = false
            createCartsForRound()
        } // easy-mode summons carts during new rounds.
        powerUp()

        val delayBeforePrepareElimination = (8..12).random()
        tasks += delay(delayBeforePrepareElimination, TimeUnit.SECONDS) {
            if (!isCountdownActive) { // prevent double-prepare due to random condition
                if (harder) createCartsForRound() // hard-mode summons carts when music stops.
                prepareElimination()
            }
        }

        remainingPlayers().forEach { eventController().addPoints(it.uniqueId, 10) }
    }

    private fun prepareElimination() {
        isCountdownActive = true
        canEnter = true

        eventController().songPlayer?.isPlaying = false

        val timerBar: BossBar = BossBar.bossBar(
            "<game_colour><b>ᴛɪᴍᴇ ʟᴇꜰᴛ: $secondsForRound".style(), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS
        ).also { currentBossBar = it }

        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.showBossBar(timerBar) }

        val totalTicks = secondsForRound * 20
        var remainingTicks = totalTicks

        // game logic timer
        gameLogicTask = repeatingTask(5, 5) {
            if (remainingTicks <= 0) {
                this.cancel()
                gameLogicTask = null

                powerUpLocation?.block?.type = Material.AIR
                powerUpLocation = null

                remainingPlayers().forEach { if (it.vehicle == null) eliminate(it, EliminationReason.ELIMINATED) }

                isCountdownActive = false

                if (!hasEnded) tasks += delay(80) { newRound() }
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
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
            it.sendMessage("<red>${player.name} <grey>ʜᴀѕ ʙᴇᴇɴ ᴇʟɪᴍɪɴᴀᴛᴇᴅ!".style())
            it.playSound(Sound.ENTITY_PLAYER_HURT)
        }

        currentBossBar?.let { player.hideBossBar(it) }
        if (player.allowFlight) player.allowFlight = false // if had double-jump
        if (player.gameMode != GameMode.SPECTATOR) {
            Util.runAction(PlayerType.PARTICIPANT, PlayerType.PARTICIPANT) { it.playSound(Sound.ENTITY_ITEM_BREAK) }
        } // don't apply cosmetics if in camera sequence

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
        } // animate death
        super.eliminate(player, reason)

        val value = "$roundNumber ʀᴏᴜɴᴅ${if (roundNumber > 1) "ѕ" else ""}"
        when (remainingPlayers().size) {
            1 -> {
                formattedWinners[player.uniqueId] = value
                formattedWinners[remainingPlayers().first().uniqueId] = "$value (1ѕᴛ ᴘʟᴀᴄᴇ!)"
                remainingPlayers().first().teleport(gameConfig.spawnPoints.random().randomLocation())

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
        hasEnded = true
        tasks.forEach { it?.cancel() }.also { tasks.clear() } // this will cancel all game tasks.
        donationEventsEnabled = false

        val winner = remainingPlayers().first()
        eventController().addPoints(winner.uniqueId, 15)

        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { viewer -> currentBossBar?.let { viewer.hideBossBar(it) } }
        doWinAnimation(winner)
    }

    private fun createCartsForRound() {
        val numCarts = if (remainingPlayers().size == 2) 1 else ceil(remainingPlayers().size * 2 / 3.0).toInt() // ceil condition fails at 2 players
        repeat(numCarts) { summonMinecart() }

        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.playSound(Sound.ENTITY_ITEM_PICKUP) }
    }

    private fun powerUp() {
        val reducedFrequency = remainingPlayers().size < 4 && roundNumber % 4 == 0 // 4 remaining -> every 4th round
        val regularPowerUp = remainingPlayers().size > 4 && roundNumber % 2 == 0 // 5+ remaining -> every 2nd round

        if (reducedFrequency || regularPowerUp) {

            val localLocation = floorBlocks.random()
            powerUpLocation = MapSinglePoint(localLocation.blockX, localLocation.blockY + 1.0, localLocation.blockZ)
            powerUpLocation!!.block.type = Material.BEACON
            powerUpLocation!!.world.spawn(powerUpLocation!!, Firework::class.java) {
                it.fireworkMeta = it.fireworkMeta.apply {
                    addEffect(
                        FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(Color.FUCHSIA, Color.PURPLE, Color.MAROON).withFade(Color.FUCHSIA, Color.PURPLE, Color.MAROON).build()
                    )
                }
                it.detonate()
            }

            val notification = "<game_colour><b>« ᴀ ᴍʏѕᴛᴇʀɪᴏᴜѕ ᴘᴏᴡᴇʀ-ᴜᴘ ʜᴀѕ ѕᴘᴀᴡɴᴇᴅ ᴏɴ ᴛʜᴇ ꜰʟᴏᴏʀ! »".style()
            Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.sendMessage(notification) }
            Util.runAction(PlayerType.PARTICIPANT) {
                it.sendMessage("<grey>ꜰɪɴᴅ ᴛʜᴇ ʙᴇᴀᴄᴏɴ ᴏɴ ᴛʜᴇ ᴍᴀᴘ ᴛᴏ ᴜɴʟᴏᴄᴋ ɪᴛ!".style())
                it.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
            }
        }
    }

    private fun stunPlayer(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 25, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false, false))
        player.playSound(Sound.ENTITY_ITEM_BREAK)
        player.title(
            "<red>ѕᴛᴜɴɴᴇᴅ! ᴛᴏᴏ ᴇᴀʀʟʏ!".style(), "<gold>ᴛʜᴇ ᴍᴜѕɪᴄ ʜᴀѕ ɴᴏᴛ ѕᴛᴏᴘᴘᴇᴅ...".style(),
            titleTimes(Duration.ZERO, Duration.ofSeconds(10), Duration.ofMillis(250))
        )

        stunnedPlayers.add(player.uniqueId)

        delay(10, TimeUnit.SECONDS) {
            stunnedPlayers.remove(player.uniqueId)

            if (Bukkit.getPlayer(player.uniqueId) == null) return@delay
            player.removePotionEffect(PotionEffectType.SLOWNESS)
            player.removePotionEffect(PotionEffectType.BLINDNESS)
        }
    }

    private fun summonMinecart(): Minecart {
        return ChristmasEventPlugin.instance.serverWorld.spawn(floorBlocks.random().add(0.0, 1.5, 0.0), Minecart::class.java) {
            it.isInvulnerable = true
            it.isSlowWhenEmpty = false
            minecarts.add(it)
        }
    }

    private fun doWinAnimation(player: Player) {
        val worldNPCs = mutableListOf<WorldNPC>()
        val animationTasks = mutableListOf<TwilightRunnable>()
        val poweredRails = mapOf(
            MapRegion(MapSinglePoint(606, 111, 811), MapSinglePoint(626, 111, 811)) to Pair(Shape.EAST_WEST, "left"),
            MapRegion(MapSinglePoint(606, 111, 789), MapSinglePoint(626, 111, 789)) to Pair(Shape.EAST_WEST, "right"),
            MapRegion(MapSinglePoint(627, 111, 790), MapSinglePoint(627, 111, 810)) to Pair(Shape.NORTH_SOUTH, "right"),
            MapRegion(MapSinglePoint(605, 111, 790), MapSinglePoint(605, 111, 810)) to Pair(Shape.NORTH_SOUTH, "left")
        )
        val connectorRails = mapOf(
            MapSinglePoint(605, 111, 811) to Shape.NORTH_EAST,
            MapSinglePoint(627, 111, 811) to Shape.NORTH_WEST,
            MapSinglePoint(627, 111, 789) to Shape.SOUTH_WEST,
            MapSinglePoint(605, 111, 789) to Shape.SOUTH_EAST
        )
        val droppedItems = mutableListOf<Item>()

        connectorRails.forEach { (location, shape) ->
            location.block.apply {
                type = Material.RAIL
                blockData = (blockData as Rail).apply { this.shape = shape }
            }
        }

        poweredRails.forEach { (region, railData) ->
            region.toSingleBlockLocations().forEach {
                it.block.apply {
                    setType(Material.POWERED_RAIL, false)
                    blockData = Bukkit.createBlockData(Material.POWERED_RAIL).apply {
                        (this as Rail).shape = railData.first
                        (this as Powerable).isPowered = true
                    }
                }
            }
        }

        poweredRails.keys.flatMap { it.toSingleBlockLocations() }.forEachIndexed { index, location ->
            if (index % 6 == 0) {
                val direction = Vector(0, 0, 0)
                val value = poweredRails.entries.first { entry -> entry.key.contains(location) }.value

                direction.apply {
                    if (value.first == Shape.NORTH_SOUTH) {
                        setZ(if (value.second == "left") 1 else -1)
                    } else {
                        setX(if (value.second == "left") 1 else -1)
                    }
                }

                location.world.spawn(location.add(0.0, 1.0, 0.0), Minecart::class.java) { minecart ->
                    minecart.maxSpeed = 0.6
                    minecart.velocity = direction.normalize()
                    minecarts.add(minecart)

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
                    val displayName = "$randomColour${player.name}".style()

                    val npc = WorldNPC.createFromLive(displayName, player, location).also {
                        worldNPCs.add(it)
                        it.spawnForAll()
                        it.npc.mainHand = ItemStack(Material.MINECART).packetObj()
                        it.npc.updateEquipment()
                    }

                    Bukkit.getOnlinePlayers().forEach { loopedPlayer ->
                        val passengerPacket = WrapperPlayServerSetPassengers(minecart.entityId, intArrayOf(npc.npc.id))
                        delay(1) { passengerPacket.sendPacket(loopedPlayer) }

                        animationTasks += repeatingTask(30) {
                            delay((0..15).random()) {
                                WrapperPlayServerEntityAnimation(
                                    npc.npc.id,
                                    WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                                ).sendPacket(loopedPlayer)

                                minecart.world.dropItemNaturally(minecart.location.add(0.0, 1.0, 0.0), ItemStack(Material.MINECART)) {
                                    it.velocity = Vector(Random.nextDouble(-1.0, 1.0), Random.nextDouble(0.0, 0.5), Random.nextDouble(-1.0, 1.0))
                                    droppedItems.add(it)
                                }
                            }
                        } // NPC swing

                        var yaw = 0
                        animationTasks += repeatingTask((5..25).random(), 4) {
                            WrapperPlayServerEntityHeadLook(npc.npc.id, yaw.toFloat()).sendPacket(loopedPlayer)
                            WrapperPlayServerEntityRotation(npc.npc.id, yaw.toFloat(), 0F, true).sendPacket(loopedPlayer)
                            yaw += 10
                        } // NPC look
                    }
                }
            }
        }

        delay(20, TimeUnit.SECONDS) {
            worldNPCs.forEach { it.despawnForAll() }
            minecarts.forEach { it.remove() }
            droppedItems.forEach { it.remove() }
            animationTasks.forEach { it.cancel() }
            poweredRails.keys.flatMap { it.toSingleBlockLocations() }.forEach { it.block.type = Material.AIR }
            connectorRails.keys.forEach { it.block.type = Material.AIR }

            Util.fillArena(110, Material.AIR)
            super.endGame()
        }
    }

    override fun handleGameEvents() {
        listeners += event<InventoryClickEvent> { isCancelled = true }

        listeners += event<PlayerToggleSneakEvent> { if (player.vehicle != null) isCancelled = true }

        listeners += event<VehicleEnterEvent> {
            if (entered !is Player) return@event
            if (entered.vehicle != null) { // prevent moving between minecarts
                isCancelled = true
                return@event
            }

            if (stunnedPlayers.contains((entered as Player).uniqueId)) {
                isCancelled = true
                return@event
            }

            if (!canEnter) {
                stunPlayer(entered as Player)
                isCancelled = true
            }
        }

        listeners += event<PlayerInteractEvent> {
            if (clickedBlock?.type == Material.BEACON) {
                clickedBlock?.type = Material.AIR
                val randomPowerUp = PowerUp.entries.random()

                Util.runAction(PlayerType.PARTICIPANT) {
                    if (it == player) {
                        it.sendMessage("<green><b>ʏᴏᴜ'ᴠᴇ ꜰᴏᴜɴᴅ ᴀ ${randomPowerUp.displayName} ᴘᴏᴡᴇʀ-ᴜᴘ!".style())
                    } else {
                        it.sendMessage("<green><b>« ${player.name} ʜᴀѕ ꜰᴏᴜɴᴅ ᴀ ${randomPowerUp.displayName} ᴘᴏᴡᴇʀ-ᴜᴘ! »".style())
                    }
                }
                Util.runAction(PlayerType.OPTED_OUT) { it.sendMessage("<green><b>« ${player.name} ʜᴀѕ ꜰᴏᴜɴᴅ ᴀ ${randomPowerUp.displayName} ᴘᴏᴡᴇʀ-ᴜᴘ! »".style()) }

                when (randomPowerUp) {
                    PowerUp.BLINDNESS -> player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 2, false, false, false))

                    PowerUp.DOUBLE_JUMP -> player.allowFlight = true

                    PowerUp.ENDER_PEARL -> player.inventory.setItem(0, ItemStack(Material.ENDER_PEARL, 1))

                    PowerUp.EXTRA_CART -> player.inventory.addItem(ItemStack(Material.MINECART, 1))

                    PowerUp.FISHING_ROD -> player.inventory.setItem(0, ItemStack(Material.FISHING_ROD, 1))

                    PowerUp.JUMP_BOOST -> player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 8, 3, false, false, false))

                    PowerUp.PUSH_RANDOM -> {
                        val eventPlayer = player // prevent shadowing
                        remainingPlayers().random().apply {
                            velocity = this.location.direction.multiply(2).add(Vector(0.0, 1.5, 0.0))
                            sendMessage("<game_colour>ʏᴏᴜ'ᴠᴇ ʙᴇᴇɴ ᴘᴜѕʜᴇᴅ ʙʏ ᴀ ᴘᴏᴡᴇʀ-ᴜᴘ!".style())
                            eventPlayer.sendMessage("<game_colour>ʏᴏᴜ'ᴠᴇ ᴘᴜѕʜᴇᴅ ᴀ ʀᴀɴᴅᴏᴍ ᴘʟᴀʏᴇʀ (${name}) ᴡɪᴛʜ ᴛʜᴇ ᴘᴏᴡᴇʀ-ᴜᴘ!".style())
                        }
                    }

                    PowerUp.PUSH_SELF -> player.velocity = player.location.direction.multiply(2).add(Vector(0.0, 1.5, 0.0))

                    PowerUp.RANDOM_TP -> player.teleport(floorBlocks.random())

                    PowerUp.SLOWNESS -> player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 2, false, false, false))
                }
            }

            if (item?.type == Material.MINECART) {
                if (player.vehicle == null) {
                    val minecart = summonMinecart()
                    minecart.addPassenger(player)
                    minecarts.add(minecart)
                    item?.amount = item?.amount?.minus(1) ?: 0
                }
            }
        }

        listeners += event<PlayerToggleFlightEvent> {
            if (!(remainingPlayers().contains(player))) return@event // OP'd players need to be able to fly
            isCancelled = true
            player.allowFlight = false
            player.isFlying = false

            player.velocity = player.location.direction.multiply(0.5).add(Vector(0.0, 1.0, 0.0))
            player.playSound(Sound.ITEM_TOTEM_USE)
        } // double-jump

        listeners += event<BlockPhysicsEvent> {
            if (block.type == Material.POWERED_RAIL) isCancelled = true
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> TODO()
            DonationTier.MEDIUM -> TODO()
            DonationTier.HIGH -> TODO()
        }
    }

    private enum class PowerUp(val displayName: String) {
        BLINDNESS("ʙʟɪɴᴅɴᴇѕѕ"),
        DOUBLE_JUMP("ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘ"),
        ENDER_PEARL("ᴇɴᴅᴇʀ ᴘᴇᴀʀʟ"),
        EXTRA_CART("ᴇxᴛʀᴀ ᴍɪɴᴇᴄᴀʀᴛ"),
        FISHING_ROD("ꜰɪѕʜɪɴɢ ʀᴏᴅ"),
        JUMP_BOOST("ᴊᴜᴍᴘ ʙᴏᴏѕᴛ"),
        PUSH_RANDOM("ʀᴀɴᴅᴏᴍ ᴘʟᴀʏᴇʀ ʙᴏᴏѕᴛ"),
        PUSH_SELF("ʀᴀɴᴅᴏᴍ ѕᴇʟꜰ-ʙᴏᴏѕᴛ"),
        RANDOM_TP("ʀᴀɴᴅᴏᴍ ᴛᴘ"),
        SLOWNESS("ѕʟᴏᴡɴᴇѕѕ")
    }
}
