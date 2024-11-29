package gg.flyte.christmas.lobby

import gg.flyte.christmas.minigame.world.MapRegion
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.util.eventController
import gg.flyte.twilight.event.event
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class LobbySnowballs {
    private val lobbyRegion = MapRegion(
        MapSinglePoint(545, 100, 500), // PLACEHOLDER VALUES (I DONT HAVE THE REAL MAP)
        MapSinglePoint(575, 120, 535)
    )

    init {
        event<PlayerInteractEvent> {
            if (!shouldProcessEvent(this)) return@event

            val clickedBlock = clickedBlock ?: return@event

            // Only handle snow-type blocks
            if (clickedBlock.type != Material.SNOW && clickedBlock.type != Material.SNOW_BLOCK) {
                return@event
            }

            // Give snowball and play effects
            if (player.inventory.firstEmpty() != -1) {
                giveSnowball(player)
                playSnowCollectionEffects(player, clickedBlock.location)
            }

            // Cancel vanilla block breaking
            isCancelled = true
        }
    }

    private fun shouldProcessEvent(event: PlayerInteractEvent): Boolean {
        return eventController().currentGame == null &&
                lobbyRegion.contains(event.player.location) &&
                event.player.gameMode == GameMode.ADVENTURE
    }

    private fun playSnowCollectionEffects(player: Player, location: org.bukkit.Location) {
        // Sound effects
        player.playSound(location, Sound.BLOCK_SNOW_BREAK, 1.0f, 1.5f)
        player.playSound(location, Sound.BLOCK_SNOW_STEP, 0.5f, 2.0f)

        // Snow particles
        location.world.spawnParticle(
            org.bukkit.Particle.SNOWFLAKE,
            location.add(0.5, 0.5, 0.5),
            8,  // Count
            0.2, 0.2, 0.2,  // Spread
            0.1  // Speed
        )
    }

    private fun giveSnowball(player: Player) {
        player.inventory.addItem(ItemStack(Material.SNOWBALL, 1))
    }

    companion object {
        private var instance: LobbySnowballs? = null

        fun initialize() {
            instance = LobbySnowballs()
        }
    }
}