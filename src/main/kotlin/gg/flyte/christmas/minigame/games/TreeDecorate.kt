package gg.flyte.christmas.minigame.games

import dev.shreyasayyengar.menuapi.menu.MenuItem
import gg.flyte.christmas.donation.DonationTier
import gg.flyte.christmas.minigame.engine.EventMiniGame
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.minigame.engine.PlayerType
import gg.flyte.christmas.util.*
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.time.TimeUnit
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.time.Duration

class TreeDecorate : EventMiniGame(GameConfig.TREE_DECORATE) {
    private var booleanCanDesecrate = false
    private val decorationTextureURLs = listOf(
        "515bc1aa72d2452dcaf1178ab252bcbe62c9a5741c8e69edd7ac44d46a2d93e6",
        "c48f9c52a2f82b013cfa632b42ee4ce419ea26802e2589d7631bcb828085f38f",
        "5e39248b341c87ce3e4294ac214c6f74468889cb1273ae7412906fd28db097e8",
        "8e717b485d95b3bd14db1350c55a29da99ee1d3195c050617088a9677ac42",
        "47a985c31ad461e322b39c433eae79ff46fb64800cdecadf2f6d0f9727252",
        "940da99ea4718907f17190eb15352c0da0de0cee186d4fabf6158f81926a504f",
        "2db24dc262631663ba1e3e3398645013dc5cd2331ec9b9f3eb26855a0b104baa",
        "1e3ad039e903e30f90daa68cebfc5cee72b5ed84d6044382409c67f374d1732b",
        "e258b0b460dee9e67b59f69808caa5db4665969b4b30af43d0e086a133645318",
        "e040b03876580350dbf81333aea696a6d2f3f7d5156fb0ce25771283df609a9f",
        "21bc9d42b0041e8f95cb9b26628fdaf50cd0e36f7bb9d6b3a4d2af3949da97d6",
        "df5898ad54e634c59fab5b284d49b3e25d015512caa3ab5620cecf00b84f1345"
    )

    override fun preparePlayer(player: Player) {
        player.formatInventory()
        player.gameMode = GameMode.CREATIVE
        player.teleport(gameConfig.spawnPoints.random().randomLocation())

        for (texture in decorationTextureURLs) {
            var menuItem = MenuItem(Material.PLAYER_HEAD)
                .setName("Decoration")
                .setSkullTexture(texture)
            player.inventory.addItem(menuItem.itemStack)
        }

        player.inventory.addItem(ItemStack(Material.BEACON))
        player.inventory.addItem(ItemStack(Material.LANTERN))
        player.inventory.addItem(ItemStack(Material.IRON_BARS))
        player.inventory.addItem(ItemStack(Material.BELL))
        player.inventory.addItem(ItemStack(Material.RED_MUSHROOM_BLOCK))
    }

    override fun startGame() {
        donationEventsEnabled = true

        Util.runAction(PlayerType.PARTICIPANT) {
            it.title(
                "<game_colour>ʜᴀᴘᴘʏ ᴅᴇᴄᴏʀᴀᴛɪɴɢ!".style(), Component.empty(),
                titleTimes(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            )
            eventController().songPlayer?.isPlaying = false
            it.playSound(Sound.MUSIC_DISC_CHIRP)
        }

        delay(15, TimeUnit.MINUTES) { allowDesecration() }

        delay(200, TimeUnit.SECONDS) {
            eventController().songPlayer?.isPlaying = true // Santa Tell Me would now be over.
        }
    }

    private fun allowDesecration() {
        booleanCanDesecrate = true
        Util.runAction(PlayerType.PARTICIPANT) {
            it.title(
                "<game_colour>ꜰᴜʟʟ ᴄʀᴇᴀᴛɪᴠᴇ ɪɴᴠᴇɴᴛᴏʀʏ!".style(), "<grey>ɪs ɴᴏᴡ ᴜɴʟᴏᴄᴋᴇᴅ".style(),
                titleTimes(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            )
            it.playSound(Sound.ENTITY_EVOKER_FANGS_ATTACK)
        }
    }

    override fun handleGameEvents() {
        listeners += event<PlayerInteractEvent> {
            if (!hasItem()) return@event

            when (item!!.type) {
                Material.BUCKET,
                Material.WATER_BUCKET,
                Material.LAVA_BUCKET,
                Material.POWDER_SNOW_BUCKET,
                Material.TROPICAL_FISH_BUCKET,
                Material.COD_BUCKET,
                Material.PUFFERFISH_BUCKET,
                Material.SALMON_BUCKET,
                Material.AXOLOTL_BUCKET,
                Material.TADPOLE_BUCKET,
                Material.END_CRYSTAL,
                Material.TNT_MINECART,
                Material.FLINT_AND_STEEL,
                Material.FIRE_CHARGE,
                Material.FIREWORK_ROCKET,
                Material.TNT
                    -> {
                    if (!booleanCanDesecrate) {
                        isCancelled = true
                        player.sendMessage("<red>ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴅᴏ ᴛʜᴀᴛ! ᴀᴛ ʟᴇᴀsᴛ ɴᴏᴛ ʏᴇᴛ...".style())
                        player.playSound(Sound.BLOCK_NOTE_BLOCK_BASS)
                    }
                }

                else -> {
                    if (material.name.lowercase().contains("spawn")) {
                        if (!booleanCanDesecrate) {
                            isCancelled = true
                            player.sendMessage("<red>ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴅᴏ ᴛʜᴀᴛ! ᴀᴛ ʟᴇᴀsᴛ ɴᴏᴛ ʏᴇᴛ...".style())
                            player.playSound(Sound.BLOCK_NOTE_BLOCK_BASS)
                        }
                    }
                }
            }
        }

        listeners += event<BlockPlaceEvent> {
            when (block.type) {
                Material.TNT,
                    -> {
                    if (!booleanCanDesecrate) {
                        isCancelled = true
                        player.sendMessage("<red>ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴅᴏ ᴛʜᴀᴛ! ᴀᴛ ʟᴇᴀsᴛ ɴᴏᴛ ʏᴇᴛ...".style())
                        player.playSound(Sound.BLOCK_NOTE_BLOCK_BASS)
                    }
                }

                else -> {}
            }
        }
    }

    override fun handleDonation(tier: DonationTier, donorName: String?) {
        when (tier) {
            DonationTier.LOW -> {}
            DonationTier.MEDIUM -> {}
            DonationTier.HIGH -> allowDesecration()
        }
    }
}
