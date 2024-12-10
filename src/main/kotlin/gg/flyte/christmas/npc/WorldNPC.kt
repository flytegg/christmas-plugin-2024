package gg.flyte.christmas.npc

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.attribute.Attributes
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.npc.NPC
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.util.MojangAPIUtil
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.util.eventController
import gg.flyte.christmas.util.packetObj
import gg.flyte.christmas.util.sendPacket
import gg.flyte.christmas.util.style
import gg.flyte.twilight.scheduler.async
import gg.flyte.twilight.scheduler.sync
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.*

/**
 * Wrapper for a Packet-based ServerPlayer (NPC) through PacketEvent's [NPC] implementation.
 */
class WorldNPC private constructor(displayName: String, textureProperties: List<TextureProperty?>?, val location: Location) {

    private val userProfile: UserProfile = UserProfile(UUID.randomUUID(), displayName, textureProperties)
    private var id: Int = SpigotReflectionUtil.generateEntityId()
    private val tablistName = "NPC-$id".style()
    var scale = 0.5
    val npc: NPC = NPC(userProfile, id, tablistName)

    /**
     * Spawns this NPC for the given player via packets
     * @param player the player to spawn the NPC for
     */
    fun spawnFor(player: Player) {
        this.npc.location = location.packetObj()
        try {
            this.npc.spawn(PacketEvents.getAPI().playerManager.getUser(player).channel)
        } catch (_: Exception) {
        } // try-catch to prevent very rare exception with netty channel being invalidated. (when player leaves)

        // skin packet
        WrapperPlayServerEntityMetadata(id, listOf(EntityData(17, EntityDataTypes.BYTE, 127.toByte()))).sendPacket(player)

        // scale packet (if needed)
        val modifier = WrapperPlayServerUpdateAttributes.PropertyModifier(
            Attributes.GENERIC_SCALE.name,
            scale,
            WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION
        )
        var property = WrapperPlayServerUpdateAttributes.Property(Attributes.GENERIC_SCALE, scale, listOf(modifier))
        WrapperPlayServerUpdateAttributes(id, listOf(property)).sendPacket(player)

    }

    /**
     * Spawns this NPC for all players via packets
     */
    fun spawnForAll() {
        for (player in Bukkit.getOnlinePlayers()) spawnFor(player)
    }

    /**
     * Despawns this NPC for the given player via packets
     * @param player the player to despawn the NPC for
     */
    fun despawnFor(player: Player) = this.npc.despawn(PacketEvents.getAPI().playerManager.getUser(player).channel)

    /**
     * Despawns this NPC for all players via packets
     */
    fun despawnForAll() {
        for (player in Bukkit.getOnlinePlayers()) despawnFor(player)
    }

    companion object {
        private val leaderBoardNPCs = HashMap<Int, WorldNPC>()
        private val leaderboardPositionToLocation = mapOf(
            0 to MapSinglePoint(535.5, 108.3, 503.5, -90, 0), // 2.5
            1 to MapSinglePoint(535.5, 106.55, 507.5, -90, 0), // 2.0
            2 to MapSinglePoint(535.5, 105, 499.5, -90, 0) // 1.5
        )
        private val leaderboardPositionToNamePlateLocation = mapOf(
            0 to MapSinglePoint(537.3, 107.48, 503.55, -90, 0),
            1 to MapSinglePoint(537.3, 105.72, 507.55, -90, 0),
            2 to MapSinglePoint(537.3, 104.25, 499.55, -90, 0)
        )
        private val placeDefaultComponent = mapOf(
            0 to "<colour:#ffcb1a>➊",
            1 to "<colour:#d0d0d0>➋",
            2 to "<colour:#a39341>➌"
        )

        /**
         * Refreshes the podium with the current top 3 players in the event.
         */
        fun refreshPodium() {
            ChristmasEventPlugin.instance.serverWorld.entities.forEach {
                if (it.persistentDataContainer.has(NamespacedKey("christmas", "placeholder"), PersistentDataType.BOOLEAN)) {
                    it.remove()
                }
            }

            // remove all existing leaderboard NPCs
            leaderBoardNPCs.forEach { (_, npc) ->
                ChristmasEventPlugin.instance.worldNPCs.remove(npc)
                npc.despawnForAll()
            }

            eventController().points.entries
                .sortedByDescending { it.value }
                .take(3)
                .forEachIndexed { index, (uniqueId, points) ->
                    ChristmasEventPlugin.instance.worldNPCs.remove(leaderBoardNPCs[index])

                    async {
                        leaderBoardNPCs[index] = createFromUniqueId("", uniqueId, leaderboardPositionToLocation[index]!!).apply {
                            this.scale = when (index) {
                                0 -> 2.5
                                1 -> 2.0
                                2 -> 1.5
                                else -> 1.0
                            }

                            ChristmasEventPlugin.instance.worldNPCs += this
                        }
                        sync {
                            leaderBoardNPCs[index]?.spawnForAll()
                        }
                    }

                    ChristmasEventPlugin.instance.serverWorld.spawn(leaderboardPositionToNamePlateLocation[index]!!, TextDisplay::class.java) {
                        it.text("${placeDefaultComponent[index]!!} ${Bukkit.getOfflinePlayer(uniqueId).name}\nᴘᴏɪɴᴛs: $points".style())
                        it.transformation = it.transformation.apply { this.scale.mul(1.5F) }
                        it.backgroundColor = Color.fromARGB(0, 0, 0, 0)
                        it.billboard = Display.Billboard.FIXED
                        it.persistentDataContainer.set(NamespacedKey("christmas", "placeholder"), PersistentDataType.BOOLEAN, true)
                    }
                }
        }

        /**
         * Creates a new [WorldNPC] modelled from an existing [Player] reference.
         * @param displayName the display name of the NPC
         * @param modelAfter the player to model the NPC after
         * @param location the location to spawn the NPC at
         * @return the newly created [WorldNPC] instance
         */
        fun createFromLive(displayName: String, modelAfter: Player, location: Location): WorldNPC {
            val textureProperties = PacketEvents.getAPI().playerManager.getUser(modelAfter).profile.textureProperties
            return WorldNPC(displayName, textureProperties, location)
        }

        /**
         * Creates a new [WorldNPC] modelled from a [UUID] reference.
         * @param displayName the display name of the NPC
         * @param modelAfter the player to model the NPC after
         * @param location the location to spawn the NPC at
         * @return the newly created [WorldNPC] instance
         */
        fun createFromUniqueId(displayName: String, modelAfter: UUID, location: Location): WorldNPC {
            // fetch texture properties from Mojang using player name
            val textureProperty = MojangAPIUtil.requestPlayerTextureProperties(modelAfter)
            return WorldNPC(displayName, textureProperty, location)
        }
    }
}
