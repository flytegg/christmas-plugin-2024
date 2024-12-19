package gg.flyte.christmas.minigame.games

import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.engine.EventMiniGame
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.engine.PlayerType
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.util.*
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.*
import kotlin.random.Random

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

    // ticks left | total ticks
    private var glowingTickData: Pair<Int, Int> = 0 to 0
    private var nauseatedTickData: Pair<Int, Int> = 0 to 0
    private var rapidFireTickData: Pair<Int, Int> = 0 to 0

    // lateinit since <game_colour> is not mapped yet at time of init
    private lateinit var glowingBossBar: BossBar
    private lateinit var nauseatedBossBar: BossBar
    private lateinit var rapidFireBossBar: BossBar

    override fun preparePlayer(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.formatInventory()
        player.teleport(gameConfig.spawnPoints.random().randomLocation())
        player.walkSpeed = 0.4F

        scores[player.uniqueId] = 0

        ItemStack(Material.GOLDEN_HOE).apply {
            itemMeta = itemMeta.apply {
                displayName("<!i><game_colour>ᴘᴀɪɴᴛʙᴀʟʟ <gold>ɢᴜɴ!".style())
                addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE)
            }
        }.apply { player.inventory.setItem(0, this) }
    }

    override fun startGame() {
        eventController().sidebarManager.dataSupplier = scores
        simpleCountdown {
            started = true
            donationEventsEnabled = true
            glowingBossBar = BossBar.bossBar("<game_colour><b>ɢʟᴏᴡɪɴɢ ᴇɴᴀʙʟᴇᴅ".style(), 1.0F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
            nauseatedBossBar = BossBar.bossBar("<game_colour><b>ʏᴏᴜ ꜰᴇᴇʟ sɪᴄᴋ...".style(), 1.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
            rapidFireBossBar = BossBar.bossBar("<game_colour><b>ʀᴀᴘɪᴅ ꜰɪʀᴇ".style(), 1.0F, BossBar.Color.RED, BossBar.Overlay.PROGRESS)

            Util.runAction(PlayerType.PARTICIPANT) {
                it.title(
                    "<game_colour>ѕʜᴏᴏᴛ!".style(), Component.empty(),
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
        donationEventsEnabled = false

        for (entry in scores) eventController().addPoints(entry.key, entry.value)
        scores.entries
            .sortedByDescending { it.value }
            .take(3)
            .also {
                it.forEach { entry ->
                    formattedWinners[entry.key] = entry.value.toString() + " ᴋɪʟʟ${if (entry.value > 1) "ѕ" else ""}"
                }
            }

        Util.runAction(PlayerType.PARTICIPANT) {
            it.walkSpeed = 0.2F
            it.isGlowing = false
            it.hideBossBar(glowingBossBar)
            it.hideBossBar(nauseatedBossBar)
            it.hideBossBar(rapidFireBossBar)
        }

        super.endGame()
    }

    private fun updateScoreboard() {
        val timeLeft = "<aqua>ᴛɪᴍᴇ ʟᴇꜰᴛ: <red><b>${gameTime}".style()
        Bukkit.getOnlinePlayers().forEach { eventController().sidebarManager.updateLines(it, listOf(Component.empty(), timeLeft)) }
    }

    private fun glowEnabled() = glowingTickData.first > 0

    private fun nauseaEnabled() = nauseatedTickData.first > 0

    private fun rapidFireEnabled() = rapidFireTickData.first > 0

    override fun handleGameEvents() {
        listeners += event<PlayerInteractEvent> {
            if (!started) return@event

            if (!hasItem()) return@event
            if (item!!.type != Material.GOLDEN_HOE) return@event
            if (!(action.name.lowercase().contains("right"))) return@event

            if (rapidFireEnabled()) {
                var times = 0 // must reach 4
                repeatingTask(1) {
                    if (times == 4) {
                        cancel()
                        return@repeatingTask
                    }
                    player.launchProjectile(Snowball::class.java).apply { item = ItemStack(paintMaterials.random()) }
                    times++
                }
            } else {
                // singly shoot
                player.launchProjectile(Snowball::class.java).apply { item = ItemStack(paintMaterials.random()) }
            }
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
            shooter.sendMessage("<green>ʏᴏᴜ ʜɪᴛ <red>${hitPlayer.name} <grey>— <green>+1".style())
            scores[shooter.uniqueId] = scores[shooter.uniqueId]!! + 1
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> {
                if (Random.nextBoolean()) {
                    remainingPlayers().forEach {
                        it.isGlowing = true
                        it.showBossBar(glowingBossBar)
                    }

                    if (glowEnabled()) {
                        // extend duration if already active
                        glowingTickData = glowingTickData.let { it.first + (10 * 20) to it.second + (10 * 20) }
                    } else {
                        // set initial duration
                        glowingTickData = 10 * 20 to 10 * 20
                        tasks += repeatingTask(1) {
                            val (ticksLeft, totalTicks) = glowingTickData
                            glowingBossBar.progress(Math.clamp(ticksLeft / totalTicks.toFloat(), 0.0F, 1.0F))

                            if (ticksLeft == 0) {
                                remainingPlayers().forEach {
                                    it.isGlowing = false
                                    it.hideBossBar(glowingBossBar)
                                }
                                cancel()
                            } else {
                                glowingTickData = ticksLeft - 1 to totalTicks
                            }
                        }
                    }

                    announceDonationEvent("<game_colour>ᴀʟʟ ᴘʟᴀʏᴇʀs ᴀʀᴇ ɴᴏᴡ ɢʟᴏᴡɪɴɢ (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})".style())

                } else {
                    remainingPlayers().forEach {
                        it.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.NAUSEA,
                                PotionEffect.INFINITE_DURATION,
                                1,
                                false,
                                false,
                                false
                            )
                        )
                        it.showBossBar(nauseatedBossBar)
                    }

                    if (nauseaEnabled()) {
                        // extend duration if already active
                        nauseatedTickData = nauseatedTickData.let { it.first + (10 * 20) to it.second + (10 * 20) }
                    } else {
                        // set initial duration
                        nauseatedTickData = 10 * 20 to 10 * 20
                        tasks += repeatingTask(1) {
                            val (ticksLeft, totalTicks) = nauseatedTickData
                            nauseatedBossBar.progress(Math.clamp(ticksLeft / totalTicks.toFloat(), 0.0F, 1.0F))

                            if (ticksLeft == 0) {
                                remainingPlayers().forEach {
                                    it.clearActivePotionEffects()
                                    it.hideBossBar(nauseatedBossBar)
                                }
                                cancel()
                            } else {
                                nauseatedTickData = ticksLeft - 1 to totalTicks
                            }
                        }
                    }

                    announceDonationEvent("<game_colour>ᴀʟʟ ᴘʟᴀʏᴇʀs ᴀʀᴇ ɴᴀᴜsᴇᴀᴛᴇᴅ (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})".style())
                }
            }

            DonationTier.MEDIUM -> {
                announceDonationEvent("<game_colour>ᴀʟʟ ᴘʟᴀʏᴇʀs ʜᴀᴠᴇ ʙᴇᴇɴ ʀᴇɢʀᴏᴜᴘᴇᴅ (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})".style())
                remainingPlayers().forEach { it.teleport(MapSinglePoint(207, 70, 315, 180, 0)) }
            }

            DonationTier.HIGH -> {
                remainingPlayers().forEach {
                    // find the golden hoe in the player's inventory
                    val goldenHoe = it.inventory.contents.firstOrNull { item -> item?.type == Material.GOLDEN_HOE }
                    goldenHoe?.addUnsafeEnchantment(Enchantment.UNBREAKING, 1)
                    goldenHoe?.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE)

                    it.showBossBar(rapidFireBossBar)
                }

                if (rapidFireEnabled()) {
                    // extend duration if already active
                    rapidFireTickData = rapidFireTickData.let { it.first + (20 * 20) to it.second + (20 * 20) }
                } else {
                    // set initial duration
                    rapidFireTickData = 20 * 20 to 20 * 20
                    tasks += repeatingTask(1) {
                        val (ticksLeft, totalTicks) = rapidFireTickData
                        rapidFireBossBar.progress(Math.clamp(ticksLeft / totalTicks.toFloat(), 0.0F, 1.0F))

                        if (ticksLeft == 0) {
                            cancel()
                            remainingPlayers().forEach {
                                val goldenHoe = it.inventory.contents.firstOrNull { item -> item?.type == Material.GOLDEN_HOE }
                                goldenHoe?.removeEnchantment(Enchantment.UNBREAKING)
                                it.hideBossBar(rapidFireBossBar)
                            }
                        } else {
                            rapidFireTickData = ticksLeft - 1 to totalTicks
                        }
                    }
                }

                announceDonationEvent("<game_colour>ʀᴀᴘɪᴅ ꜰɪʀᴇ ɪs ᴏɴ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})".style())
            }
        }
    }
}
