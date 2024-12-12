package gg.flyte.christmas.visual

import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.twilight.scheduler.delay
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.*

/**
 * I'd honestly recommend not touching this class unless you're very familiar with the Bukkit API.
 */
class CameraSequence(
    locations: List<MapSinglePoint>,
    private val players: Collection<Player>,
    private val component: Component?,
    teleportDuration: Int = 15,
    timelineSeparation: Int = 30,
    private val onComplete: (() -> Unit)? = null
) {

    companion object {
        val ACTIVE_CAMERAS = mutableSetOf<UUID>()
    }

    init {
        if (locations.size < 2) throw IllegalArgumentException("At least 2 locations are required for a camera sequence.")

        val controlPoints = mutableMapOf<Int, Point>()
        for ((index, location) in locations.withIndex()) {
            controlPoints[index * timelineSeparation] = Point(
                location.toVector(),
                location.yaw.toFloat(),
                location.pitch.toFloat()
            )
        }
        SequenceTask(generateSmoothPath(controlPoints), teleportDuration).runTaskTimer(ChristmasEventPlugin.instance, 0, 1)
    }

    data class Point(
        val position: Vector,
        val pointYaw: Float,
        val pointPitch: Float
    ) : Location(ChristmasEventPlugin.instance.serverWorld, position.x, position.y, position.z, pointYaw, pointPitch)

    fun generateSmoothPath(controlPoints: Map<Int, Point>): Map<Int, Point> {
        val sortedPoints = controlPoints.toSortedMap()
        val resultPoints = mutableMapOf<Int, Point>()

        val keys = sortedPoints.keys.toList()
        val values = sortedPoints.values.toList()

        if (keys.size < 2) throw IllegalArgumentException("At least 2 points are required for interpolation.")

        val totalTicks = keys.last()

        for (tick in 0..totalTicks) {
            val t = tick.toDouble() / totalTicks
            val point = interpolatePoints(values, t)
            resultPoints[tick] = point
        }

        return resultPoints
    }

    private fun interpolatePoints(points: List<Point>, t: Double): Point {
        // Directly return start or end point when t is 0 or 1
        if (t <= 0.0) return points[0]
        if (t >= 1.0) return points[1]

        val n = points.size
        val p1 = ((t * (n - 1)).toInt()).coerceAtMost(n - 2)
        val p0 = (p1 - 1).coerceAtLeast(0)
        val p2 = (p1 + 1).coerceAtMost(n - 1)
        val p3 = (p2 + 1).coerceAtMost(n - 1)

        val localT = (t * (n - 1)) - p1
        val interpolatedPosition = catmullRomPosition(points[p0].position, points[p1].position, points[p2].position, points[p3].position, localT)
        val interpolatedYaw = interpolateAngle(points[p1].pointYaw, points[p2].pointYaw, localT).toFloat()
        val interpolatedPitch = interpolateAngle(points[p1].pointPitch, points[p2].pointPitch, localT).toFloat()

        return Point(interpolatedPosition, pointYaw = interpolatedYaw, pointPitch = interpolatedPitch)
    }

    private fun catmullRomPosition(p0: Vector, p1: Vector, p2: Vector, p3: Vector, t: Double): Vector {
        return Vector(
            lerpSmooth(p0.x, p1.x, p2.x, p3.x, t),
            lerpSmooth(p0.y, p1.y, p2.y, p3.y, t),
            lerpSmooth(p0.z, p1.z, p2.z, p3.z, t)
        )
    }

    private fun lerpSmooth(d0: Double, d1: Double, d2: Double, d3: Double, t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5 * (2 * d1 + (-d0 + d2) * t + (2 * d0 - 5 * d1 + 4 * d2 - d3) * t2 + (-d0 + 3 * d1 - 3 * d2 + d3) * t3)
    }

    private fun interpolateAngle(angle1: Float, angle2: Float, t: Double): Double {
        val delta = ((angle2 - angle1 + 360) % 360)
        val shortestDelta = if (delta > 180) delta - 360 else delta
        return (angle1 + shortestDelta * t)
    }

    private inner class SequenceTask(private var interpolatedPath: Map<Int, Point>, private val teleportDuration: Int) : BukkitRunnable() {
        private var currentTick = 0
        private var itemDisplay: ItemDisplay? = null
        private var textDisplay: TextDisplay? = null

        /**
         * Due to the atrocities of the Bukkit API, this runnable must maintain a very specific
         * code execution order, so that the ItemDisplay continues to interpolate, the player
         * continues to spectate, and the TextDisplay is continually mounted onto the ItemDisplay.
         *
         * If the order of execution is changed, there will be breakages with an unknown number of API components.
         *
         * The current order is such that:
         * - ItemDisplay's teleportDuration must be set in the spawn lambda
         * - TextDisplay's transformation must be set in the spawn lambda
         * - The spawn lambdas for both entities must be separate.
         * - Player must be teleported and set to spectator at least 5 ticks after the ItemDisplay is spawned.
         * - The TextDisplay must be removed before each ItemDisplay teleporation.
         * - The TextDisplay must be re-added as a passenger after each ItemDisplay teleportation.
         */
        override fun run() {
            var nextPosition = interpolatedPath[currentTick]

            if (nextPosition == null) {
                this.cancel()

                ACTIVE_CAMERAS.remove(itemDisplay?.uniqueId)

                itemDisplay?.remove()
                textDisplay?.remove()
                onComplete?.invoke()
                return
            }

            if (itemDisplay == null) {
                // Spawn a single ItemDisplay entity at the starting location
                itemDisplay = nextPosition.world.spawn(nextPosition, ItemDisplay::class.java) { itemDisplay ->
                    itemDisplay.setItemStack(ItemStack(Material.AIR))
                    itemDisplay.setRotation(nextPosition.pointYaw, nextPosition.pointPitch)
                    itemDisplay.teleportDuration = this.teleportDuration
                    itemDisplay.viewRange = Float.MAX_VALUE

                    if (component != null) {
                        textDisplay = nextPosition.world.spawn(nextPosition, TextDisplay::class.java) { textDisplay ->
                            textDisplay.text(component)
                            textDisplay.isSeeThrough = true
                            textDisplay.alignment = TextDisplay.TextAlignment.LEFT
                            textDisplay.lineWidth = 300
                            textDisplay.backgroundColor = Color.fromARGB(225, 38, 38, 38)
                            textDisplay.billboard = Display.Billboard.CENTER
                            textDisplay.interpolationDelay = 50 // 50 ticks to stay down
                            textDisplay.interpolationDuration = 50 // 50 ticks to move up
                            textDisplay.transformation = textDisplay.transformation.apply { translation.add(0F, -10F, -3F) }
                            delay(5) { textDisplay.transformation = textDisplay.transformation.apply { translation.add(0F, 8.5F, 0F) } }
                        }
                    }

                    ACTIVE_CAMERAS.add(itemDisplay!!.uniqueId)
                }

                players.forEach { player ->
                    player.gameMode = GameMode.SPECTATOR
                    player.teleport(itemDisplay!!.location)
                    delay(1) { player.spectatorTarget = itemDisplay }
                }
            } else {
                itemDisplay?.apply {
                    if (textDisplay != null) removePassenger(textDisplay!!)
                    teleport(nextPosition)
                    if (textDisplay != null) addPassenger(textDisplay!!)
                }
            }

            currentTick++
        }
    }
}