package gg.flyte.christmas.minigame.games

import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.engine.EventMiniGame
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.engine.PlayerType
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.util.Util
import gg.flyte.christmas.util.eventController
import gg.flyte.christmas.util.formatInventory
import gg.flyte.christmas.util.style
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.hidePlayer
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.extension.showPlayer
import gg.flyte.twilight.scheduler.TwilightRunnable
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.data.type.Snow
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class Spleef : EventMiniGame(GameConfig.SPLEEF) {
    private var overviewTasks = mutableListOf<TwilightRunnable>()
    private var floorLevelBlocks: List<MapSinglePoint> = listOf(
        Util.fillArena(110, Material.SNOW_BLOCK),
        Util.fillArena(98, Material.SNOW_BLOCK),
        Util.fillArena(86, Material.SNOW_BLOCK),
    ).flatMap { it }
    private var seconds = 0
    private var doubleJumps = mutableMapOf<Player, Int>()

    override fun startGameOverview() {
        super.startGameOverview()

        overviewTasks += repeatingTask(1) {
            while (floorLevelBlocks.random().block.type != Material.AIR) {
                floorLevelBlocks.random().block.type = Material.AIR
            }
        }
    }

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())
        ItemStack(Material.DIAMOND_SHOVEL).apply {
            itemMeta = itemMeta.apply {
                displayName("<!i><game_colour>Snow Shovel!".style())
            }
        }.let { player.inventory.setItem(0, it) }
        doubleJumps[player] = 3
    }

    override fun startGame() {
        overviewTasks.forEach { it.cancel() }
        donationEventsEnabled = true

        for (point in floorLevelBlocks) point.block.type = Material.SNOW_BLOCK // reset after game overview

        simpleCountdown {
            Util.runAction(PlayerType.PARTICIPANT) {
                it.gameMode = GameMode.SURVIVAL
                it.allowFlight = true
            }

            tasks += repeatingTask(20) {
                seconds++
                updateScoreboard()
                // every 30 seconds, they get points
                if (seconds % 20 == 0) {
                    remainingPlayers().forEach {
                        it.sendMessage("<green>+1 ᴘᴏɪɴᴛ ꜰᴏʀ ѕᴜʀᴠɪᴠɪɴɢ 30 ѕᴇᴄᴏɴᴅѕ!".style())
                        eventController().addPoints(it.uniqueId, 1)
                    }
                }
            }

            tasks += repeatingTask(5) {
                delay((1..15).random()) {
                    // decay random snow blocks, that are not air
                    floorLevelBlocks.filter { it.block.type != Material.AIR }.random().let {
                        when (it.block.type) {
                            Material.SNOW_BLOCK -> {
                                it.block.type = Material.SNOW
                                if (it.block.blockData !is Snow) return@delay
                                it.block.blockData = (it.block.blockData as Snow).apply { layers = 5 }
                            }

                            Material.SNOW -> {
                                val blockData = it.block.blockData as Snow
                                if (blockData.layers == 2) {
                                    it.block.type = Material.AIR
                                } else {
                                    blockData.layers -= 3
                                    it.block.blockData = blockData
                                }
                            }

                            else -> return@delay
                        }
                    }
                }
            }

            manageActionBars()
        }
    }

    override fun eliminate(player: Player, reason: EliminationReason) {
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.sendMessage("<red>${player.name} <grey>has been eliminated!".style()) }
        doubleJumps.remove(player)

        player.allowFlight = false
        if (player.gameMode != GameMode.SPECTATOR) {
            Util.runAction(PlayerType.PARTICIPANT, PlayerType.PARTICIPANT) { it.playSound(Sound.ENTITY_ITEM_BREAK) }
        } // don't apply cosmetics if in camera sequence

        @Suppress("DuplicatedCode") // yes ik but im lazy and can't think of any other animations
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

        val value = "$seconds ѕᴇᴄᴏɴᴅ${if (seconds > 1) "ѕ" else ""}"
        when (remainingPlayers().size) {
            1 -> {
                formattedWinners[player.uniqueId] = value
                formattedWinners[remainingPlayers().first().uniqueId] = "$value (1ѕᴛ ᴘʟᴀᴄᴇ!)"
                endGame()
            }

            2 -> formattedWinners[player.uniqueId] = value
        }
    }

    override fun endGame() {
        donationEventsEnabled = false

        for (point in floorLevelBlocks) point.block.type = Material.AIR
        super.endGame()
    }

    private fun updateScoreboard() {
        val playersLeft = "<aqua>ᴘʟᴀʏᴇʀs ʟᴇꜰᴛ: <red><b>${remainingPlayers().size}".style()
        Bukkit.getOnlinePlayers().forEach { eventController().sidebarManager.updateLines(it, listOf(Component.empty(), playersLeft)) }
    }

    private fun manageActionBars() {
        tasks += repeatingTask(10) {
            remainingPlayers().forEach {
                if (doubleJumps[it]!! > 0) {
                    it.sendActionBar("<green><b>${doubleJumps[it]!!} <reset><game_colour>ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘѕ ʟᴇꜰᴛ!".style())
                }
            }
        }
    }

    override fun handleGameEvents() {
        listeners += event<BlockBreakEvent> {
            isCancelled = true
            floorLevelBlocks.forEach {
                if (it.block == block) {
                    isCancelled = false
                    isDropItems = false
                    player.inventory.addItem(ItemStack(Material.SNOWBALL, (1..4).random()).apply {
                        itemMeta = itemMeta.apply { setMaxStackSize(99) }
                    })
                }
            }
        }

        listeners += event<PlayerToggleFlightEvent> {
            if (player.gameMode != GameMode.SURVIVAL) return@event // elimed players alr in ADVENTURE
            isCancelled = true

            if (doubleJumps[player]!! > 0) {
                player.isFlying = false
                player.velocity = player.location.direction.multiply(0.5).add(Vector(0.0, 1.25, 0.0))
                player.playSound(Sound.ITEM_TOTEM_USE)
                doubleJumps[player] = doubleJumps[player]!! - 1
            } else player.allowFlight = false
        }

        listeners += event<ProjectileHitEvent> {
            if (hitBlock == null) return@event
            if (entity !is Snowball) return@event

            floorLevelBlocks.any { it.block == hitBlock }.let {
                when (hitBlock!!.type) {
                    Material.SNOW_BLOCK -> {
                        hitBlock!!.type = Material.SNOW
                        if (hitBlock!!.blockData !is Snow) return@event
                        hitBlock!!.blockData = (hitBlock!!.blockData as Snow).apply { layers = 5 }
                    }

                    Material.SNOW -> {
                        val blockData = hitBlock!!.blockData as Snow
                        if (blockData.layers == 2) {
                            hitBlock!!.type = Material.AIR
                        } else {
                            blockData.layers = blockData.layers - 3
                            hitBlock!!.blockData = blockData
                        }
                    }

                    else -> return@event
                }
            }
        }

        listeners += event<PlayerMoveEvent> {
            if (player.location.blockY < 70 && remainingPlayers().contains(player)) eliminate(player, EliminationReason.ELIMINATED)
        }
    }

    override fun handleDonation(tier: DonationTier) {
        when (tier) {
            DonationTier.LOW -> TODO()
            DonationTier.MEDIUM -> TODO()
            DonationTier.HIGH -> TODO()
        }
    }
}
