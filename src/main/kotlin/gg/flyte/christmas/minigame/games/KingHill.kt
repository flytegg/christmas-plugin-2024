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
import gg.flyte.twilight.extension.hidePlayer
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.extension.showPlayer
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

class KingHill : EventMiniGame(GameConfig.KING_OF_THE_HILL) {
    private var hillRegion = MapRegion(MapSinglePoint(824, 85, 633), MapSinglePoint(830, 88, 627))
    private var pvpEnabled = false
    private var gameTime = 150
    private val respawnBelow = 60
    private val timeOnHill = mutableMapOf<UUID, Int>()

    private var delayedKbTicksTotal = 0
    private var delayedKbTicksLeft = -1

    private var thrownAroundTicksTotal = 0
    private var thrownAroundTicksLeft = -1

    private val velocityMap = mutableMapOf<UUID, MutableList<Vector>>()

    private lateinit var delayedKbBossbar: BossBar

    private val doubleJumps = mutableMapOf<UUID, Int>()

    private val invisibleTimeLeft = mutableMapOf<UUID, Int>()

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
                    remainingPlayers().forEach { it.hideBossBar(delayedKbBossbar) }

                    delayedKbTicksLeft = -1

                    remainingPlayers().forEach { player ->
                        val stick = player.inventory.find { it.type == Material.STICK }

                        if (stick == null) {
                            return@forEach
                        }

                        stick.editMeta { it.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE) }
                    }

                    thrownAroundTicksLeft = delayedKbTicksTotal / 6
                    thrownAroundTicksTotal = delayedKbTicksTotal / 6
                    delayedKbTicksTotal = 0
                } else {
                    delayedKbBossbar.progress(delayedKbTicksLeft.toFloat() / delayedKbTicksTotal)
                    delayedKbTicksLeft -= 1
                }
            }

            tasks += repeatingTask(1, TimeUnit.TICKS) {
                if (thrownAroundTicksLeft == 0) {
                    thrownAroundTicksLeft = -1

                    velocityMap.entries.clear()
                    return@repeatingTask
                }

                if (thrownAroundTicksLeft == -1) {
                    return@repeatingTask
                }

                velocityMap.entries.forEach {
                    val player = Bukkit.getPlayer(it.key)!!
                    val vectors = it.value

                    val vectorCount = vectors.size

                    if (vectorCount == 0) {
                        return@forEach
                    }

                    val ticksPassed = thrownAroundTicksTotal - thrownAroundTicksLeft
                    val previousTicksPassed = thrownAroundTicksTotal - (thrownAroundTicksLeft + 1)

                    val ratio = ticksPassed.toDouble() / thrownAroundTicksTotal.toDouble()
                    val previousRatio = previousTicksPassed.toDouble() / thrownAroundTicksTotal.toDouble()

                    val index = ratio * vectorCount
                    val previousIndex = previousRatio * vectorCount

                    val floor = floor(index)
                    val previousFloor = floor(previousIndex)

                    if (floor == previousFloor) {
                        return@forEach
                    }

                    val value = vectors[floor.toInt()]

                    player.velocity = value
                }

                thrownAroundTicksLeft -= 1
            }

            delayedKbBossbar = BossBar.bossBar("<game_colour><b>ᴅᴇʟᴀʏᴇᴅ ᴋɴᴏᴄᴋʙᴀᴄᴋ".style(), 1.0F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)

            manageActionBars()
        }
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
            if (delayedKbTicksLeft <= 0) {
                return@event
            }

            if (entity !is Player) {
                return@event
            }

            val velocityList = velocityMap.computeIfAbsent(entity.uniqueId) { mutableListOf() }

            val damager = damageSource.causingEntity ?: damageSource.directEntity

            if (damager !is Player) {
                return@event
            }

            val damagedLocation = entity.location.toVector()
            val damagerLocation = damager.location.toVector()

            val direction = damagedLocation.subtract(damagerLocation)

            if (direction.lengthSquared() == 0.0) {
                return@event
            }

            val normalized = direction.normalize()

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
            if (damager !is Player) {
                return@event
            }

            val player = damager as Player

            val isInvisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY)

            if (isInvisible) {
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
        val random = (0..3).random()

        when (random) {
            0 -> doAddDoubleJumps(name)
            1 -> doApplySlowFalling(name)
            2 -> doApplyKingsBlindness(name)
            3 -> doApplyJumpBoost(name)
        }
    }

    private fun mediumTierDonation(name: String?) {
        val random = (0..1).random()

        when (random) {
            0 -> doDelayedKnockback(name)
            1 -> doApplyInvisibility(name)
        }
    }

    private fun performDoubleJump(player: Player) {
        player.velocity = player.location.direction.multiply(0.5).add(Vector(0.0, 1.25, 0.0))
        player.playSound(Sound.ENTITY_BREEZE_SHOOT)
    }

    private fun doDelayedKnockback(name: String?) {
        if (delayedKbTicksLeft == -1) delayedKbTicksLeft = 0
        if (delayedKbTicksTotal == -1) delayedKbTicksTotal = 0

        delayedKbTicksLeft += 20 * 5
        delayedKbTicksTotal += 20 * 5

        remainingPlayers().forEach { player ->
            player.showBossBar(delayedKbBossbar)

            val stick = player.inventory.find { it.type == Material.STICK }

            if (stick == null) {
                return@forEach
            }

            @Suppress("UnstableApiUsage")
            val modifier = AttributeModifier(
                NamespacedKey(ChristmasEventPlugin.instance, "kinghill_knockback_resistance"),
                1.0,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY
            )

            stick.editMeta {
                if (it.getAttributeModifiers(Attribute.KNOCKBACK_RESISTANCE)?.isEmpty() != false) {
                    it.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier)
                }
            }
        }

        val message =
            "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴅᴇʟᴀʏᴇᴅ ᴋɴᴏᴄᴋʙᴀᴄᴋ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())
    }

    private fun doAddDoubleJumps(name: String?) {
        val amount = (3..5).random()

        val message = "<green>+<red>$amount</red> ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())

        remainingPlayers().forEach {
            val doubleJumpCount = doubleJumps.computeIfAbsent(it.uniqueId) { 0 }

            it.allowFlight = true

            doubleJumps[it.uniqueId] = doubleJumpCount + amount
        }
    }

    private fun doShufflePositions(name: String?) {
        var timeLeftSeconds = 5

        tasks += repeatingTask(0, 1, TimeUnit.SECONDS) {
            val message = "<green>sʜᴜꜰꜰʟɪɴɢ ᴘᴏsɪᴛɪᴏɴs ɪɴ <red>$timeLeftSeconds</red> sᴇᴄᴏɴᴅs! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
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

        tasks += repeatingTask(1, TimeUnit.TICKS) {
            remainingPlayers().forEach {
                val invisibleTimeRemaining = invisibleTimeLeft[it.uniqueId] ?: return@repeatingTask

                if (invisibleTimeRemaining == 0) {
                    it.showPlayer()
                } else {
                    invisibleTimeLeft[it.uniqueId] = invisibleTimeRemaining - 1
                }
            }
        }
    }

    private fun doApplySlowFalling(name: String?) {
        val message = "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ sʟᴏᴡ ꜰᴀʟʟɪɴɢ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())

        remainingPlayers().forEach {
            val duration = it.getPotionEffect(PotionEffectType.SLOW_FALLING)?.duration ?: 0

            it.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, duration + 5 * 20, 0))
        }
    }

    private fun doApplyKingsBlindness(name: String?) {
        val kingUuid = timeOnHill.entries
            .filter { Bukkit.getPlayer(it.key) != null }
            .filter {
                hillRegion.contains(Bukkit.getPlayer(it.key)!!.location)
            }
            .minByOrNull { -it.value }?.key

        if (kingUuid == null) {
            lowTierDonation(name)
            return
        }

        val king = Bukkit.getPlayer(kingUuid) ?: return

        val duration = king.getPotionEffect(PotionEffectType.BLINDNESS)?.duration ?: 0

        king.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, duration + 5 * 20, 0))

        val message = "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴋɪɴɢ's ʙʟɪɴᴅɴᴇss! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())
    }

    private fun doApplyJumpBoost(name: String?) {
        val message = "<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴊᴜᴍᴘ ʙᴏᴏsᴛ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())

        remainingPlayers().forEach {
            val duration = it.getPotionEffect(PotionEffectType.JUMP_BOOST)?.duration ?: 0

            it.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, duration + 5 * 20, 1))
        }
    }

    private fun doApplyInvisibility(name: String?) {
        val message = "<green>+<red>8</red> sᴇᴄᴏɴᴅs ᴏꜰ ɪɴᴠɪsɪʙɪʟɪᴛʏ! (${if (name != null) "<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ" else "ᴅᴏɴᴀᴛɪᴏɴ"})"
        announceDonationEvent(message.style())

        remainingPlayers()
            .filter { !hillRegion.contains(it.location) }
            .forEach {
                var timeLeft = invisibleTimeLeft.computeIfAbsent(it.uniqueId) { 0 }
                timeLeft += 8 * 20

                invisibleTimeLeft[it.uniqueId] = timeLeft

                it.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, timeLeft, 0))
                it.hidePlayer()
            }
    }
}
