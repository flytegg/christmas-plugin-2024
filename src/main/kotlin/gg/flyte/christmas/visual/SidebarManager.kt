package gg.flyte.christmas.visual

import com.google.common.base.Preconditions
import fr.mrmicky.fastboard.adventure.FastBoard
import gg.flyte.christmas.util.eventController
import gg.flyte.christmas.util.style
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * Manages the sidebar for the Christmas event through FastBoard's API.
 */
class SidebarManager {
    private val boardRegistry = mutableMapOf<UUID, FastBoard>()
    var dataSupplier = mutableMapOf<UUID, Int>()

    /**
     * Updates the sidebar for all online players.
     */
    fun update() {
        Bukkit.getOnlinePlayers().forEach {
            val board = boardRegistry.getOrPut(it.uniqueId) { FastBoard(it) }
            board.updateTitle("<red><b>ᴄʜʀɪsᴛᴍᴀs ᴇᴠᴇɴᴛ".style())
            updateLines(it)
        }
    }

    /**
     * Removes the sidebar for the provided player.
     * @param player The player to remove the sidebar for.
     */
    fun remove(player: Player) {
        boardRegistry.remove(player.uniqueId)?.delete()
    }

    /**
     * Updates the sidebar for the provided player.
     * @param player The player to update the sidebar for.
     * @param addExtra Additional components to add to the sidebar.
     */
    fun updateLines(player: Player, addExtra: List<Component>? = null) {
        val board = boardRegistry[player.uniqueId] ?: return

        val lines = mutableListOf(
            currentGameLine(),
            Component.empty(),
            getComponentForPositionAt(0, player),
            getComponentForPositionAt(1, player),
            getComponentForPositionAt(2, player)
        )

        if (!isTop3(player)) {
            lines += listOf(
                Component.empty(),
                "<colour:#b2ffab>ʏᴏᴜʀ sᴄᴏʀᴇ<grey>: ".style(),
                "<grey>${getPlacementByUUID(player.uniqueId)}. <colour:#ebadff><b>ʏᴏᴜ <reset><grey>(${dataSupplier[player.uniqueId]})".style()
            )
        }

        if (addExtra != null) lines += addExtra

        lines += listOf(
            Component.empty(),
            "<light_purple>ꜰʟʏᴛᴇ.ɢɢ/ᴅᴏɴᴀᴛᴇ".style()
        )

        board.lines.clear()
        board.updateLines(lines)
    }

    /**
     * Returns a string representing the current game line for the sidebar.
     */
    private fun currentGameLine() = "<aqua>ɢᴀᴍᴇ: <0>".style(eventController().currentGame?.gameConfig?.smallDisplayName ?: "<grey>ɴᴏɴᴇ".style())

    /**
     * Returns a component representing the player at the provided position.
     *
     * For example:
     *
     * "➊: <playerName> (score)" or "➊: None" or "➊: YOU (score)"
     */
    private fun getComponentForPositionAt(position: Int, player: Player): Component {
        Preconditions.checkArgument(position in 0..2, "Position must be between 0 and 2")

        val placeDefaultComponent = mapOf(
            0 to "<colour:#ffcb1a>➊<grey>:",
            1 to "<colour:#d0d0d0>➋<grey>:",
            2 to "<colour:#a39341>➌<grey>:"
        )

        val uniqueIdAtPosition = getUUIDByPlacement(position)
        val nameComponent = when (uniqueIdAtPosition) {
            player.uniqueId -> "<colour:#ebadff><b>ʏᴏᴜ <reset><colour:#fcb3b3>(${dataSupplier[player.uniqueId]})".style()
            null -> "<white>ɴᴏɴᴇ".style()
            else -> "<colour:#f5d6ff>${Bukkit.getOfflinePlayer(uniqueIdAtPosition).name ?: "ᴜɴᴋɴᴏᴡɴ"} <reset><colour:#fcb3b3>(${dataSupplier[uniqueIdAtPosition]})".style()
        }

        return "${placeDefaultComponent[position]!!} <0>".style(nameComponent)
    }

    /**
     * Returns whether the provided player is in the top 3.
     */
    private fun isTop3(player: Player): Boolean = (0..2).any { getUUIDByPlacement(it) == player.uniqueId }

    /**
     * Returns the UUID of the player at the provided position.
     * @param position The position to get the UUID for.
     * @return The UUID of the player at the provided position, or null if there is no player at that position.
     */
    private fun getUUIDByPlacement(position: Int): UUID? {
        if (position >= dataSupplier.size) return null
        return dataSupplier.entries.sortedByDescending { it.value }[position].key
    }

    /**
     * Returns the placement of the player with the provided UUID. NON-Zero indexed.
     * @param uuid The UUID of the player to get the placement for.
     * @return The placement of the player with the provided UUID. [e.g 1, 2, 3, ...]
     */
    private fun getPlacementByUUID(uuid: UUID): Int {
        val sorted = dataSupplier.entries.sortedByDescending { it.value }
        return sorted.indexOfFirst { it.key == uuid } + 1
    }
}
