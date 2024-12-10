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
import java.time.Duration
import java.util.*

class KingHill : EventMiniGame(GameConfig.KING_OF_THE_HILL) {
    private var hillRegion = MapRegion(MapSinglePoint(824, 85, 633), MapSinglePoint(830, 88, 627))
    private var pvpEnabled = false
    private var gameTime = 90
    private val respawnBelow = 71
    private val timeOnHill = mutableMapOf<UUID, Int>()

    override fun startGameOverview() {
        super.startGameOverview()
        eventController().sidebarManager.dataSupplier = timeOnHill
    }

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())

        var stick = ItemStack(Material.STICK).apply {
            itemMeta = itemMeta.apply {
                displayName("<!i><game_colour>Knockback Stick!".style())
            }

            addUnsafeEnchantment(Enchantment.KNOCKBACK, 5)
        }
        player.inventory.addItem(stick)
        timeOnHill[player.uniqueId] = 0
    }

    override fun startGame() {
        simpleCountdown {
            pvpEnabled = true
            Util.runAction(PlayerType.PARTICIPANT) {
                preparePlayer(it)
                it.title(
                    Component.empty(), "<game_colour>PVP Enabled!".style(),
                    titleTimes(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(300))
                )
            }

            tasks += repeatingTask(1, TimeUnit.SECONDS) {
                Util.runAction(PlayerType.PARTICIPANT) {
                    if (hillRegion.contains(it.location)) {
                        if (gameTime == 0) return@runAction // viewing win animation; don't increment time

                        timeOnHill[it.uniqueId] = timeOnHill[it.uniqueId]!! + 1
                        it.playSound(Sound.ENTITY_ITEM_PICKUP)
                        it.sendMessage("<green>+1 second".style())
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
        }
    }

    override fun endGame() {
        Util.runAction(PlayerType.PARTICIPANT) { it.teleport(gameConfig.spawnPoints.random().randomLocation()) }
        for (entry in timeOnHill) eventController().addPoints(entry.key, entry.value)

        val (first) = timeOnHill.entries
            .sortedBy { it.value }
            .take(3)
            .also { it.forEach { formattedWinners.put(it.key, it.value.toString() + " second${if (it.value > 1) "s" else ""}") } }

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
    }

    override fun handleDonation(tier: DonationTier) {
        when (tier) {
            DonationTier.LOW -> TODO()
            DonationTier.MEDIUM -> TODO()
            DonationTier.HIGH -> TODO()
        }
    }
}
