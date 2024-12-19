package gg.flyte.christmas.minigame.games

import dev.shreyasayyengar.menuapi.menu.MenuItem
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
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.*
import kotlin.random.Random

class BaubleTag : EventMiniGame(GameConfig.BAUBLE_TAG) {
    private val baubleTextureURLs = listOf(
        "940da99ea4718907f17190eb15352c0da0de0cee186d4fabf6158f81926a504f",
        "2db24dc262631663ba1e3e3398645013dc5cd2331ec9b9f3eb26855a0b104baa",
        "1e3ad039e903e30f90daa68cebfc5cee72b5ed84d6044382409c67f374d1732b",
        "e258b0b460dee9e67b59f69808caa5db4665969b4b30af43d0e086a133645318",
        "e040b03876580350dbf81333aea696a6d2f3f7d5156fb0ce25771283df609a9f"
    )
    private lateinit var baubleForRound: ItemStack
    private val regroupPoint = MapSinglePoint(205.5, 127, 1271.5)
    private val taggedPlayers = mutableListOf<UUID>()
    private var regroup = false
    private var secondsForRound = 60
    private var roundNumber = 0
    private var taggerWalkSpeed = 0.4F
    private var runnerWalkSpeed = 0.3F

    // ticks left | total ticks
    private var glowingTickData: Pair<Int, Int> = 0 to 0
    private var doubleSpeedTickData: Pair<Int, Int> = 0 to 0

    // lateinit since <game_colour> is not mapped yet at time of init
    private lateinit var glowingBossBar: BossBar
    private lateinit var doubleSpeedBossBar: BossBar

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())
        player.walkSpeed = runnerWalkSpeed
    }

    override fun startGame() {
        simpleCountdown {
            newRound()
            manageActionBars()
            donationEventsEnabled = true

            glowingBossBar = BossBar.bossBar("<game_colour><b>ɢʟᴏᴡɪɴɢ ᴇɴᴀʙʟᴇᴅ".style(), 1.0F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
            doubleSpeedBossBar = BossBar.bossBar("<game_colour><b>ᴅᴏᴜʙʟᴇ sᴘᴇᴇᴅ".style(), 1.0F, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS)
        }
    }

    private fun newRound() {
        taggedPlayers.clear()
        baubleForRound = randomBaubleItem()

        val remaining = remainingPlayers()
        val newTaggers =
            if (remaining.size <= 2) listOf(remaining.random()) else remaining.shuffled().take((remaining.size * 0.3).toInt().coerceAtLeast(1))

        newTaggers.forEach { tagPlayer(it) }

        if (remainingPlayers().size <= 10) regroup = true

        if (regroup) remainingPlayers().forEach { it.teleport(regroupPoint) }

        if (secondsForRound != 10) secondsForRound = 60 - roundNumber * 10

        tasks += repeatingTask(1, TimeUnit.SECONDS) {
            Bukkit.getOnlinePlayers().forEach {
                val isTagger = taggedPlayers.contains(it.uniqueId)
                eventController().sidebarManager.updateLines(
                    it, listOf(
                        Component.empty(),
                        "<aqua>sʜᴀᴛᴛᴇʀs ɪɴ: <red><b>${secondsForRound}".style(),
                        Component.empty(),
                        "<yellow>ɢᴏᴀʟ: <b>${if (isTagger) "<red>ᴛᴀɢ sᴏᴍᴇᴏɴᴇ" else "<game_colour>ʀᴜɴ ᴀᴡᴀʏ"}".style(),
                    )
                )
            }

            if (secondsForRound == 0) {
                tasks += delay(100) { roundNumber++; newRound() }
                taggedPlayers.forEach { eliminate(Bukkit.getPlayer(it)!!, EliminationReason.ELIMINATED) }
                cancel()
                return@repeatingTask
            }

            secondsForRound--
        }
        tasks += repeatingTask(5) {
            taggedPlayers.map { Bukkit.getPlayer(it)!! }.forEach { tagger ->
                tagger.world.spawnParticle(Particle.DUST, tagger.location.clone().add(0.0, 2.75, 0.0), 20, Particle.DustOptions(Color.RED, 1F))
                tagger.compassTarget = tagger.location.getNearbyPlayers(500.0, 500.0, 500.0) { it != tagger }.first().location
            }
        }
    }

    override fun eliminate(player: Player, reason: EliminationReason) {
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) { it.sendMessage("<red>${player.name} <grey>ʜᴀѕ ʙᴇᴇɴ ᴇʟɪᴍɪɴᴀᴛᴇᴅ!".style()) }
        player.walkSpeed = 0.2F
        player.world.createExplosion(player.location, 3F, false, false)
        player.world.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1F, 1F)
        player.world.spawnParticle(Particle.BLOCK, player.eyeLocation, 10000, 0.5, 0.5, 0.5, Bukkit.createBlockData(Material.GLASS))
        super.eliminate(player, reason)
        when (remainingPlayers().size) {
            1 -> {
                formattedWinners[player.uniqueId] = "2ɴᴅ ᴘʟᴀᴄᴇ!"
                formattedWinners[remainingPlayers().first().uniqueId] = "1ѕᴛ ᴘʟᴀᴄᴇ!"

                // formattedWinners currently have keys in order of elimination, reverse it to get actual winners.
                LinkedHashMap(formattedWinners.toList().asReversed().toMap()).apply {
                    formattedWinners.clear()
                    formattedWinners.putAll(this)
                }
                endGame()
            }

            2 -> formattedWinners[player.uniqueId] = "3ѕᴛ ᴘʟᴀᴄᴇ!"
        }
    }

    override fun endGame() {
        donationEventsEnabled = false

        eventController().addPoints(remainingPlayers().first().uniqueId, 15)
        Util.runAction(PlayerType.PARTICIPANT) {
            it.walkSpeed = 0.2F
            it.hideBossBar(glowingBossBar)
            it.hideBossBar(doubleSpeedBossBar)
            it.isGlowing = false
        }

        super.endGame()
    }

    private fun tagPlayer(newTagger: Player, oldTagger: Player? = null) {
        if (remainingPlayers().none { it.uniqueId == newTagger.uniqueId }) return
        if (oldTagger != null && remainingPlayers().none { it.uniqueId == oldTagger.uniqueId }) return

        this.taggedPlayers.add(newTagger.uniqueId)

        if (oldTagger != null) {
            this.taggedPlayers.remove(oldTagger.uniqueId)
            oldTagger.sendMessage("<game_colour>ʏᴏᴜ ʜᴀᴠᴇ ᴛᴀɢɢᴇᴅ <red>${newTagger.name}!".style())
            oldTagger.formatInventory()

            oldTagger.clearActivePotionEffects()
            oldTagger.walkSpeed = runnerWalkSpeed // use walkSpeed rather than potions to prevent stephen-like FOV changes

            newTagger.sendMessage("<red>ʏᴏᴜ ʜᴀᴠᴇ ʙᴇᴇɴ ᴛᴀɢɢᴇᴅ ʙʏ <game_colour>${oldTagger.name}!".style())
        } else newTagger.sendMessage("« <red><b>ʏᴏᴜ <game_colour>ʜᴀᴠᴇ ѕᴛᴀʀᴛᴇᴅ ᴛʜɪѕ ʀᴏᴜɴᴅ ʙᴇɪɴɢ <red>ᴛʜᴇ ɪᴛ!<reset> »".style())

        newTagger.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
        newTagger.formatInventory()
        newTagger.inventory.setItem(0, baubleForRound)
        newTagger.inventory.setItem(1, getPointer())
        newTagger.equipment.helmet = baubleForRound

        newTagger.clearActivePotionEffects()
        delay(20) { newTagger.walkSpeed = taggerWalkSpeed }

        newTagger.world.spawn(newTagger.location, Firework::class.java) {
            it.fireworkMeta = it.fireworkMeta.apply {
                power = 2
                addEffect(
                    FireworkEffect.builder()
                        .with((FireworkEffect.Type.BURST))
                        .withColor(Color.GREEN, Color.RED)
                        .build()
                )
            }

            it.detonate()
        }
    }

    private fun manageActionBars() {
        tasks += repeatingTask(40) {
            remainingPlayers().forEach { player ->
                if (taggedPlayers.contains(player.uniqueId)) {
                    player.sendActionBar("<game_colour>ʏᴏᴜ'ʀᴇ ɪᴛ. <red>ᴛᴀɢ sᴏᴍᴇᴏɴᴇ!".style())
                } else {
                    player.sendActionBar("<game_colour>ʀᴜɴ ᴀᴡᴀʏ!".style())
                }
            }
        }
    }

    private fun randomBaubleItem(): ItemStack {
        return MenuItem(ItemStack(Material.PLAYER_HEAD).apply {
            (itemMeta as SkullMeta).apply {
                displayName("<!i><game_colour><b>ʙᴀᴜʙʟᴇ".style())
            }
        }).setSkullTexture(baubleTextureURLs.random()).itemStack
    }

    private fun getPointer(): ItemStack {
        return ItemStack(Material.COMPASS).apply {
            itemMeta = (itemMeta as CompassMeta).apply {
                displayName("<!i><b><game_colour>ᴘʟᴀʏᴇʀ ᴘᴏɪɴᴛᴇʀ".style())
            }
        }
    }

    private fun glowEnabled() = glowingTickData.first > 0

    private fun doubleSpeedEnabled() = doubleSpeedTickData.first > 0

    override fun handleGameEvents() {
        listeners += event<EntityDamageEvent>(priority = EventPriority.HIGHEST) {
            // return@event -> already cancelled by lower priority [HousekeepingEventListener]

            if (entity !is Player) return@event
            val damager = (this as? EntityDamageByEntityEvent)?.damager as? Player ?: return@event

            // allows an actual hit to go happen
            if (remainingPlayers().contains(damager)) {
                isCancelled = false
                damage = 0.0
            }

            if (taggedPlayers.contains(damager.uniqueId)) {
                if (taggedPlayers.contains(entity.uniqueId)) {
                    return@event
                } else {
                    tagPlayer(entity as Player, damager)
                }
            }
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
                        // extent duration if already active
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

                    val message =
                        "<green>+<red>10</red> <game_colour>sᴇᴄᴏɴᴅs ᴏꜰ ɢʟᴏᴡ ᴀᴘᴘʟɪᴇᴅ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())
                } else {
                    fun forceApplySpeed() {
                        remainingPlayers().forEach { it.walkSpeed = if (taggedPlayers.contains(it.uniqueId)) taggerWalkSpeed else runnerWalkSpeed }
                    }

                    this.taggerWalkSpeed = 0.6F
                    this.runnerWalkSpeed = 0.5F
                    forceApplySpeed()
                    remainingPlayers().forEach { it.showBossBar(doubleSpeedBossBar) }

                    if (doubleSpeedEnabled()) {
                        // extent duration if already active
                        doubleSpeedTickData = doubleSpeedTickData.let { it.first + (10 * 20) to it.second + (10 * 20) }
                    } else {
                        // set initial duration
                        doubleSpeedTickData = 10 * 20 to 10 * 20

                        tasks += repeatingTask(1) {
                            val (ticksLeft, totalTicks) = doubleSpeedTickData
                            doubleSpeedBossBar.progress(Math.clamp(ticksLeft / totalTicks.toFloat(), 0.0F, 1.0F))

                            if (ticksLeft == 0) {
                                forceApplySpeed()
                                cancel()
                            } else {
                                doubleSpeedTickData = ticksLeft - 1 to totalTicks
                            }
                        }
                    }

                    val message =
                        "<green>+<red>10</red> <game_colour>sᴇᴄᴏɴᴅs ᴏꜰ ᴅᴏᴜʙʟᴇ sᴘᴇᴇᴅ ᴀᴘᴘʟɪᴇᴅ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())
                }
            }

            DonationTier.MEDIUM -> {
                if (Random.nextBoolean()) {
                    remainingPlayers().forEach { it.teleport(regroupPoint) }
                    val message =
                        "<game_colour>ᴘʟᴀʏᴇʀѕ ʜᴀᴠᴇ ʙᴇᴇɴ <red>ʀᴇɢʀᴏᴜᴘᴇᴅ! (${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                    announceDonationEvent(message.style())
                } else {
                    // randomly swap a tagged player for a runner
                    val tagged = remainingPlayers().filter { taggedPlayers.contains(it.uniqueId) }
                    val runner = remainingPlayers().filter { !taggedPlayers.contains(it.uniqueId) }

                    if (tagged.isNotEmpty() && runner.isNotEmpty()) {
                        tagPlayer(runner.random(), tagged.random())
                        val message =
                            "<game_colour>ᴛᴀɢɢᴇᴅ ᴘʟᴀʏᴇʀ'ѕ ʜᴀᴠᴇ ʙᴇᴇɴ <red>ꜱᴡᴀᴘᴘᴇᴅ! <game_colour>(${if (donorName != null) "<aqua>$donorName's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
                        announceDonationEvent(message.style())
                    }
                }
            }

            DonationTier.HIGH -> {
                if (remainingPlayers().size <= 2) return // don't forcibly explode the last two :(

                if (Random.nextBoolean()) {
                    val victim = remainingPlayers().random()

                    // Ensure at least 2 players remain after the event
                    val surroundingVictims = victim.location.getNearbyPlayers(3.0, 3.0, 3.0) { it != victim }
                    surroundingVictims.takeWhile { remainingPlayers().size - surroundingVictims.size - 1 >= 2 } // leave at least 2 players

                    if (surroundingVictims.isNotEmpty()) {
                        surroundingVictims.forEach { eliminate(it, EliminationReason.ELIMINATED) }
                        eliminate(victim, EliminationReason.ELIMINATED)
                    }
                } else {
                    val stephen = remainingPlayers().find { it.uniqueId == UUID.fromString("69e8f7d5-11f9-4818-a3bb-7f237df32949") }
                    if (stephen != null) eliminate(stephen, EliminationReason.ELIMINATED)
                }
            }
        }
    }
}
