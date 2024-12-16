package gg.flyte.christmas.minigame.games

import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.engine.EventMiniGame
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.engine.PlayerType
import gg.flyte.christmas.minigame.world.MapRegion
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.util.*
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.util.Vector
import java.time.Duration
import java.util.*
import kotlin.math.floor

class KingHill : EventMiniGame(GameConfig.KING_OF_THE_HILL) {
    private var hillRegion = MapRegion(MapSinglePoint(824, 85, 633), MapSinglePoint(830, 88, 627))
    private var pvpEnabled = false
    private var gameTime = 90
    private val respawnBelow = 71
    private val timeOnHill = mutableMapOf<UUID, Int>()

    private var delayedKbTicksTotal = 0
    private var delayedKbTicksLeft = -1

    private var thrownAroundTicksTotal = 0
    private var thrownAroundTicksLeft = -1

    private val velocityMap = mutableMapOf<UUID, MutableList<Vector>>()

    override fun startGameOverview() {
        super.startGameOverview()
        eventController().sidebarManager.dataSupplier = timeOnHill
    }

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())

        val stick = ItemStack(Material.STICK).apply {
            itemMeta = itemMeta.apply {
                displayName("<!i><game_colour>ᴋɴᴏᴄᴋʙᴀᴄᴋ ѕᴛɪᴄᴋ!".style())
            }

            addUnsafeEnchantment(Enchantment.KNOCKBACK, 5)
        }
        player.inventory.addItem(stick)
        timeOnHill[player.uniqueId] = 0
    }

    override fun startGame() {
        simpleCountdown {
            donationEventsEnabled = true

            pvpEnabled = true
            Util.runAction(PlayerType.PARTICIPANT) {
                preparePlayer(it)
                it.title(
                    Component.empty(), "<game_colour>ᴘᴠᴘ ᴇɴᴀʙʟᴇᴅ!".style(),
                    titleTimes(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(300))
                )
            }

            tasks += repeatingTask(1, TimeUnit.SECONDS) {
                Util.runAction(PlayerType.PARTICIPANT) {
                    if (hillRegion.contains(it.location)) {
                        if (gameTime == 0) return@runAction // viewing win animation; don't increment time

                        timeOnHill[it.uniqueId] = timeOnHill[it.uniqueId]!! + 1
                        it.playSound(Sound.ENTITY_ITEM_PICKUP)
                        it.sendActionBar("<green>+1 ѕᴇᴄᴏɴᴅ".style())
                    }
                }
                gameTime--

                if (gameTime == 0) {
                    pvpEnabled = false
                    cancel()
                    endGame()
                }

                updateScoreboard()
            }

            tasks += repeatingTask(1, TimeUnit.TICKS) {
                if (delayedKbTicksLeft == -1) {
                    return@repeatingTask
                }

                if (delayedKbTicksLeft == 0) {
                    delayedKbTicksLeft = -1

                    thrownAroundTicksLeft = delayedKbTicksTotal
                    thrownAroundTicksTotal = delayedKbTicksTotal
                    delayedKbTicksTotal = 0
                } else {
                    delayedKbTicksLeft -= 1
                }
            }

            tasks += repeatingTask(1, TimeUnit.TICKS) {
                if (thrownAroundTicksLeft == -1) {
                    return@repeatingTask
                }

                thrownAroundTicksLeft -= 1

                if (thrownAroundTicksLeft == 0) {
                    velocityMap.entries.clear()
                    return@repeatingTask
                }

                velocityMap.entries.forEach {
                    val player = Bukkit.getPlayer(it.key)!!
                    val vectors = it.value

                    val vectorCount = vectors.size

                    if (vectorCount == 0) {
                        return@forEach
                    }

                    val interval = thrownAroundTicksLeft.toDouble() / vectorCount.toDouble()
                    val floor = floor(interval)
                    if (floor != interval || interval < 0 || interval >= vectorCount) {
                        return@forEach
                    }

                    val value = vectors[interval.toInt()]
                    player.velocity = value
                }
            }
        }
    }

    override fun endGame() {
        donationEventsEnabled = false

        Util.runAction(PlayerType.PARTICIPANT) { it.teleport(gameConfig.spawnPoints.random().randomLocation()) }
        for (entry in timeOnHill) eventController().addPoints(entry.key, entry.value)

        val (first) = timeOnHill.entries
            .sortedByDescending { it.value }
            .take(3)
            .also {
                it.forEach { entry ->
                    formattedWinners[entry.key] = entry.value.toString() + " ѕᴇᴄᴏɴᴅ${if (entry.value > 1) "ѕ" else ""}"
                }
            }

        var yaw = 0F
        ChristmasEventPlugin.instance.serverWorld.spawn(MapSinglePoint(827.5, 105, 630.5, 0, 0), ItemDisplay::class.java) {
            it.setItemStack(ItemStack(Material.PLAYER_HEAD).apply {
                val meta = itemMeta as SkullMeta
                meta.owningPlayer = Bukkit.getOfflinePlayer(first.key)
                itemMeta = meta
            })
            it.interpolationDelay = -1
            it.interpolationDuration = 200
            it.teleportDuration = 15
            it.isGlowing = true

            tasks += delay(1) {
                it.transformation = it.transformation.apply {
                    scale.mul(25F)
                } // apply scale transformation

                tasks += repeatingTask(0, 14) {
                    val clone = it.location.clone()
                    clone.yaw = yaw
                    it.teleport(clone)

                    yaw += 90
                } // rotate the head lol
            }

            tasks += delay(20, TimeUnit.SECONDS) {
                it.remove()
                super.endGame()
            }
        }
    }

    private fun updateScoreboard() {
        val timeLeft = "<aqua>ᴛɪᴍᴇ ʟᴇꜰᴛ: <red><b>${gameTime}".style()
        Bukkit.getOnlinePlayers().forEach { eventController().sidebarManager.updateLines(it, listOf(Component.empty(), timeLeft)) }
    }

    @Suppress("UnstableApiUsage")
    override fun handleGameEvents() {
        listeners += event<EntityDamageEvent>(priority = EventPriority.HIGHEST) {
            // return@event -> already cancelled by lower priority [HousekeepingEventListener]

            if (cause == EntityDamageEvent.DamageCause.FALL) return@event

            entity as? Player ?: return@event
            val damager = (this as? EntityDamageByEntityEvent)?.damager as? Player ?: return@event

            if (!pvpEnabled) {
                isCancelled = true
                damager.playSound(Sound.BLOCK_NOTE_BLOCK_BASS)
                return@event
            }
            isCancelled = false
            damage = 0.0
        }

        listeners += event<PlayerMoveEvent> {
            if (player.location.blockY < respawnBelow) {
                player.teleport(gameConfig.spawnPoints.random().randomLocation())
                player.playSound(Sound.ENTITY_PLAYER_TELEPORT)
            }
        }

        listeners += event<EntityDamageByEntityEvent>(priority = EventPriority.HIGHEST) {
            if (delayedKbTicksLeft == 0) {
                return@event
            }

            if (entity !is Player) {
                return@event
            }

            val velocityList = velocityMap.computeIfAbsent(entity.uniqueId) { mutableListOf() }

            val damagedLocation = entity.location.toVector()
            val damagerLocation = (damageSource.causingEntity ?: damageSource.directEntity)!!.location.toVector()

            val direction = damagedLocation.subtract(damagerLocation).normalize()

            velocityList.add(direction)
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> TODO()
            DonationTier.MEDIUM -> doDelayedKnockback(donorName)
            DonationTier.HIGH -> TODO()
        }
    }

    private fun doDelayedKnockback(name: String?) {
        delayedKbTicksLeft = 0
        delayedKbTicksTotal = 0

        delayedKbTicksLeft += 20 * 5
        delayedKbTicksTotal += 20 * 5

        val message =
            "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴅᴇʟᴀʏᴇᴅ ᴋɴᴏᴄᴋʙᴀᴄᴋ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())
    }
}
