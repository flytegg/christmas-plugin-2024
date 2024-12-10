package gg.flyte.christmas

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.util.MojangAPIUtil
import dev.shreyasayyengar.menuapi.menu.MenuManager
import gg.flyte.christmas.commands.EventCommand
import gg.flyte.christmas.donation.DonationListener
import gg.flyte.christmas.listeners.HousekeepingEventListener
import gg.flyte.christmas.minigame.engine.EventController
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.npc.WorldNPC
import gg.flyte.christmas.util.Util
import gg.flyte.christmas.util.colourise
import gg.flyte.christmas.util.eventController
import gg.flyte.christmas.util.style
import gg.flyte.twilight.twilight
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.bukkit.scoreboard.Team.Option
import revxrsal.commands.bukkit.BukkitLamp
import java.util.*
import kotlin.reflect.full.primaryConstructor

class ChristmasEventPlugin : JavaPlugin() {
    lateinit var serverWorld: World
    lateinit var lobbySpawn: MapSinglePoint
    lateinit var scoreBoardTab: Scoreboard
    var cameraPlayer: UUID = UUID.fromString("a008c892-e7e1-48e1-8235-8aa389318b7a") // "devous" | Josh
    var eventController: EventController = EventController()
    var worldNPCs: MutableSet<WorldNPC> = HashSet()

    companion object {
        val instance: ChristmasEventPlugin by lazy { getPlugin(ChristmasEventPlugin::class.java) }
    }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this).also { it.load() })
    }

    override fun onEnable() {
        logger.info("ChristmasEventPlugin has been enabled!")

        initDependencies()
        createConfig()
        configureWorld()
        registerCommands()
        registerEvents()
        registerPacketAPI()
        loadNPCs()
    }

    override fun onDisable() {
        serverWorld.entities.forEach {
            val kickMessage = Component.empty()
                .append("<colour:#ff7070>ᴛʜᴀɴᴋ ʏᴏᴜ ꜰᴏʀ ᴊᴏɪɴɪɴɢ ᴜs!\n".style())
                .append("<colour:#67c45e>ᴡᴇ ᴡɪsʜ ʏᴏᴜ ᴀ ᴍᴇʀʀʏ ᴄʜʀɪsᴛᴍᴀs\n".style())
                .append("<gradient:#EE57FF:#E89EB8>ꜰʟʏᴛᴇ".style())

            if (it !is Player) it.remove() else it.kick(kickMessage)
        } // clean up podium, spectate points, misc entities.

        for (npc in worldNPCs) {
            npc.location.getNearbyEntitiesByType(TextDisplay::class.java, 5.0).forEach { it.remove() }
        }
    }

    private fun initDependencies() {
        twilight(this)
        MenuManager(this)
    }

    private fun createConfig() {
        config.options().copyDefaults(true)
        saveDefaultConfig()
    }

    private fun registerCommands() {
        val lamp = BukkitLamp.builder(this).build()
        lamp.register(EventCommand())
    }

    private fun registerEvents() {
        HousekeepingEventListener()
        DonationListener()
    }

    private fun registerPacketAPI() {
        PacketEvents.getAPI().init()
    }

    private fun loadNPCs() {
        for (contributor in Util.getEventContributors()) {
            val uniqueId = contributor.uniqueId
            val contribution = contributor.contribution
            val location = contributor.location

            val randomColour = mapOf(
                "<dark_red>" to "4",
                "<red>" to "c",
                "<gold>" to "6",
                "<dark_green>" to "2",
                "<green>" to "a",
                "<blue>" to "9",
            ).entries.random()

            val displayName = "§${randomColour.value}${MojangAPIUtil.requestPlayerName(uniqueId)}".colourise()

            val contributorNPC = WorldNPC.createFromUniqueId(displayName, uniqueId, location).also { worldNPCs += it }
            contributorNPC.npc.prefixName = "${randomColour.key}<obf>W ".style()
            contributorNPC.npc.suffixName = " ${randomColour.key}<obf>W".style()
            contributorNPC.spawnForAll()

            location.world.spawn(location.clone().add(0.0, 2.5, 0.0), TextDisplay::class.java).apply {
                text("<pink>$contribution".style())
                backgroundColor = Color.fromARGB(150, 0, 0, 0)
                billboard = Display.Billboard.CENTER
            }
        }
    }

    private fun configureWorld() {
        serverWorld = Bukkit.getWorld("world")!!.apply {
            setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            setGameRule(GameRule.DO_MOB_SPAWNING, false)
            setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            setGameRule(GameRule.DO_FIRE_TICK, false)
            setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, true)
            setStorm(false)
            difficulty = Difficulty.PEACEFUL
            time = 6000
        }

        // Create Podium Model
        serverWorld.spawn(MapSinglePoint(535.5, 105.0, 503.5), ItemDisplay::class.java) {
            it.setItemStack(ItemStack(Material.PAPER).apply {
                itemMeta = itemMeta?.apply { setCustomModelData(2) }
            })
            it.transformation = it.transformation.apply { this.scale.mul(4F) }
        }

        // Create Event Leaderboard Display
        serverWorld.spawn(MapSinglePoint(535.5, 121, 503.5, -90, 0), TextDisplay::class.java) {
            it.text("<gold>ᴇᴠᴇɴᴛ\nʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ".style())
            it.transformation = it.transformation.apply {
                this.scale.mul(10F)
            }
            it.billboard = Display.Billboard.CENTER
            it.isDefaultBackground = false
            it.backgroundColor = Color.fromRGB(94, 68, 23)
            it.isSeeThrough = false
        }

        // Create Event Contributors Display
        serverWorld.spawn(MapSinglePoint(544.5, 108, 457.5, 0, 30), TextDisplay::class.java) {
            it.text("<colour:#d45757>ᴇᴠᴇɴᴛ\nᴄᴏɴᴛʀɪʙᴜᴛᴏʀꜱ".style())
            it.transformation = it.transformation.apply { this.scale.mul(7F) }
            it.billboard = Display.Billboard.CENTER
            it.isDefaultBackground = false
            it.backgroundColor = Color.fromARGB(255, 255, 207, 207)
            it.brightness = Display.Brightness(15, 15)
            it.isSeeThrough = false
        }

        // Create Donation Info
        serverWorld.spawn(MapSinglePoint(555, 105, 502, 43, 25), TextDisplay::class.java) {
            it.text(
                Component.empty()
                    .append("<white>sᴜᴘᴘᴏʀᴛ <colour:#ec8339>ʙᴇsᴛ ꜰʀɪᴇɴᴅs ᴀɴɪᴍᴀʟ sᴏᴄɪᴇᴛʏ\n".style())
                    .append("\n".style())
                    .append("<white>ᴛʜɪs ᴄʜʀɪsᴛᴍᴀs ᴄʜᴀʀɪᴛʏ ᴇᴠᴇɴᴛ ɪs ɪɴ sᴜᴘᴘᴏʀᴛ ᴏꜰ ᴛʜᴇ <colour:#ec8339>ʙᴇsᴛ ꜰʀɪᴇɴᴅs ᴀɴɪᴍᴀʟ sᴏᴄɪᴇᴛʏ ".style())
                    .append("<white>ᴡʜɪᴄʜ ɪs ᴀ ʟᴇᴀᴅɪɴɢ ɴᴀᴛɪᴏɴᴀʟ ᴀɴɪᴍᴀʟ ᴡᴇʟꜰᴀʀᴇ ᴏʀɢᴀɴɪsᴀᴛɪᴏɴ ᴅᴇᴅɪᴄᴀᴛᴇᴅ ᴛᴏ ᴇɴᴅɪɴɢ ᴛʜᴇ ᴋɪʟʟɪɴɢ ".style())
                    .append("<white>ᴏꜰ ᴅᴏɢs ᴀɴᴅ ᴄᴀᴛs ɪɴ ᴀᴍᴇʀɪᴄᴀ's sʜᴇʟᴛᴇʀs ᴅᴜᴇ ᴛᴏ ʜᴏᴍᴇʟᴇssɴᴇss ᴀɴᴅ ᴏᴠᴇʀᴘᴏᴘᴜʟᴀᴛɪᴏɴ. ".style())
                    .append("<white>ᴛʜᴇʏ ᴀʀᴇ ᴀ ɴᴏɴ-ᴘʀᴏꜰɪᴛ ᴏʀɢᴀɴɪsᴀᴛɪᴏɴ ᴛʜᴀᴛ ʀᴜɴs ᴛʜᴇ ʟᴀʀᴇsᴛ ɴᴏ-ᴋɪʟʟ ᴍᴏᴠᴇᴍᴇɴᴛ & ᴀɴɪᴍᴀʟ sᴀɴᴄᴛᴜᴀʀʏ ɪɴ ᴛʜᴇ ᴜ.s.".style())
                    .append("\n\n".style())
                    .append("<colour:#ff3d9b>ᴛʜᴇ ʙᴇsᴛ ᴘᴀʀᴛ? ᴇᴠᴇʀʏ ᴅᴏʟʟᴀʀ ʏᴏᴜ ᴘᴜᴛ ꜰᴏᴜʀᴛʜ ᴡɪʟʟ ʙᴇ <b><colour:#ec8339>ᴛʀɪᴘʟᴇ ᴍᴀᴛᴄʜᴇᴅ<reset><white>!\n\n".style())
                    .append("<white>ʜᴇᴀᴅ ᴏᴠᴇʀ ᴛᴏ <gradient:#ff80e8:#ffffff>ꜰʟʏᴛᴇ.ɢɢ/ᴅᴏɴᴀᴛᴇ</gradient> ᴛᴏ ᴅᴏɴᴀᴛᴇ ɴᴏᴡ!\n\n".style())
                    .append("<white>ʟᴇᴀʀɴ ᴍᴏʀᴇ @ <colour:#ec8339>ʙᴇsᴛꜰʀɪᴇɴᴅs.ᴏʀɢ\n\n".style())
                    .append("<red>ᴛʜᴀɴᴋ ʏᴏᴜ ꜰᴏʀ ᴊᴏɪɴɪɴɢ ᴜs ❤".style())

            )
            it.lineWidth = 250
            it.transformation = it.transformation.apply { this.scale.mul(2F) }
            it.billboard = Display.Billboard.FIXED
            it.isDefaultBackground = false
            it.backgroundColor = Color.fromARGB(255, 100, 100, 100)
            it.brightness = Display.Brightness(15, 15)
            it.isSeeThrough = false
        }

        WorldNPC.refreshPodium()

        // Create scoreboard:
        // note: player list displays entries by alphabetical order of the team they have entries with
        scoreBoardTab = Bukkit.getScoreboardManager().newScoreboard.apply {
            registerNewTeam("a. staff").apply {
                setOption(Option.COLLISION_RULE, Team.OptionStatus.NEVER)
                setOption(Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
                color(NamedTextColor.GRAY)
                prefix("<red><b>ѕᴛᴀꜰꜰ ".style())
            }
            registerNewTeam("b. player").apply {
                color(NamedTextColor.GRAY)
                setOption(Option.COLLISION_RULE, Team.OptionStatus.NEVER)
                setOption(Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
            }
        }

        eventController.updateDonationBar()

        lobbySpawn = MapSinglePoint(559.5, 103, 518.5, 135, 0)

        GameConfig.entries.forEach { it.gameClass.primaryConstructor } // preload/cache classes for reflection
        eventController().startPlaylist()

    }
}