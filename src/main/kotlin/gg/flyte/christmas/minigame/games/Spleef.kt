package gg.flyte.christmas.minigame.games

import gg.flyte.christmas.ChristmasEventPlugin
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.RangedAttackGoal
import net.minecraft.world.entity.animal.SnowGolem
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.data.type.Snow
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.*
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.sqrt
import kotlin.random.Random

class Spleef : EventMiniGame(GameConfig.SPLEEF) {
    private var overviewTasks = mutableListOf<TwilightRunnable>()
    private var floorLevelBlocks: List<MapSinglePoint> = listOf(
        Util.fillArena(110, Material.SNOW_BLOCK),
        Util.fillArena(98, Material.SNOW_BLOCK),
        Util.fillArena(86, Material.SNOW_BLOCK)
    ).flatten()

    private var gameTime = 0
    private var doubleJumps = mutableMapOf<UUID, Int>()
    private var unlimitedJumps = false
    private var remainingUnlimitedJumpTicks = 0
    private val unlimitedJumpBar = BossBar.bossBar("<gold><b>UNLIMITED DOUBLE JUMPS".style(), 1.0F, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_6)
    private var unlimitedJumpBarTicks = 0.0F
    private var powerfulSnowballs = false
    private var remainingPowerfulSnowballTicks = 0
    private val snowballBar = BossBar.bossBar("<gold><b>POWERFUL SNOWBALLS".style(), 1.0F, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6)
    private var snowballBarTicks = 0.0F
    private val snowmen = mutableListOf<Snowman>()
    private val bees = mutableListOf<Bee>()
    private var bottomLayerMelted = false

    override fun startGameOverview() {
        super.startGameOverview()

        overviewTasks += repeatingTask(1) {
            while (floorLevelBlocks.random().block.type != Material.AIR) {
                wearDownSnowBlock(floorLevelBlocks.random().block, true)
            }
        }
    }

    override fun preparePlayer(player: Player) {
        player.formatInventory()

        ItemStack(Material.DIAMOND_SHOVEL).apply {
            itemMeta = itemMeta.apply {
                displayName("<!i><game_colour>sɴᴏᴡ sʜᴏᴠᴇʟ!".style())
            }
        }.let { player.inventory.setItem(0, it) }

        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())

        doubleJumps[player.uniqueId] = 3
    }

    override fun startGame() {
        overviewTasks.forEach { it.cancel() }

        donationEventsEnabled = true

        for (block in floorLevelBlocks) block.block.type = Material.SNOW_BLOCK // reset after game overview

        simpleCountdown {
            Util.runAction(PlayerType.PARTICIPANT) {
                it.gameMode = GameMode.SURVIVAL
                it.allowFlight = true
            }

            // prevent snow golems from forming snow layers
            ChristmasEventPlugin.instance.serverWorld.setGameRule(GameRule.MOB_GRIEFING, false)

            manageActionBars()
            addUnlimitedJumpsTask()
            addPowerfulSnowballsTask()
            addSnowmenSetTargetTask()

            tasks += repeatingTask(1) {
                remainingPlayers().forEach {
                    if (it.location.blockY < 70) {
                        if (remainingPlayers().contains(it)) {
                            eliminate(it, EliminationReason.ELIMINATED)
                        }
                    }
                }

                snowmen.toList().forEach { //prevent concurrent modification
                    if (it.location.blockY < 70) {
                        it.remove()
                        snowmen.remove(it)
                    }
                }

                bees.toList().forEach {
                    if (it.location.blockY < 70) {
                        it.remove()
                        bees.remove(it)
                    }
                }
            }

            tasks += repeatingTask(20) {
                gameTime++

                if (gameTime % 30 == 0) {
                    remainingPlayers().forEach {
                        it.sendMessage("<green>+1 ᴘᴏɪɴᴛ ꜰᴏʀ sᴜʀᴠɪᴠɪɴɢ 30 sᴇᴄᴏɴᴅs!".style())
                        eventController().addPoints(it.uniqueId, 1)
                    }
                }

                updateScoreboard()
            }

            tasks += repeatingTask(5) {
                delay((1..15).random()) {
                    wearDownSnowBlock(floorLevelBlocks.filter { it.block.type != Material.AIR }.random().block)
                }
            }
        }
    }

    override fun eliminate(player: Player, reason: EliminationReason) {
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
            it.sendMessage("<red>${player.name} <grey>has been eliminated!".style())
        }

        player.allowFlight = false
        player.hideBossBar(unlimitedJumpBar)
        player.hideBossBar(snowballBar)
        doubleJumps.remove(player.uniqueId)

        if (player.gameMode != GameMode.SPECTATOR) {
            Util.runAction(PlayerType.PARTICIPANT, PlayerType.PARTICIPANT) {
                it.playSound(Sound.ENTITY_ITEM_BREAK)
            }
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

        val value = "$gameTime ѕᴇᴄᴏɴᴅ${if (gameTime > 1) "ѕ" else ""}"
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
        //undo what was done in startGame()
        ChristmasEventPlugin.instance.serverWorld.setGameRule(GameRule.MOB_GRIEFING, true)

        for (point in floorLevelBlocks) point.block.type = Material.AIR

        remainingPlayers().forEach {
            it.allowFlight = false
            unlimitedJumpBar.removeViewer(it)
            snowballBar.removeViewer(it)
        }

        snowmen.forEach { it.remove() }
        bees.forEach { it.remove() }

        super.endGame()
    }

    private fun updateScoreboard() {
        val playersLeft = "<aqua>ᴘʟᴀʏᴇʀs ʟᴇꜰᴛ: <red><b>${remainingPlayers().size}".style()
        Bukkit.getOnlinePlayers().forEach { eventController().sidebarManager.updateLines(it, listOf(Component.empty(), playersLeft)) }
    }

    private fun manageActionBars() {
        tasks += repeatingTask(10) {
            remainingPlayers().forEach {
                if (unlimitedJumps) {
                    it.sendActionBar("<gold><b>UNLIMITED<reset> <game_colour>ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs!".style())
                } else if (doubleJumps[it.uniqueId]!! > 0) {
                    it.sendActionBar("<green><b>${doubleJumps[it.uniqueId]!!} <reset><game_colour>ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs ʟᴇꜰᴛ!".style())
                } else {
                    it.sendActionBar("<red><b>0 <reset><game_colour>ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘs ʟᴇꜰᴛ!".style())
                }
            }
        }
    }

    // todo tasks events
    private fun addUnlimitedJumpsTask() {
        tasks += repeatingTask(1) {
            if (!unlimitedJumps && remainingUnlimitedJumpTicks > 0) {
                unlimitedJumps = true

                remainingPlayers().forEach {
                    it.allowFlight = true
                    it.showBossBar(unlimitedJumpBar)
                }
            }

            if (!unlimitedJumps) return@repeatingTask

            unlimitedJumpBar.progress(Math.clamp(remainingUnlimitedJumpTicks / unlimitedJumpBarTicks, 0.0F, 1.0F))

            if (remainingUnlimitedJumpTicks-- <= 0) {
                unlimitedJumps = false
                unlimitedJumpBarTicks = 0.0F

                remainingPlayers().forEach {
                    it.allowFlight = doubleJumps[it.uniqueId]!! > 0
                    unlimitedJumpBar.removeViewer(it)
                }
            }
        }
    }

    private fun addPowerfulSnowballsTask() {
        tasks += repeatingTask(1) {
            if (!powerfulSnowballs && remainingPowerfulSnowballTicks > 0) {
                powerfulSnowballs = true

                remainingPlayers().forEach {
                    it.showBossBar(snowballBar)
                }
            }

            if (!powerfulSnowballs) return@repeatingTask

            snowballBar.progress(Math.clamp(remainingPowerfulSnowballTicks / snowballBarTicks, 0.0F, 1.0F))

            if (remainingPowerfulSnowballTicks-- <= 0) {
                powerfulSnowballs = false
                snowballBarTicks = 0.0F

                remainingPlayers().forEach {
                    snowballBar.removeViewer(it)
                }
            }
        }
    }

    private fun addSnowmenSetTargetTask() {
        tasks += repeatingTask(1) {
            snowmen.removeIf(Snowman::isDead)
            bees.removeIf(Bee::isDead)

            snowmen.forEach {
                val target = remainingPlayers().minByOrNull { player ->
                    player.location.distance(it.location)
                }

                if (target != null) {
                    it.target = target
                }
            }
        }
    }
    // todo tasks events

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

            spawnSnowParticles(block)
        }

        listeners += event<PlayerToggleFlightEvent> {
            if (player.gameMode != GameMode.SURVIVAL) return@event
            isCancelled = true

            if (unlimitedJumps) {
                doubleJump(player)
            } else if (doubleJumps[player.uniqueId]!! > 0) {
                doubleJump(player)

                doubleJumps[player.uniqueId] = doubleJumps[player.uniqueId]!! - 1
            } else {
                player.allowFlight = false
            }
        }

        listeners += event<ProjectileLaunchEvent> {
            if (entity !is Snowball) return@event

            if (powerfulSnowballs) {
                entity.velocity = entity.velocity.multiply(2)
            }
        }

        listeners += event<ProjectileHitEvent> {
            if (hitEntity != null && entity.shooter !is Player) { //Snowman or snowball rain
                isCancelled = true // make snowballs fly through entities to increase chances of spleefing
            }

            if (hitBlock == null) return@event
            if (entity !is Snowball) return@event

            if (floorLevelBlocks.any { it.block == hitBlock }) {
                if (powerfulSnowballs) {
                    hitBlock!!.type = Material.AIR
                    spawnSnowParticles(hitBlock!!)
                } else {
                    wearDownSnowBlock(hitBlock!!)
                }
            }

            // implies snowball source is snowball rain
            if (entity.shooter == null) {
                isCancelled = true // make snowball rain destroy multiple blocks
                entity.location.y -= 1
            }
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> lowTierDonation(donorName)
            DonationTier.MEDIUM -> midTierDonation(donorName)
            DonationTier.HIGH -> highTierDonation(donorName)
        }
    }

    private fun lowTierDonation(donorName: String?) {
        fun doExtraDoubleJumps(name: String?) {
            val increase = (1..2).random()
            val plural = if (increase > 1) "s" else ""

            remainingPlayers().forEach {
                doubleJumps[it.uniqueId] = doubleJumps[it.uniqueId]!! + increase
                it.allowFlight = true

                if (name != null) {
                    it.sendMessage("<green>+<red>$increase</red> ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘ$plural! (<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ)".style())
                } else {
                    it.sendMessage("<green>+<red>$increase</red> ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘ$plural! (ᴅᴏɴᴀᴛɪᴏɴ)".style())
                }
            }
        }

        fun doUnlimitedDoubleJumps(name: String?) {
            remainingUnlimitedJumpTicks += 20 * 5
            unlimitedJumpBarTicks += 20 * 5

            remainingPlayers().forEach {
                if (name != null) {
                    it.sendMessage("<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴜɴʟɪᴍɪᴛᴇᴅ ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘ! (<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ)".style())
                } else {
                    it.sendMessage("<green>+<red>5</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴜɴʟɪᴍɪᴛᴇᴅ ᴅᴏᴜʙʟᴇ ᴊᴜᴍᴘ! (ᴅᴏɴᴀᴛɪᴏɴ)".style())
                }
            }
        }

        fun doPowerfulSnowballs(name: String?) {
            remainingPowerfulSnowballTicks += 20 * 10
            snowballBarTicks += 20 * 10

            remainingPlayers().forEach {
                if (name != null) {
                    it.sendMessage("<green>+<red>10</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴘᴏᴡᴇʀꜰᴜʟ sɴᴏᴡʙᴀʟʟs! (<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ)".style())
                } else {
                    it.sendMessage("<green>+<red>10</red> sᴇᴄᴏɴᴅs ᴏꜰ ᴘᴏᴡᴇʀꜰᴜʟ sɴᴏᴡʙᴀʟʟs! (ᴅᴏɴᴀᴛɪᴏɴ)".style())
                }
            }
        }

        when ((0..2).random()) {
            0 -> doExtraDoubleJumps(donorName)
            1 -> doUnlimitedDoubleJumps(donorName)
            2 -> doPowerfulSnowballs(donorName)
        }
    }

    private fun midTierDonation(donorName: String?) {
        if (Random.nextBoolean()) doSpawnSnowGolem(donorName, (0..2).random() == 0)
        else doSnowballRain(donorName)
    }

    private fun highTierDonation(donorName: String?) {
        var random = Random.nextBoolean()

        if (bottomLayerMelted) random = false

        if (random) {
            doMeltBottomLayer(donorName)
        } else {
            doSnowballRain(donorName)
            doSpawnSnowGolem(donorName, true)
        }
    }

    private fun doubleJump(player: Player) {
        player.isFlying = false
        player.velocity = player.location.direction.multiply(0.5).add(Vector(0.0, 1.25, 0.0))

        player.playSound(Sound.ENTITY_BREEZE_SHOOT)
    }

    private fun wearDownSnowBlock(block: Block, gradual: Boolean = false) {
        when (block.type) {
            Material.SNOW_BLOCK -> {
                block.type = Material.SNOW

                if (block.blockData !is Snow) {
                    return
                }

                block.blockData = (block.blockData as Snow).apply {
                    layers = if (gradual) 7 else 5
                }

                spawnSnowParticles(block)
            }

            Material.SNOW -> {
                val blockData = block.blockData as Snow

                if (blockData.layers <= 2) {
                    block.type = Material.AIR
                } else {
                    blockData.layers -= 1.coerceAtMost(if (gradual) (2..3).random() else 3)
                    block.blockData = blockData
                }

                spawnSnowParticles(block)
            }

            else -> return
        }
    }

    private fun spawnSnowParticles(block: Block) {
        block.world.spawnParticle(
            Particle.SNOWFLAKE,
            block.location.clone().add(0.5, 0.5, 0.5),
            30,
            0.2,
            0.2,
            0.2,
            0.1
        )
    }

    private fun doSpawnSnowGolem(name: String?, flying: Boolean) {
        val nmsWorld = (ChristmasEventPlugin.instance.serverWorld as CraftWorld).handle

        val snowmanName =
            if (name != null) "<aqua>$name's</aqua> Sɴᴏᴡ Gᴏʟᴇᴍ".style()
            else "<game_colour>${if (flying) "Fʟʏɪɴɢ" else "Aɴɢʀʏ"} Sɴᴏᴡ Gᴏʟᴇᴍ".style()

        val location = gameConfig.spawnPoints.random().randomLocation()
        if (flying) {
            location.y += 10
        }

        CustomSnowGolem(this, nmsWorld, location, flying).spawn().let {
            it.customName(snowmanName)
            it.isCustomNameVisible = true

            it.getAttribute(Attribute.FOLLOW_RANGE)!!.baseValue = 64.0

            if (flying) {
                CustomBee(nmsWorld, location).spawn().let { bee ->
                    bee.isInvisible = true
                    bee.isSilent = true

                    bee.getAttribute(Attribute.MOVEMENT_SPEED)!!.baseValue = 0.5
                    bee.getAttribute(Attribute.FLYING_SPEED)!!.baseValue = 0.5

                    bee.addPassenger(it)
                    bees.add(bee)
                }
            }

            snowmen.add(it)
        }

        val flyingText = if (flying) " ꜰʟʏɪɴɢ" else "ɴ ᴀɴɢʀʏ"
        val message =
            if (name != null) "<green>A$flyingText sɴᴏᴡᴍᴀɴ ʜᴀs ᴊᴏɪɴᴇᴅ ᴛʜᴇ ɢᴀᴍᴇ! (<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ)".style()
            else "<green>A$flyingText sɴᴏᴡᴍᴀɴ ʜᴀs ᴊᴏɪɴᴇᴅ ᴛʜᴇ ɢᴀᴍᴇ! (ᴅᴏɴᴀᴛɪᴏɴ)".style()

        remainingPlayers().forEach { it.sendMessage(message) }
    }

    private fun doSnowballRain(name: String?) {
        val world = ChristmasEventPlugin.instance.serverWorld

        floorLevelBlocks.forEach {
            val location = it.block.location
            location.y = 150.0 + (0..20).random().toDouble()

            if ((0..9).random() == 0) {
                world.spawn(location, Snowball::class.java).shooter = null
            }
        }

        val message =
            if (name != null) "<green>A sɴᴏᴡʙᴀʟʟ ʀᴀɪɴ ʜᴀs sᴛᴀʀᴛᴇᴅ! (<aqua>$name's</aqua> ᴅᴏɴᴀᴛɪᴏɴ)".style()
            else "<green>A sɴᴏᴡʙᴀʟʟ ʀᴀɪɴ ʜᴀs sᴛᴀʀᴛᴇᴅ! (ᴅᴏɴᴀᴛɪᴏɴ)".style()

        remainingPlayers().forEach {
            it.playSound(it, Sound.WEATHER_RAIN, 1.0F, 0.5F)
            it.sendMessage(message)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun doMeltBottomLayer(name: String?) {
        bottomLayerMelted = true
        var countdown = 5

        val meltedText =
            if (name != null) "<red><b>Tʜᴇ ʙᴏᴛᴛᴏᴍ ʟᴀʏᴇʀ ᴡᴀs ᴍᴇʟᴛᴇᴅ ʙʏ <aqua>$name</aqua>!".style()
            else "<red><b>Tʜᴇ ʙᴏᴛᴛᴏᴍ ʟᴀʏᴇʀ ʜᴀs ᴍᴇʟᴛᴇᴅ!".style()

        tasks += repeatingTask(20) {
            if (countdown == 0) {
                cancel()
                remainingPlayers().forEach { it.sendMessage(meltedText) }
            } else {
                remainingPlayers().forEach {
                    it.sendMessage("<red><b>Tʜᴇ ʙᴏᴛᴛᴏᴍ ʟᴀʏᴇʀ ᴡɪʟʟ ᴍᴇʟᴛ ɪɴ <aqua>$countdown</aqua> sᴇᴄᴏɴᴅs!".style())
                }
                countdown--
            }
        }

        //start of gpt code
        val blocksToDestroy = mutableListOf<Block>()
        var sectionSize = 0

        GlobalScope.launch(Dispatchers.IO) {
            floorLevelBlocks.forEach {
                if (it.block.y == 86) {
                    blocksToDestroy.add(it.block)
                }
            }

            sectionSize = blocksToDestroy.size / 4
        }

        var currentIndex = 0
        for (i in 0 until 4) {
            tasks += delay(120 + i) {
                blocksToDestroy.subList(currentIndex, currentIndex + sectionSize).forEach {
                    it.breakNaturally()
                    spawnSnowParticles(it)
                }

                currentIndex += sectionSize
            }
        }
    }

    private class CustomSnowGolem(private val game: Spleef, private val world: Level, location: Location, private val withMount: Boolean) :
        SnowGolem(EntityType.SNOW_GOLEM, world) {
        init {
            setPos(location.x, location.y, location.z)
        }

        fun spawn(): Snowman {
            world.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)

            return bukkitEntity as Snowman
        }

        override fun registerGoals() {
            super.registerGoals()

            val range = if (withMount) 18.0F else 12.0F
            val attackGoal = RangedAttackGoal(this, 2.25, 2, range)

            goalSelector.removeAllGoals { goal -> goal is RangedAttackGoal }
            goalSelector.addGoal(1, attackGoal)
        }

        override fun performRangedAttack(target: LivingEntity, pullProgress: Float) {
            if (!onGround && !withMount) return

            val dx = target.x - x
            val dz = target.z - z
            val targetY = target.y - 1.6 // different from the original method; aim at feet instead of eyes

            val extraY = sqrt(dx * dx + dz * dz) * (if (game.powerfulSnowballs) 0.1 else 0.2)
            val world = level()

            if (world is ServerLevel) {
                val item = Items.SNOWBALL.defaultInstance
                val snowball = net.minecraft.world.entity.projectile.Snowball(world, this, item)

                Projectile.spawnProjectile(snowball, world, item) { snowballEntity ->
                    val dy = targetY - snowballEntity.y + extraY
                    snowballEntity.shoot(dx, dy, dz, 1.6f, 10.0f)
                }
            }

            playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0f, 0.4f / (getRandom().nextFloat() * 0.4f + 0.8f))
        }
    }

    private class CustomBee(private val world: Level, location: Location) : net.minecraft.world.entity.animal.Bee(EntityType.BEE, world) {
        init {
            setPos(location.x, location.y, location.z)
        }

        fun spawn(): Bee {
            world.addFreshEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)

            return bukkitEntity as Bee
        }

        override fun getFlyingSpeed(): Float {
            return this.speed * 0.1F
        }
    }
}
