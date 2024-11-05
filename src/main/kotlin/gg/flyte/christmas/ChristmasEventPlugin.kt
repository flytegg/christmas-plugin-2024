package gg.flyte.christmas

import com.github.retrooper.packetevents.PacketEvents
import dev.shreyasayyengar.menuapi.menu.MenuManager
import gg.flyte.christmas.commands.EventCommand
import gg.flyte.christmas.listeners.HousekeepingEventListener
import gg.flyte.christmas.minigame.engine.EventController
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.npc.WorldNPC
import gg.flyte.christmas.util.Util
import gg.flyte.christmas.util.colourise
import gg.flyte.christmas.util.eventController
import gg.flyte.twilight.twilight
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.bukkit.scoreboard.Team.Option
import revxrsal.commands.bukkit.BukkitLamp
import java.util.UUID
import kotlin.reflect.full.primaryConstructor

class ChristmasEventPlugin : JavaPlugin() {
    lateinit var serverWorld: World
    lateinit var lobbySpawn: Location
    lateinit var scoreBoardTab: Scoreboard
    var cameraPlayer: UUID = UUID.fromString("a008c892-e7e1-48e1-8235-8aa389318b7a") // "devous" | Josh
    var eventController: EventController = EventController()
    var worldNPCs: MutableSet<WorldNPC> = HashSet()

    companion object {
        val instance: ChristmasEventPlugin by lazy { getPlugin(ChristmasEventPlugin::class.java) }
    }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        logger.info("ChristmasEventPlugin has been enabled!")

        initDependencies()
        createConfig()
        fixData()
        registerCommands()
        registerEvents()
        registerPacketAPI()
        loadContributorNPCs()
    }

    override fun onDisable() {
        for (npc in worldNPCs) {
            npc.location.getNearbyEntitiesByType(TextDisplay::class.java, 5.0).forEach { it.remove() }
        }
    }

    private fun initDependencies() {
        twilight(this)
        MenuManager(this)
    }

    private fun createConfig() {
        config.options().configuration()
        saveDefaultConfig()
    }

    private fun registerCommands() {
        val lamp = BukkitLamp.builder(this).build()
        lamp.register(EventCommand())
    }

    private fun registerEvents() {
        HousekeepingEventListener()
    }

    private fun registerPacketAPI() {
        PacketEvents.getAPI().init()
    }

    private fun loadContributorNPCs() {
        for (contributor in Util.getEventContributors()) {
            val playerName = contributor.playerName
            val contribution = contributor.contribution
            val location = contributor.location // TODO configure pitch and yaw

            var randomColour: String = listOf("4", "c", "6", "2", "a", "9").random()
            val displayName: String = "§$randomColour$playerName".colourise()

            var createFromName = WorldNPC.createFromName(displayName, playerName, location).also { worldNPCs += it }
            createFromName.npc.prefixName = Component.text("§$randomColour§kW ")
            createFromName.npc.suffixName = Component.text(" §$randomColour§kW")

            Bukkit.getOnlinePlayers().forEach { createFromName.spawnFor(it) }

            location.world.spawn(location.clone().add(0.0, 2.5, 0.0), TextDisplay::class.java).apply {
                text(Component.text(contribution, TextColor.color(255, 196, 255)))
                backgroundColor = Color.fromRGB(84, 72, 84)
                billboard = Display.Billboard.CENTER
                isSeeThrough = false
            }
        }
    }

    private fun fixData() {
        serverWorld = Bukkit.getWorld("world")!!.apply {
            setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            setGameRule(GameRule.DO_MOB_SPAWNING, false)
            setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            setGameRule(GameRule.DO_FIRE_TICK, false)
            setStorm(false)
            time = 6000
        }

        // player list displays entries by alphabetical order of the team they have entries with
        scoreBoardTab = Bukkit.getScoreboardManager().newScoreboard.apply {
            registerNewTeam("a. staff").apply {
                setOption(Option.COLLISION_RULE, Team.OptionStatus.NEVER)
                setOption(Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
                color(NamedTextColor.GRAY)
                prefix(Component.text("ѕᴛᴀꜰꜰ ", NamedTextColor.RED, TextDecoration.BOLD))
            }

            registerNewTeam("b. player").apply {
                color(NamedTextColor.GRAY)
                setOption(Option.COLLISION_RULE, Team.OptionStatus.NEVER)
                setOption(Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
            }
        }

        lobbySpawn = MapSinglePoint(559.5, 105, 554.5, 180, 0)

        GameConfig.entries.forEach { it.gameClass.primaryConstructor } // preload/cache classes for reflection
        eventController().startPlaylist()
    }
}
