package gg.flyte.christmas.minigame.world

import gg.flyte.christmas.ChristmasEventPlugin
import org.bukkit.Location

/**
 * Represents a single point in the map, with the location's world
 * defined by the server's world defined [ChristmasEventPlugin.serverWorld]
 *
 * @param x The x coordinate
 * @param y The y coordinate
 * @param z The z coordinate
 * @param yaw The yaw rotation (default 0)
 * @param pitch The pitch rotation (default 0)
 *
 * @see Location
 * @return A [org.bukkit.Location] object with the given coordinates
 */
data class MapSinglePoint(
    val x: Number,
    val y: Number,
    val z: Number,
    val yaw: Number = 0,
    val pitch: Number = 0
) : Location(ChristmasEventPlugin.instance.serverWorld, x.toDouble(), y.toDouble(), z.toDouble(), yaw.toFloat(), pitch.toFloat())

/**
 * Represents a region in the map, with the location's world
 * defined by the server's world defined [ChristmasEventPlugin.serverWorld]
 *
 * @param minPoint The minimum point of the region
 * @param maxPoint The maximum point of the region
 *
 * @see MapSinglePoint
 */
class MapRegion(minPoint: MapSinglePoint, maxPoint: MapSinglePoint) {
    val minPoint: MapSinglePoint = MapSinglePoint(
        x = minOf(minPoint.x.toDouble(), maxPoint.x.toDouble()),
        y = minOf(minPoint.y.toDouble(), maxPoint.y.toDouble()),
        z = minOf(minPoint.z.toDouble(), maxPoint.z.toDouble()),
        yaw = minPoint.yaw,
        pitch = minPoint.pitch
    )
    val maxPoint: MapSinglePoint = MapSinglePoint(
        x = maxOf(minPoint.x.toDouble(), maxPoint.x.toDouble()),
        y = maxOf(minPoint.y.toDouble(), maxPoint.y.toDouble()),
        z = maxOf(minPoint.z.toDouble(), maxPoint.z.toDouble()),
        yaw = maxPoint.yaw,
        pitch = maxPoint.pitch
    )

    companion object {
        fun single(point: MapSinglePoint): MapRegion = MapRegion(point, point)
    }

    /**
     * Checks if a location is within the region
     */
    fun contains(location: Location): Boolean {
        return location.x in minPoint.x.toDouble()..maxPoint.x.toDouble() &&
                location.y in minPoint.y.toDouble()..maxPoint.y.toDouble() &&
                location.z in minPoint.z.toDouble()..maxPoint.z.toDouble()
    }

    /**
     * Gets the center of the region
     *
     * @return The center of the region as an [org.bukkit.Location] object
     */
    fun getCenter(): Location {
        val centerX = (minPoint.x.toDouble() + maxPoint.x.toDouble()) / 2
        val centerY = (minPoint.y.toDouble() + maxPoint.y.toDouble()) / 2
        val centerZ = (minPoint.z.toDouble() + maxPoint.z.toDouble()) / 2
        return Location(minPoint.world, centerX, centerY, centerZ)
    }

    /**
     * Gets a random location within the region
     *
     * @return A random location within the region as an [org.bukkit.Location] object
     */
    fun randomLocation(): Location {
        val randomX = minPoint.x.toDouble() + Math.random() * (maxPoint.x.toDouble() - minPoint.x.toDouble())
        val randomY = minPoint.y.toDouble() + Math.random() * (maxPoint.y.toDouble() - minPoint.y.toDouble())
        val randomZ = minPoint.z.toDouble() + Math.random() * (maxPoint.z.toDouble() - minPoint.z.toDouble())
        return Location(minPoint.world, randomX, randomY, randomZ, minPoint.yaw.toFloat(), minPoint.pitch.toFloat())
    }

    /**
     * Converts the region to a list of single block locations
     *
     * @return A list of [MapSinglePoint] objects representing each block in the region
     */
    fun toSingleBlockLocations(): List<MapSinglePoint> {
        val points = mutableListOf<MapSinglePoint>()
        for (x in minPoint.x.toInt()..maxPoint.x.toInt()) {
            for (y in minPoint.y.toInt()..maxPoint.y.toInt()) {
                for (z in minPoint.z.toInt()..maxPoint.z.toInt()) {
                    points.add(MapSinglePoint(x, y, z, minPoint.yaw, minPoint.pitch))
                }
            }
        }
        return points
    }
}
