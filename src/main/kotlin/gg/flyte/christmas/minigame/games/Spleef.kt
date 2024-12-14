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
import gg.flyte.twilight.extension.getNearestPlayer
import gg.flyte.twilight.extension.hidePlayer
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.extension.showPlayer
import gg.flyte.twilight.scheduler.TwilightRunnable
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.repeatingTask
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
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.sqrt

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
                displayName("<!i><game_colour>Snow Shovel!".style())
            }
        }.let { player.inventory.setItem(0, it) }

        player.gameMode = GameMode.ADVENTURE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())

        doubleJumps[player.uniqueId] = 3
    }

    override fun startGame() {
        overviewTasks.forEach { it.cancel() }

        donationEventsEnabled = true

        for (point in floorLevelBlocks) {
            point.block.type = Material.SNOW_BLOCK
        } // reset after game overview

        simpleCountdown {
            Util.runAction(PlayerType.PARTICIPANT) {
                it.gameMode = GameMode.SURVIVAL
                it.allowFlight = true
            }

            //prevent snow golems from forming snow layers
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

                if (gameTime % 20 == 0) {
                    remainingPlayers().forEach {
                        it.sendMessage("<green>+1 point for surviving 30 seconds!".style())
                        eventController().addPoints(it.uniqueId, 1)
                    }
                }

                updateScoreboard()
            }

            tasks += repeatingTask(5) {
                delay((1..15).random()) {
                    floorLevelBlocks.filter { it.block.type != Material.AIR }.random().let {
                        wearDownSnowBlock(it.block)
                    }
                }
            }
        }
    }

    override fun eliminate(player: Player, reason: EliminationReason) {
        Util.runAction(PlayerType.PARTICIPANT, PlayerType.OPTED_OUT) {
            it.sendMessage("<red>${player.name} <grey>has been eliminated!".style())
        }

        player.apply {
            allowFlight = false
            unlimitedJumpBar.removeViewer(player)
            snowballBar.removeViewer(player)

            @Suppress("DuplicatedCode") // yes ik but im lazy and can't think of any other animations
            if (reason == EliminationReason.ELIMINATED) {
                if (gameMode != GameMode.SPECTATOR) {
                    Util.runAction(PlayerType.PARTICIPANT, PlayerType.PARTICIPANT) { it.playSound(Sound.ENTITY_ITEM_BREAK) }
                } // don't apply cosmetics if in camera sequence

                addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1, false, false, false))

                val itemDisplay = world.spawn(location, ItemDisplay::class.java) {
                    it.setItemStack(ItemStack(Material.AIR))
                    it.teleportDuration = 59 // max (minecraft limitation)
                }

                addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1, false, false, false))

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
        }

        super.eliminate(player, reason)

        val value = "$gameTime second${if (gameTime > 1) "s" else ""}" // bro imagine surviving a singular amount of seconds
        when (remainingPlayers().size) {
            1 -> {
                formattedWinners[player.uniqueId] = value
                formattedWinners[remainingPlayers().first().uniqueId] = "$value (1st Place!)"
                endGame()
            }

            2 -> formattedWinners[player.uniqueId] = value
        }
    }

    override fun endGame() {
        //undo what was done in startGame()
        ChristmasEventPlugin.instance.serverWorld.setGameRule(GameRule.MOB_GRIEFING, true)

        for (point in floorLevelBlocks) {
            point.block.type = Material.AIR
        }

        remainingPlayers().forEach {
            it.allowFlight = false
            unlimitedJumpBar.removeViewer(it)
            snowballBar.removeViewer(it)
        }

        snowmen.forEach { it.remove() }

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
                    it.sendActionBar("<gold><b>UNLIMITED<reset> <game_colour>double jumps!".style())
                    return@forEach
                }

                if (doubleJumps[it.uniqueId]!! > 0) {
                    it.sendActionBar("<green><b>${doubleJumps[it.uniqueId]!!} <reset><game_colour>double jumps left!".style())
                } else {
                    it.sendActionBar("<red><b>0 <reset><game_colour>double jumps left!".style())
                }
            }
        }
    }

    private fun addUnlimitedJumpsTask() {
        tasks += repeatingTask(1) {
            if (!unlimitedJumps && remainingUnlimitedJumpTicks > 0) {
                unlimitedJumps = true

                remainingPlayers().forEach {
                    it.allowFlight = true
                    it.showBossBar(unlimitedJumpBar)
                }
            }

            if (!unlimitedJumps) {
                return@repeatingTask
            }

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

            if (!powerfulSnowballs) {
                return@repeatingTask
            }

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
                val target = it.getNearestPlayer(64.0, 64.0, 64.0)
                if (target != null) {
                    it.target = target
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
            if (hitEntity != null && entity.shooter !is Player) {
                isCancelled = true // make snowballs fly through entities to increase chances of spleefing
            }

            if (hitBlock == null) return@event
            if (entity !is Snowball) return@event

            if (floorLevelBlocks.any { it.block == hitBlock }) {
                if (powerfulSnowballs) {
                    hitBlock!!.type = Material.AIR
                } else {
                    wearDownSnowBlock(hitBlock!!)
                }
            }

            // implies snowball source is snowball rain
            if (entity.shooter !is Player && entity.shooter !is Snowman) {
                isCancelled = true // make snowball rain destroy multiple blocks
            }
        }

        listeners += event<PlayerDropItemEvent> {
            isCancelled = true
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
            }

            Material.SNOW -> {
                val blockData = block.blockData as Snow

                if (blockData.layers <= 2) {
                    block.type = Material.AIR
                } else {
                    blockData.layers -= if (gradual) (1..2).random() else 3
                    block.blockData = blockData
                }
            }

            else -> return
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
        val random = (0..2).random()

        when (random) {
            0 -> extraDoubleJumps(donorName)
            1 -> unlimitedDoubleJumps(donorName)
            2 -> powerfulSnowballs(donorName)
        }
    }

    private fun midTierDonation(donorName: String?) {
        val random = (0..1).random()

        when (random) {
            0 -> spawnSnowGolem(donorName, (0..2).random() == 0)
            1 -> snowballRain(donorName)
        }
    }

    private fun highTierDonation(donorName: String?) {
        var random = (0..1).random()

        if (bottomLayerMelted) random = 1

        when (random) {
            0 -> meltBottomLayer(donorName)
            1 -> {
                snowballRain(donorName)
                spawnSnowGolem(donorName, true)
            }
        }
    }

    private fun extraDoubleJumps(name: String?) {
        val increase = (1..2).random()
        val plural = if (increase > 1) "s" else ""

        remainingPlayers().forEach {
            doubleJumps[it.uniqueId] = doubleJumps[it.uniqueId]!! + increase
            it.allowFlight = true

            if (name != null) {
                it.sendMessage("<green>+<red>$increase</red> double jump$plural! (<aqua>$name's</aqua> donation)".style())
            } else {
                it.sendMessage("<green>+<red>$increase</red> double jump$plural! (donation)".style())
            }
        }
    }

    private fun unlimitedDoubleJumps(name: String?) {
        remainingUnlimitedJumpTicks += 20 * 5
        unlimitedJumpBarTicks += 20 * 5

        remainingPlayers().forEach {
            if (name != null) {
                it.sendMessage("<green>+<red>5</red> seconds of unlimited double jump! (<aqua>$name's</aqua> donation)".style())
            } else {
                it.sendMessage("<green>+<red>5</red> seconds of unlimited double jump! (donation)".style())
            }
        }
    }

    private fun powerfulSnowballs(name: String?) {
        remainingPowerfulSnowballTicks += 20 * 10
        snowballBarTicks += 20 * 10

        remainingPlayers().forEach {
            if (name != null) {
                it.sendMessage("<green>+<red>10</red> seconds of powerful snowballs! (<aqua>$name's</aqua> donation)".style())
            } else {
                it.sendMessage("<green>+<red>10</red> seconds of powerful snowballs! (donation)".style())
            }
        }
    }

    private fun spawnSnowGolem(name: String?, flying: Boolean) {
        val nmsWorld = (ChristmasEventPlugin.instance.serverWorld as CraftWorld).handle

        val snowmanName =
            if (name != null) "<aqua>$name's</aqua> Snow Golem".style()
            else "<game_colour>${if (flying) "Flying" else "Angry"} Snow Golem".style()

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

        val flyingText = if (flying) " flying" else "n angry"
        val message =
            if (name != null) "<green>A$flyingText snowman has joined the game! (<aqua>$name's</aqua> donation)".style()
            else "<green>A$flyingText snowman has joined the game! (donation)".style()

        remainingPlayers().forEach { it.sendMessage(message) }
    }

    private fun snowballRain(name: String?) {
        val world = ChristmasEventPlugin.instance.serverWorld

        floorLevelBlocks.forEach {
            val location = it.block.location
            location.y = 150.0 + (0..20).random().toDouble()

            if ((0..9).random() == 0) {
                world.spawn(location, Snowball::class.java)
            }
        }

        val message =
            if (name != null) "<green>A snowball rain has started! (<aqua>$name's</aqua> donation)".style()
            else "<green>A snowball rain has started! (donation)".style()

        remainingPlayers().forEach {
            it.playSound(it, Sound.WEATHER_RAIN, 1.0F, 0.5F)
            it.sendMessage(message)
        }
    }

    private fun meltBottomLayer(name: String?) {
        bottomLayerMelted = true
        var secondsElapsed = 0

        val meltedText =
            if (name != null) "<red><b>The bottom layer was melted by <aqua>$name</aqua>!".style()
            else "<red><b>The bottom layer has melted!".style()

        tasks += repeatingTask(20) {
            if (secondsElapsed++ == 5) {
                cancel()
                remainingPlayers().forEach { it.sendMessage(meltedText) }
            } else {
                remainingPlayers().forEach {
                    it.sendMessage("<red><b>The bottom layer will melt in <aqua>${6 - secondsElapsed}</aqua> seconds!".style())
                }
            }
        }

        tasks += delay(120) {
            floorLevelBlocks.forEach {
                if (it.block.y == 86) {
                    it.block.breakNaturally()
                }
            }
        }
    }

    private class CustomSnowGolem(private val game: Spleef, private val world: Level, location: Location, private val withMount: Boolean) : SnowGolem(EntityType.SNOW_GOLEM, world) {
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
