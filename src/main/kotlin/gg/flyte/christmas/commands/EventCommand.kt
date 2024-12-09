package gg.flyte.christmas.commands

import dev.shreyasayyengar.menuapi.menu.MenuItem
import dev.shreyasayyengar.menuapi.menu.StandardMenu
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donation.DonateEvent
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.npc.WorldNPC
import gg.flyte.christmas.util.colourise
import gg.flyte.christmas.util.eventController
import gg.flyte.christmas.util.style
import gg.flyte.christmas.util.toLegacyString
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.async
import gg.flyte.twilight.scheduler.sync
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

/**
 * Miscellaneous commands to manage the event.
 */
@Suppress("unused") // power of lamp!
class EventCommand(val menu: StandardMenu = StandardMenu("&c☃ Event Menu!".colourise(), 54)) {
    private val availableGames = GameConfig.entries
    private var selectedIndex = -1
    private var modifyingGame: UUID? = null

    init {
        menu.setItem(13, setGameSwitcher())

        menu.setItem(
            21, MenuItem(Material.PAINTING)
                .setName("&cTake a Screenie!".colourise())
                .setEnchantmentGlint(true)
                .closeWhenClicked(true)
                .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                    // TODO
                    whoClicked.sendMessage("<red>Coming soon!".style())
                }
        )
        menu.setItem(
            23, MenuItem(Material.ENDER_PEARL)
                .setName("&cTeleport Everyone to Me!".colourise())
                .closeWhenClicked(true)
                .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                    Bukkit.getOnlinePlayers().forEach { it.teleport(whoClicked) }
                    whoClicked.sendMessage("<green>Teleported all players to you!".style())
                    whoClicked.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                }
        )

        menu.setItem(42, setEndGameButton())

        menu.onClose { whoClosed, inventory, event ->
            if (whoClosed.uniqueId != modifyingGame) return@onClose // not the one who interacted with the game switcher

            modifyingGame = null
            eventController().setMiniGame(availableGames[selectedIndex])
            eventController().sidebarManager.update()
            whoClosed.sendMessage("<grey>Selected game: <0>".style(availableGames[selectedIndex].displayName))
            whoClosed.playSound(Sound.UI_BUTTON_CLICK)
        }
    }

    /**
     * Opens the event panel for the player, provided that they have the
     * permission `event.panel`.
     *
     * @param sender The player who executed the command.
     */
    @Command("event")
    @CommandPermission("event.panel")
    fun openEventPanel(sender: Player) {
        menu.setItem(42, setEndGameButton())
        menu.open(true, sender)
    }

    /**
     * Opt out of the event, or opt back in if the player is already opted out.
     *
     * Players who opt out will not be marked as an active player when minigames start,
     * and instead will teleport them to a viewing area immediately.
     *
     * @param sender The player who executed the command.
     */
    @Command("event optout")
    @CommandPermission("event.optout")
    fun optOut(sender: Player) {
        var remove = eventController().optOut.remove(sender.uniqueId)
        if (remove) {
            sender.sendMessage("<green>You have opted back into the event!".style())
        } else {
            eventController().optOut.add(sender.uniqueId)
            sender.sendMessage("<red>You have opted out of the event!".style())
        }

        sender.playSound(Sound.UI_BUTTON_CLICK)
    }

    /**
     * Loads the most recent points data from the `config.yml`.
     *
     * @param sender The player who executed the command.
     */
    @Command("event DANGER-load-crash")
    @CommandPermission("event.loadcrash")
    fun loadCrash(sender: CommandSender) {
        async {
            eventController().points.clear()
            ChristmasEventPlugin.instance.config.getConfigurationSection("points")?.getKeys(false)?.forEach {
                eventController().points[UUID.fromString(it)] = ChristmasEventPlugin.instance.config.getInt("points.$it")
            }
            sync {
                eventController().sidebarManager.update()
                WorldNPC.refreshPodium()
                sender.sendMessage("<green>Loaded crash data! Your scoreboard should now show the most recent serialised data!".style())
            }
        }
    }

    @Command("donate")
    fun donate(sender: Player) {
        var book = Book.book(
            "<red>Donating Info".style(), Component.empty(),
            Component.empty()
                .append("".style())
                .append("<black>ᴛʜɪs ᴄʜʀɪsᴛᴍᴀs ᴇᴠᴇɴᴛ ɪs ɪɴ sᴜᴘᴘᴏʀᴛ ᴏꜰ ᴛʜᴇ <colour:#ec8339>ʙᴇsᴛ ꜰʀɪᴇɴᴅs ᴀɴɪᴍᴀʟ sᴏᴄɪᴇᴛʏ, ".style())
                .append("<black>ᴀ ʟᴇᴀᴅɪɴɢ ᴜ.s. ɴᴀᴛɪᴏɴᴀʟ ᴀɴɪᴍᴀʟ ᴡᴇʟꜰᴀʀᴇ ᴏʀɢᴀɴɪsᴀᴛɪᴏɴ.\n".style())
                .append("".style())
                .append("<colour:#ff3d9b>ᴛʜᴇ ʙᴇsᴛ ᴘᴀʀᴛ? <black>ᴇᴠᴇʀʏ ᴅᴏʟʟᴀʀ ʏᴏᴜ ᴘᴜᴛ ꜰᴏᴜʀᴛʜ ᴡɪʟʟ ʙᴇ <b><colour:#ae61f2>ᴛʀɪᴘᴘʟᴇ ᴍᴀᴛᴄʜᴇᴅ<reset><black>!\n".style())
                .append(
                    "\n<black><0> ᴛᴏ ᴅᴏɴᴀᴛᴇ ɴᴏᴡ!".style(
                        "<colour:#ff80e8>ꜰʟʏᴛᴇ.ɢɢ/ᴅᴏɴᴀᴛᴇ".style().clickEvent(ClickEvent.openUrl("https://flyte.gg/donate"))
                    )
                )
        )

        sender.openBook(book)
    }

    @Command("leaderboard")
    fun showLeaderboard(sender: Player) {
        // TODO show leaderboard
    }

    @Command("event mock-donation-now <amount>")
    @CommandPermission("event.mockdonation")
    fun mockDonation(sender: Player, amount: Double) {
        val donationEvent = DonateEvent(UUID.randomUUID().toString(), sender.name, null, amount.toString(), System.currentTimeMillis())
        Bukkit.getPluginManager().callEvent(donationEvent)
    }

    @Command("event mock-donation-now <amount> <target>")
    @CommandPermission("event.mockdonation")
    fun mockDonation(sender: Player, amount: Double, target: Player) {
        val donationEvent = DonateEvent(UUID.randomUUID().toString(), sender.name, null, amount.toString(), System.currentTimeMillis())
        Bukkit.getPluginManager().callEvent(donationEvent)
    }

    private fun setGameSwitcher(): MenuItem {
        val menuItem = MenuItem(Material.STRUCTURE_VOID).apply {
            setName("&b&lSelect Game:".colourise())
            updateGameSwitcher(this) // initial lore setup
            onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                inventoryClickEvent.isCancelled = true

                if (modifyingGame != null && modifyingGame != whoClicked.uniqueId) {
                    whoClicked.closeInventory()
                    whoClicked.sendMessage("<red>Someone else is currently modifying the game!".style())
                    whoClicked.playSound(Sound.ENTITY_VILLAGER_NO)
                    return@onClick
                }

                modifyingGame = whoClicked.uniqueId

                if (clickType.name.lowercase().contains("left")) {
                    selectedIndex = (selectedIndex + 1) % availableGames.size // cycle around
                } else if (clickType.name.lowercase().contains("right")) {
                    selectedIndex = if (selectedIndex == -1 || selectedIndex == 0) availableGames.size - 1 else selectedIndex - 1
                }

                this.itemStack = availableGames[selectedIndex].menuItem
                setName("&b&lSelect Game:".colourise())
                updateGameSwitcher(this)

                menu.setItem(13, this)
                whoClicked.playSound(Sound.UI_BUTTON_CLICK)
            }
        }

        return menuItem
    }

    private fun updateGameSwitcher(menuItem: MenuItem) {
        val lore = mutableListOf<String>()

        for (index in availableGames.indices) {
            val game = availableGames[index]
            val loreLine = if (index == selectedIndex) {
                "<white><bold>» <0> <white><bold>«".style(game.displayName).toLegacyString().colourise()
            } else game.displayName.toLegacyString().colourise()

            lore.add(loreLine)
        }

        menuItem.setLore(lore)

        if (selectedIndex == -1) return

        menu.setItem(
            38, MenuItem(Material.LIME_CONCRETE)
                .setName(availableGames[selectedIndex].displayName.toLegacyString().colourise())
                .setLore(
                    "",
                    "&aThis will begin the countdown".colourise(),
                    "&cimmediately &aand prepare the players".colourise(),
                    "",
                    "&eIf you do not want to start".colourise(),
                    "&eyet, simply exit this menu. ".colourise(),
                    "&eThe game has already been set.".colourise()
                )
                .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                    whoClicked.closeInventory()
                    if (eventController().currentGame == null) {
                        whoClicked.playSound(Sound.ENTITY_VILLAGER_NO)
                        whoClicked.sendMessage("<red>No game has been selected!".style())
                        return@onClick
                    }

                    eventController().prepareStart()
                    whoClicked.playSound(Sound.ENTITY_PLAYER_LEVELUP)
                    whoClicked.sendMessage("<green>Game starting! Please wait...".style())

                    selectedIndex = -1
                    menu.setItem(13, setGameSwitcher())
                    menu.removeItem(38)
                }
        )
    }

    private fun setEndGameButton(): MenuItem {
        return MenuItem(Material.RED_CONCRETE)
            .setName(
                "<red>Kill Current Game: <0>".style(eventController().currentGame?.gameConfig?.displayName ?: "None".style()).toLegacyString()
                    .colourise()
            )
            .setLore(
                "",
                "&cThis will force quit the current game".colourise(),
                "&cand teleport all players back to the lobby.".colourise(),
            )
            .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                if (eventController().currentGame == null) {
                    whoClicked.playSound(Sound.ENTITY_VILLAGER_NO)
                    whoClicked.sendMessage("<red>No game is currently running!".style())
                    return@onClick
                }

                eventController().currentGame!!.endGame()
                whoClicked.sendMessage("<red>Game terminated!".style())
                whoClicked.playSound(Sound.ENTITY_GENERIC_EXPLODE)
                eventController().sidebarManager.update()
            }
    }
}