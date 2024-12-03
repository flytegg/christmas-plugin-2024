package gg.flyte.christmas.visual

import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.twilight.scheduler.delay
import io.papermc.paper.entity.TeleportFlag
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

/**
 * @see gg.flyte.christmas.visual.CameraSequence
 */
class CameraSlide(slideTo: MapSinglePoint, onComplete: (() -> Unit)? = null) {
    constructor(gameConfig: GameConfig, onComplete: (() -> Unit)?) : this(gameConfig.centrePoint, onComplete)

    init {
        var hasCompleted = false // don't run onComplete for every online player

        Bukkit.getOnlinePlayers().forEach { loopedPlayer ->
            loopedPlayer.world.spawn(loopedPlayer.location, ItemDisplay::class.java) {
                it.setItemStack(ItemStack(Material.AIR))
                it.teleportDuration = 5
                CameraSequence.ACTIVE_CAMERAS.add(it.uniqueId)

                delay(1) {
                    loopedPlayer.gameMode = GameMode.SPECTATOR
                    loopedPlayer.spectatorTarget = it

                    // Call `onComplete` only once
                    SlideCameraTask(it, slideTo) {
                        if (!hasCompleted) {
                            onComplete?.invoke()
                            hasCompleted = true // Mark as called to prevent further executions
                        }
                    }.runTaskTimer(ChristmasEventPlugin.instance, 0, 1)
                }
            }
        }
    }

    /**
     * Represents a Bukkit task for smoothly moving the display item through a sequence of locations.
     *
     * The display movement sequence is defined by three stages:
     *  - Stage 0: Move the display to a high point, adjusting the Y-coordinate and pitch.
     *  - Stage 1: Move the display to the destination X and Z coordinates.
     *  - Stage 2: Move the display to the destination Y coordinate.
     *
     * The display movement is achieved through easing functions, providing smooth acceleration and deceleration effects.
     * The task is executed periodically based on the Bukkit scheduler and is responsible for updating the display's position
     * and orientation.
     *
     * @param itemDisplay The player whose camera is being moved.
     * @param location The final destination of the camera movement.
     * @param onComplete The callback function to execute when the camera movement is complete.
     */
    class SlideCameraTask(
        private val itemDisplay: ItemDisplay,
        private val location: Location,
        private val onComplete: (() -> Unit)? = null
    ) : BukkitRunnable() {
        private val destination = location
        private val highPoint = 200.0
        private val duration = 80
        private val epsilon = 0.5
        private var progress = 0.0
        private var stage = 0

        /**
         * Executes the display movement logic based on the current stage of the display sequence.
         *
         * The display sequence consists of three stages:
         *  - Stage 0: Move the display to a high point, adjusting the Y-coordinate and pitch.
         *  - Stage 1: Move the display to the destination X and Z coordinates.
         *  - Stage 2: Move the display to the destination Y coordinate.
         *
         * After completing the stages, the camera sequence is finished, and the task is canceled.
         */
        override fun run() {
            val (curX, curY, curZ, curYaw, curPitch) = itemDisplay.location
            val (destX, destY, destZ, destYaw, destPitch) = destination

            when (stage) {
                0 -> updateCamera(
                    deltaY = highPoint - curY,
                    deltaYaw = destYaw - curYaw,
                    deltaPitch = destPitch - curPitch
                )

                1 -> updateCamera(
                    deltaX = destX - curX,
                    deltaZ = destZ - curZ
                )

                2 -> updateCamera(
                    deltaY = destY - curY,
                )

                else -> {
                    cancel()
                    onComplete?.invoke()
                    itemDisplay.remove()
                    CameraSequence.ACTIVE_CAMERAS.remove(itemDisplay.uniqueId)
                }
            }
        }

        /**
         * Applies the ease-in-out quadratic easing function to the input.
         *
         * The ease-in-out quadratic easing function provides a smooth acceleration and deceleration effect.
         * The function is defined as follows:
         * - If t is less than 0.5, it returns 2 * t^2.
         * - Otherwise, it returns -1 + (4 - 2 * t) * t.
         *
         * @param t The input value in the range [0, 1] representing the progress of the easing.
         * @return The result of applying the ease-in-out quadratic easing function to the input.
         */
        private fun easeInOutQuad(t: Double) = if (t < 0.5) 2 * t * t else -1 + (4 - 2 * t) * t

        /**
         * Updates the camera's position and orientation based on specified delta values.
         *
         * @param deltaX The change in the X-coordinate.
         * @param deltaY The change in the Y-coordinate.
         * @param deltaZ The change in the Z-coordinate.
         * @param deltaYaw The change in the yaw rotation.
         * @param deltaPitch The change in the pitch rotation.
         */
        private fun updateCamera(
            deltaX: Double = 0.0,
            deltaY: Double = 0.0,
            deltaZ: Double = 0.0,
            deltaYaw: Float = 0F,
            deltaPitch: Float = 0F
        ) = itemDisplay.apply {
            teleport(
                location.add(deltaX.eased(), deltaY.eased(), deltaZ.eased()).addYawPitch(deltaYaw.eased(), deltaPitch.eased()),
                TeleportFlag.EntityState.RETAIN_PASSENGERS
            )
        }.also { progress() }

        /**
         * Applies easing to the current value using the ease-in-out quadratic function.
         *
         * @return The eased value.
         */
        private fun Double.eased() = this * easeInOutQuad(progress)

        /**
         * Applies easing to the current value using the ease-in-out quadratic function.
         *
         * @return The eased value.
         */
        private fun Float.eased() = (this * easeInOutQuad(progress)).toFloat()

        /**
         * Advances the progress of the camera movement, and triggers the next stage if the progress is complete.
         */
        private fun progress() {
            progress += 1.0 / duration

            if (progress >= 1.0 - epsilon) {
                progress = 0.0
                stage++
            }
        }

        /**
         * Adds the specified yaw and pitch values to the location's current yaw and pitch.
         *
         * @param yaw The change in yaw.
         * @param pitch The change in pitch.
         * @return The modified location with the updated yaw and pitch.
         */
        private fun Location.addYawPitch(yaw: Float, pitch: Float) = apply {
            this.yaw += yaw
            this.pitch += pitch
        }
    }
}

/**
 * Used for destructing Bukkit Location
 */
private operator fun Location.component1(): Double = x
private operator fun Location.component2(): Double = y
private operator fun Location.component3(): Double = z
private operator fun Location.component4(): Float = yaw
private operator fun Location.component5(): Float = pitch