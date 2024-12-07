package gg.flyte.christmas.minigame.games

import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.engine.EventMiniGame
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.engine.PlayerType
import gg.flyte.christmas.util.*
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.*

class Paintball : EventMiniGame(GameConfig.PAINTBALL) {
    private var gameTime = 90
    private val scores = mutableMapOf<UUID, Int>()
    private var started = false
    private val paintMaterials = mutableListOf(
        Material.RED_WOOL,
        Material.ORANGE_WOOL,
        Material.YELLOW_WOOL,
        Material.LIME_WOOL,
        Material.GREEN_WOOL,
        Material.LIGHT_BLUE_WOOL,
        Material.CYAN_WOOL,
        Material.BLUE_WOOL,
        Material.PURPLE_WOOL,
        Material.PINK_WOOL,
        Material.MAGENTA_WOOL,
        Material.LIGHT_GRAY_WOOL,
        Material.GRAY_WOOL,
        Material.BROWN_WOOL,
        Material.BLACK_WOOL,
        Material.WHITE_WOOL,
        Material.RED_CONCRETE,
        Material.ORANGE_CONCRETE,
        Material.YELLOW_CONCRETE,
        Material.LIME_CONCRETE,
        Material.GREEN_CONCRETE,
        Material.LIGHT_BLUE_CONCRETE,
        Material.CYAN_CONCRETE,
        Material.BLUE_CONCRETE,
        Material.PURPLE_CONCRETE,
        Material.PINK_CONCRETE,
        Material.MAGENTA_CONCRETE,
        Material.LIGHT_GRAY_CONCRETE,
        Material.GRAY_CONCRETE,
        Material.BROWN_CONCRETE,
        Material.BLACK_CONCRETE,
        Material.WHITE_CONCRETE,
        Material.RED_GLAZED_TERRACOTTA,
        Material.ORANGE_GLAZED_TERRACOTTA,
        Material.YELLOW_GLAZED_TERRACOTTA,
        Material.LIME_GLAZED_TERRACOTTA,
        Material.GREEN_GLAZED_TERRACOTTA,
        Material.LIGHT_BLUE_GLAZED_TERRACOTTA,
        Material.CYAN_GLAZED_TERRACOTTA,
        Material.BLUE_GLAZED_TERRACOTTA,
        Material.PURPLE_GLAZED_TERRACOTTA,
        Material.PINK_GLAZED_TERRACOTTA,
        Material.MAGENTA_GLAZED_TERRACOTTA,
        Material.LIGHT_GRAY_GLAZED_TERRACOTTA,
        Material.GRAY_GLAZED_TERRACOTTA,
        Material.BROWN_GLAZED_TERRACOTTA,
        Material.BLACK_GLAZED_TERRACOTTA,
        Material.WHITE_GLAZED_TERRACOTTA,
    )

    override fun preparePlayer(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.formatInventory()
        player.teleport(gameConfig.spawnPoints.random().randomLocation())
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 1000000, 2, false, false, false))

        scores[player.uniqueId] = 0

        ItemStack(Material.GOLDEN_HOE).apply {
            itemMeta = itemMeta.apply {
                displayName("<!i><game_colour>Paintball <gold>Gun!".style())
            }
        }.apply { player.inventory.setItem(0, this) }
    }

    override fun startGame() {
        eventController().sidebarManager.dataSupplier = scores
        simpleCountdown {
            started = true
            Util.runAction(PlayerType.PARTICIPANT) {
                it.title(
                    "<game_colour>Shoot!".style(), Component.empty(),
                    titleTimes(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(300))
                )
            }

            tasks += repeatingTask(1, TimeUnit.SECONDS) {
                gameTime--

                if (gameTime == 0) {
                    cancel()
                    endGame()
                }

                updateScoreboard()
            }
        }
    }

    override fun endGame() {
        @Suppress("DuplicatedCode") // I'm lazy
        for (entry in scores) eventController().addPoints(entry.key, entry.value)
        scores.entries
            .sortedByDescending { it.value }
            .take(3)
            .also { it.forEach { formattedWinners.put(it.key, it.value.toString() + " kill${if (it.value > 1) "s" else ""}") } }

        super.endGame()
    }

    private fun updateScoreboard() {
        val timeLeft = "<aqua>ᴛɪᴍᴇ ʟᴇꜰᴛ: <red><b>${gameTime}".style()
        Bukkit.getOnlinePlayers().forEach { eventController().sidebarManager.updateLines(it, listOf(Component.empty(), timeLeft)) }
    }

    override fun handleGameEvents() {
        listeners += event<PlayerInteractEvent> {
            if (!started) return@event

            if (!hasItem()) return@event
            if (item!!.type != Material.GOLDEN_HOE) return@event
            if (!(action.name.lowercase().contains("right"))) return@event

            player.launchProjectile(Snowball::class.java).apply { item = ItemStack(paintMaterials.random()) }
        }

        listeners += event<ProjectileHitEvent> {
            if (!started) return@event

            if (hitEntity == null) return@event
            if (!(hitEntity is Player && remainingPlayers().contains(hitEntity as Player))) return@event
            if (entity.shooter == null || entity.shooter !is Player) return@event

            val shooter = entity.shooter as Player
            val hitPlayer = hitEntity as Player
            if (shooter.uniqueId == hitPlayer.uniqueId) return@event

            hitPlayer.world.spawnParticle(
                Particle.BLOCK,
                hitPlayer.location,
                200,
                0.5,
                0.5,
                0.5,
                Bukkit.createBlockData((entity as ThrowableProjectile).item.type)
            )

            hitPlayer.teleport(gameConfig.spawnPoints.random().randomLocation())
            hitPlayer.playSound(Sound.ENTITY_PLAYER_HURT)

            shooter.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
            shooter.sendMessage("<green>You hit <red>${hitPlayer.name} <grey>— <green>+1".style())
            scores[shooter.uniqueId] = scores[shooter.uniqueId]!! + 1
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
