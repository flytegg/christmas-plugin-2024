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
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.time.Duration
import java.util.*
import kotlin.math.floor
import kotlin.random.Random

class KingHill : EventMiniGame(GameConfig.KING_OF_THE_HILL) {
    private var hillRegion = MapRegion(MapSinglePoint(824, 85, 633), MapSinglePoint(830, 88, 627))
    private var pvpEnabled = false
    private var gameTime = 150
    private val respawnBelow = 60
    private val timeOnHill = mutableMapOf<UUID, Int>()
    private val velocityMap = mutableMapOf<UUID, MutableList<Vector>>()
    private val doubleJumps = mutableMapOf<UUID, Int>()

    // ticks left | total ticks
    private var delayedKnockbackTickData: Pair<Int, Int> = -1 to -1
    private var thrownAroundTickData: Pair<Int, Int> = -1 to -1

    // lateinit since <game_colour> is not mapped yet at time of init
    private lateinit var delayedKnockbackBossBar: BossBar
    private lateinit var thrownAroundBossBar: BossBar

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

            delayedKnockbackBossBar =
                BossBar.bossBar("<game_colour><b>ᴅᴇʟᴀʏᴇᴅ ᴋɴᴏᴄᴋʙᴀᴄᴋ".style(), 1.0F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
            thrownAroundBossBar = BossBar.bossBar("<game_colour><b>ᴛʜʀᴏᴡɴ ᴀʀᴏᴜɴᴅ".style(), 1.0F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)

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

            tasks += repeatingTask(1) {
                val (ticksLeft, totalTicks) = thrownAroundTickData

                if (ticksLeft == 0) {
                    velocityMap.clear()
                    thrownAroundTickData = -1 to -1
                    return@repeatingTask
                }

                if (ticksLeft == -1) return@repeatingTask

                velocityMap.entries.forEach {
                    val player = Bukkit.getPlayer(it.key) ?: return@forEach

                    val vectors = it.value
                    val vectorCount = vectors.size

                    // If a player has not been hit during the delayed KB period,
                    // continue to the next player.
                    if (vectorCount == 0) return@forEach

                    val ticksPassed = totalTicks - ticksLeft
                    val previousTicksPassed = ticksPassed - 1

                    // We calculate how much of the thrown around period has passed (as a ratio).
                    val ratio = ticksPassed.toDouble() / totalTicks.toDouble()
                    val previousRatio = previousTicksPassed.toDouble() / totalTicks.toDouble()

                    // We then get our current and previous positions in the vectors list.
                    val index = ratio * vectorCount
                    val previousIndex = previousRatio * vectorCount

                    val floor = floor(index)
                    val previousFloor = floor(previousIndex)

                    // If the current and previous positions are the same, that means
                    // it is not yet time to apply the next velocity vector to the player's velocity.
                    if (floor == previousFloor) return@forEach

                    // If they are different, that means we are at a new position in the list,
                    // and it is time to apply it.
                    player.velocity = vectors[floor.toInt()]
                }

                thrownAroundTickData = ticksLeft - 1 to totalTicks
            }

            manageActionBars()
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

    private fun manageActionBars() {
        tasks += repeatingTask(10) {
            remainingPlayers().forEach {
                val doubleJumpsCount = doubleJumps.computeIfAbsent(it.uniqueId) { 0 }

                if (doubleJumpsCount > 0) {
                    it.sendActionBar("<green><b>${doubleJumps[it.uniqueId]!!} <reset><game_colour>ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs ʟᴇꜰᴛ!".style())
                } else {
                    it.sendActionBar("<red><b>0 <reset><game_colour>ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs ʟᴇꜰᴛ!".style())
                }
            }
        }
    }

    private fun performDoubleJump(player: Player) {
        player.velocity = player.location.direction.multiply(0.5).add(Vector(0.0, 1.25, 0.0))
        player.playSound(Sound.ENTITY_BREEZE_SHOOT)
    }

    private fun delayedKnockback() = delayedKnockbackTickData.first > 0

    private fun thrownAround() = thrownAroundTickData.first > 0 // unused?

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
            if (!delayedKnockback()) return@event
            if (entity !is Player) return@event

            @Suppress("UnstableApiUsage")
            val damager = damageSource.causingEntity ?: damageSource.directEntity
            if (damager !is Player) return@event

            val damagedLocation = entity.location.toVector()
            val damagerLocation = damager.location.toVector()

            val direction = damagedLocation.subtract(damagerLocation)
            if (direction.lengthSquared() == 0.0) return@event

            val normalized = direction.normalize()

            val velocityList = velocityMap.computeIfAbsent(entity.uniqueId) { mutableListOf() }
            velocityList.add(normalized.multiply(if (damager.inventory.itemInMainHand.type == Material.AIR) 0.5 else 1.5))
        }

        listeners += event<PlayerToggleFlightEvent> {
            if (player.gameMode != GameMode.ADVENTURE) return@event
            isCancelled = true

            val doubleJumpCount = doubleJumps.computeIfAbsent(player.uniqueId) { 0 }

            if (doubleJumpCount > 0) {
                performDoubleJump(player)
                doubleJumps[player.uniqueId] = doubleJumpCount - 1
                player.isFlying = false
            } else {
                player.allowFlight = false
            }
        }

        listeners += event<EntityDamageByEntityEvent> {
            if (damager !is Player) return@event

            val player = damager as Player
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY)
                player.sendMessage("<red>ʏᴏᴜ ʜɪᴛ ᴀ ᴘʟᴀʏᴇʀ sᴏ ʏᴏᴜ ᴀʀᴇ ɴᴏ ʟᴏɴɢᴇʀ ɪɴᴠɪsɪʙʟᴇ!".style())
            }
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> lowTierDonation(donorName)
            DonationTier.MEDIUM -> mediumTierDonation(donorName)
            DonationTier.HIGH -> doShufflePositions(donorName)
        }
    }

    private fun lowTierDonation(name: String?) {
        fun doAddDoubleJumps(name: String?) {
            val amount = (3..5).random()
            remainingPlayers().forEach {
                val doubleJumpCount = doubleJumps.computeIfAbsent(it.uniqueId) { 0 }
                doubleJumps[it.uniqueId] = doubleJumpCount + amount
                it.allowFlight = true
            }

            val message = "<green>+<red>$amount</red> ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            announceDonationEvent(message.style())
        }

        fun doApplySlowFalling(name: String?) {
            val message = "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ sʟᴏᴡ ꜰᴀʟʟɪɴɢ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            announceDonationEvent(message.style())

            remainingPlayers().forEach {
                val duration = it.getPotionEffect(PotionEffectType.SLOW_FALLING)?.duration ?: 0

                it.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, duration + (5 * 20), 0))
            }
        }

        fun doApplyKingsBlindness(name: String?) {
            val kingUniqueId = timeOnHill.entries
                .filter { Bukkit.getPlayer(it.key) != null }
                .filter { hillRegion.contains(Bukkit.getPlayer(it.key)!!.location) }
                .maxByOrNull { -it.value }?.key

            if (kingUniqueId == null) {
                lowTierDonation(name) // couldn't run this donation: divert to another one
                return
            }

            val king = Bukkit.getPlayer(kingUniqueId) ?: return
            val duration = king.getPotionEffect(PotionEffectType.BLINDNESS)?.duration ?: 0

            king.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, duration + (5 * 20), 0))

            val message = "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴋɪɴɢ's ʙʟɪɴᴅɴᴇss! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            announceDonationEvent(message.style())
        }

        fun doApplyJumpBoost(name: String?) {
            val message = "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴊᴜᴍᴘ ʙᴏᴏsᴛ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            announceDonationEvent(message.style())

            remainingPlayers().forEach {
                val duration = it.getPotionEffect(PotionEffectType.JUMP_BOOST)?.duration ?: 0

                it.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, duration + 5 * 20, 1))
            }
        }

        val random = (0..3).random()
        when (random) {
            0 -> doAddDoubleJumps(name)
            1 -> doApplySlowFalling(name)
            2 -> doApplyKingsBlindness(name)
            3 -> doApplyJumpBoost(name)
        }
    }

    private fun mediumTierDonation(name: String?) {
        fun doDelayedKnockback(name: String?) {
            remainingPlayers().forEach { player ->
                player.showBossBar(delayedKnockbackBossBar)

                val stick = player.inventory.find { it.type == Material.STICK }
                if (stick == null) return@forEach

                stick.editMeta {
                    if (it.getAttributeModifiers(Attribute.KNOCKBACK_RESISTANCE)?.isEmpty() != false) {
                        @Suppress("UnstableApiUsage")
                        val modifier = AttributeModifier(
                            NamespacedKey(ChristmasEventPlugin.instance, "kinghill_knockback_resistance"),
                            1.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.ANY
                        )

                        it.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier)
                    }
                }
            }

            if (delayedKnockback()) {
                // extend duration if already active
                delayedKnockbackTickData = delayedKnockbackTickData.let { it.first + (5 * 20) to it.second + (5 * 20) }
            } else {
                // set initial duration
                delayedKnockbackTickData = 5 * 20 to 5 * 20

                tasks += repeatingTask(1) {
                    val (ticksLeft, totalTicks) = delayedKnockbackTickData
                    delayedKnockbackBossBar.progress(Math.clamp(ticksLeft / totalTicks.toFloat(), 0.0F, 1.0F))

                    if (ticksLeft == -1) return@repeatingTask

                    if (ticksLeft == 0) {
                        remainingPlayers().forEach { player ->
                            player.hideBossBar(delayedKnockbackBossBar)

                            val stick = player.inventory.find { it.type == Material.STICK }
                            if (stick == null) return@forEach

                            stick.editMeta { it.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE) }
                            player.playSound(Sound.ENTITY_WITHER_BREAK_BLOCK)
                        }

                        thrownAroundTickData = delayedKnockbackTickData.let { it.second / 6 to it.second / 6 }
                        delayedKnockbackTickData = -1 to -1
                    } else {
                        delayedKnockbackTickData = ticksLeft - 1 to totalTicks
                    }
                }
            }

            val message =
                "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴅᴇʟᴀʏᴇᴅ ᴋɴᴏᴄᴋʙᴀᴄᴋ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            announceDonationEvent(message.style())
        }

        fun doApplyInvisibility(name: String?) {
            val message = "<green>+<red>8</red> sᴇᴄᴏɴᴅs ᴏꜰ ɪɴᴠɪsɪʙɪʟɪᴛʏ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            announceDonationEvent(message.style())

            remainingPlayers()
                .filter { !hillRegion.contains(it.location) }
                .forEach {
                    val duration = it.getPotionEffect(PotionEffectType.INVISIBILITY)?.duration ?: 0
                    it.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, duration + 8 * 20, 0))
                }
        }

        if (Random.nextBoolean()) doDelayedKnockback(name)
        else doApplyInvisibility(name)
    }

    private fun doShufflePositions(name: String?) {
        var timeLeftSeconds = 5

        tasks += repeatingTask(0, 1, TimeUnit.SECONDS) {
            val message =
                "<green>sʜᴜꜰꜰʟɪɴɢ ᴘᴏsɪᴛɪᴏɴs ɪɴ <red>$timeLeftSeconds</red> sᴇᴄᴏɴᴅs! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
            remainingPlayers().forEach {
                it.sendMessage(message.style())
                it.playSound(it, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, if (timeLeftSeconds == 0) 2.0F else 1.0F)
            }

            timeLeftSeconds--

            if (timeLeftSeconds == 0) cancel()
        }

        tasks += delay(timeLeftSeconds, TimeUnit.SECONDS) {
            val players = remainingPlayers()
            val positions = players.map { it.location }

            var shuffled = positions.shuffled()
            while (shuffled == positions || positions.size < 2) {
                shuffled = positions.shuffled()
            }

            shuffled.forEachIndexed { index, position ->
                val player = players[index]

                player.teleport(position)
                player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
            }

            val message = "<green>ᴘᴏsɪᴛɪᴏɴs ʜᴀᴠᴇ ʙᴇᴇɴ sʜᴜꜰꜰʟᴇᴅ!"
            remainingPlayers().forEach {
                it.sendMessage(message.style())
            }
        }
    }
}
