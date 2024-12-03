package gg.flyte.christmas.minigame.engine

import dev.shreyasayyengar.menuapi.menu.MenuItem
import gg.flyte.christmas.minigame.games.*
import gg.flyte.christmas.minigame.world.MapRegion
import gg.flyte.christmas.minigame.world.MapSinglePoint
import gg.flyte.christmas.util.style
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.reflect.KClass

enum class GameConfig(
    val gameClass: KClass<out EventMiniGame>,
    val displayName: Component,
    val smallDisplayName: Component,
    val menuItem: ItemStack,
    val colour: TextColor,
    val instructions: String,
    val minPlayers: Int,
    val centrePoint: MapSinglePoint,
    val spawnPoints: List<MapRegion>,
    val overviewLocations: List<MapSinglePoint>,
    val spectatorSpawnLocations: List<MapSinglePoint>,
    val spectatorCameraLocations: List<MapSinglePoint>,
    val eliminateOnLeave: Boolean = true,
) {
    //region AVALANCHE
    AVALANCHE(
        Avalanche::class,
        "<colour:#33ff8b>Avalanche".style(),
        "<colour:#33ff8b>ᴀᴠᴀʟᴀɴᴄʜᴇ".style(),
        ItemStack(Material.SNOWBALL),
        TextColor.fromHexString("#33ff8b")!!,
        " • ᴅᴀɴᴄᴇ ᴀʀᴏᴜɴᴅ ᴛʜᴇ ɪᴄʏ ᴘʟᴀᴛꜰᴏʀᴍ ᴜɴᴛɪʟ ᴛʜᴇ ᴄʜʀɪѕᴛᴍᴀѕ ᴍᴜѕɪᴄ ѕᴛᴏᴘѕ.\n\n" +
                " • ʀᴜɴ ᴀɴᴅ ѕᴛᴀɴᴅ ᴜɴᴅᴇʀ ѕʜᴇʟᴛᴇʀ ᴀѕ ᴛʜᴇ ѕɴᴏᴡʙᴀʟʟѕ ꜰᴀʟʟ ꜰʀᴏᴍ ᴛʜᴇ ѕᴋʏ.\n\n" +
                " • ɪꜰ ʏᴏᴜ ᴀʀᴇ ʜɪᴛ ʙʏ ᴀ ꜰᴀʟʟɪɴɢ ѕɴᴏᴡʙᴀʟʟ, ʏᴏᴜ ᴡɪʟʟ ʙᴇ ᴇʟɪᴍɪɴᴀᴛᴇᴅ!",
        2,
        MapSinglePoint(616.5, 140.0, 800.5, 0F, 90F),
        listOf(
            MapRegion(
                MapSinglePoint(624, 111, 808),
                MapSinglePoint(604, 111, 788)
            )
        ),
        listOf(
            MapSinglePoint(615, 170, 730, -0.9721985F, 39.654617F),
            MapSinglePoint(617, 122, 773, -4.1285706F, 19.746199F),
            MapSinglePoint(619, 116, 795, -1.700592F, 12.624509F),
            MapSinglePoint(619, 116, 809, 31.07602F, 13.271932F),
            MapSinglePoint(612, 116, 818, 98.003815F, 11.572447F),
            MapSinglePoint(607, 116, 820, 173.26697F, 3.2368581F),
            MapSinglePoint(602, 116, 815, -170.06134F, -7.7693725F),
            MapSinglePoint(591, 116, 809, -132.10449F, -23.631304F),
            MapSinglePoint(589, 116, 796, -89.04901F, -30.267466F),
            MapSinglePoint(598, 116, 785, -53.115173F, -28.891676F),
            MapSinglePoint(613, 116, 782, -20.256775F, -24.683378F),
            MapSinglePoint(626, 116, 788, 28.464172F, -28.163317F),
            MapSinglePoint(631, 116, 800, 75.80859F, -31.724184F),
            MapSinglePoint(624, 116, 810, 103.648865F, -26.625673F),
            MapSinglePoint(621, 116, 812, 116.031555F, -17.56166F),
            MapSinglePoint(621, 116, 813, 117.73114F, -15.619374F),
        ),
        listOf(
            MapSinglePoint(645.5, 112, 799.5, 90, 0),
            MapSinglePoint(636.5, 113, 820.5, 135, 0),
            MapSinglePoint(616.5, 112, 829.5, -180, 0),
            MapSinglePoint(596.5, 113, 820.5, -135, 0),
            MapSinglePoint(587.5, 112, 800.5, -90, 0),
            MapSinglePoint(596.5, 113, 780.5, -45, 0),
            MapSinglePoint(616.5, 112, 771.5, 0, 0),
            MapSinglePoint(636.5, 113, 780.5, 45, 0)
        ),
        listOf(
            MapSinglePoint(616.5, 118, 819.5, 180, 35),
            MapSinglePoint(597.5, 118, 800.5, -90, 35),
            MapSinglePoint(616.5, 118, 781.5, 0, 35),
            MapSinglePoint(635.5, 118, 800.5, 90, 35)
        )
    ),
    //endregion

    //region BAUBLE_TAG
    BAUBLE_TAG(
        BaubleTag::class,
        "<colour:#aed3d8>Bauble Tag".style(),
        "<colour:#aed3d8>ʙᴀᴜʙʟᴇ ᴛᴀɢ".style(),
        MenuItem(Material.PLAYER_HEAD).setSkullTexture("1e3ad039e903e30f90daa68cebfc5cee72b5ed84d6044382409c67f374d1732b").itemStack,
        TextColor.fromHexString("#aed3d8")!!,
        " • ʀᴜɴ ᴀᴡᴀʏ ꜰʀᴏᴍ ᴛʜᴇ ᴘʟᴀʏᴇʀѕ ᴡɪᴛʜ ʙᴀᴜʙʟᴇѕ ᴏɴ ᴛʜᴇɪʀ ʜᴇᴀᴅ.\n\n" +
                " • ɪꜰ ʏᴏᴜ ᴀʀᴇ ᴛᴀɢɢᴇᴅ, ʏᴏᴜ ᴡɪʟʟ ʙᴇᴄᴏᴍᴇ ᴛʜᴇ ɴᴇᴡ ʙᴀᴜʙʟᴇ ʜᴏʟᴅᴇʀ.\n\n" +
                " • ᴛʜᴇ ʙᴀᴜʙʟᴇѕ ᴡɪʟʟ ѕʜᴀᴛᴛᴇʀ ᴀᴛ ᴛʜᴇ ᴇɴᴅ ᴏꜰ ᴇᴀᴄʜ ʀᴏᴜɴᴅ.\n\n" +
                " • ᴛᴏ ᴡɪɴ, ѕᴜʀᴠɪᴠᴇ ᴛʜᴇ ᴍᴏѕᴛ ʀᴏᴜɴᴅѕ!",
        2,
        MapSinglePoint(181.5, 140.0, 300.5, -135, 90),
        listOf(
            MapRegion.single(MapSinglePoint(193, 80, 193, 0, 0)),
            MapRegion.single(MapSinglePoint(207, 82, 221, 0, 0)),
            MapRegion.single(MapSinglePoint(206, 80, 251, 0, 0)),
            MapRegion.single(MapSinglePoint(188, 76, 262, 0, 0)),
            MapRegion.single(MapSinglePoint(191, 72, 290, 0, 0)),
            MapRegion.single(MapSinglePoint(209, 71, 295, 0, 0)),
            MapRegion.single(MapSinglePoint(229, 73, 300, 0, 0)),
            MapRegion.single(MapSinglePoint(250, 76, 294, 0, 0)),
            MapRegion.single(MapSinglePoint(265, 78, 304, 0, 0)),
            MapRegion.single(MapSinglePoint(271, 76, 319, 0, 0)),
            MapRegion.single(MapSinglePoint(277, 75, 334, 0, 0)),
            MapRegion.single(MapSinglePoint(285, 75, 350, 0, 0)),
            MapRegion.single(MapSinglePoint(298, 75, 370, 0, 0)),
            MapRegion.single(MapSinglePoint(291, 75, 383, 0, 0)),
            MapRegion.single(MapSinglePoint(281, 75, 398, 0, 0)),
            MapRegion.single(MapSinglePoint(262, 75, 403, 0, 0)),
            MapRegion.single(MapSinglePoint(249, 77, 415, 0, 0)),
            MapRegion.single(MapSinglePoint(233, 79, 422, 0, 0)),
            MapRegion.single(MapSinglePoint(211, 81, 428, 0, 0)),
            MapRegion.single(MapSinglePoint(201, 77, 407, 0, 0)),
            MapRegion.single(MapSinglePoint(194, 76, 394, 0, 0)),
            MapRegion.single(MapSinglePoint(201, 74, 378, 0, 0)),
            MapRegion.single(MapSinglePoint(206, 72, 364, 0, 0)),
            MapRegion.single(MapSinglePoint(208, 71, 347, 0, 0)),
            MapRegion.single(MapSinglePoint(199, 71, 333, 0, 0)),
            MapRegion.single(MapSinglePoint(172, 73, 298, 0, 0)),
            MapRegion.single(MapSinglePoint(143, 79, 279, 0, 0)),
            MapRegion.single(MapSinglePoint(141, 77, 244, 0, 0)),
            MapRegion.single(MapSinglePoint(97, 89, 254, 0, 0)),
            MapRegion.single(MapSinglePoint(114, 86, 215, 0, 0))
        ),
        listOf(
            MapSinglePoint(173, 99, 307, 175.59639F, 24.933584F),
            MapSinglePoint(172, 93, 293, 172.44003F, 21.049002F),
            MapSinglePoint(172, 88, 278, 150.75089F, 10.609256F),
            MapSinglePoint(166, 86, 264, 148.64664F, 3.6494362F),
            MapSinglePoint(156, 86, 252, -178.25386F, 1.3834488F),
            MapSinglePoint(147, 88, 239, -110.35533F, 3.8112924F),
            MapSinglePoint(151, 93, 226, -85.26666F, 10.4474F),
            MapSinglePoint(161, 99, 215, -74.25983F, 10.932967F),
            MapSinglePoint(175, 105, 206, -61.796204F, 15.060287F),
            MapSinglePoint(190, 109, 204, -24.243927F, 27.28051F),
            MapSinglePoint(198, 105, 214, -11.05191F, 30.517662F),
            MapSinglePoint(202, 100, 229, -11.132843F, 25.25729F),
            MapSinglePoint(206, 94, 245, -13.479889F, 19.349497F),
            MapSinglePoint(209, 88, 260, -12.2659F, 17.73092F),
            MapSinglePoint(213, 83, 276, -16.474396F, 14.493787F),
            MapSinglePoint(218, 77, 292, -21.168488F, 12.065951F),
            MapSinglePoint(223, 73, 307, -24.32486F, 4.2968564F)
        ),
        listOf(MapSinglePoint(554, 204, 594, -88.70047F, 88.381485F)),
        listOf(MapSinglePoint(554, 204, 594, -88.70047F, 88.381485F)),
    ),
    //endregion

    //region BLOCK_PARTY
    BLOCK_PARTY(
        BlockParty::class,
        "<colour:#e05c6f>Block Party".style(),
        "<colour:#e05c6f>ʙʟᴏᴄᴋ ᴘᴀʀᴛʏ".style(),
        ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA),
        TextColor.fromHexString("#e05c6f")!!,
        " • ᴅᴀɴᴄᴇ ᴀʀᴏᴜɴᴅ ᴛʜᴇ ᴄᴏʟᴏᴜʀꜰᴜʟ ꜰʟᴏᴏʀ ᴜɴᴛɪʟ ᴛʜᴇ ᴄʜʀɪѕᴛᴍᴀѕ ᴍᴜѕɪᴄ ѕᴛᴏᴘѕ.\n\n" +
                " • ʀᴜɴ ᴀɴᴅ ѕᴛᴀɴᴅ ᴏɴ ᴛʜᴇ ᴄᴏʟᴏᴜʀ ᴡʜɪᴄʜ ʜᴀѕ ʙᴇᴇɴ ᴄʜᴏѕᴇɴ (ᴄʜᴇᴄᴋ ʜᴏᴛʙᴀʀ) ʙᴇꜰᴏʀᴇ ᴛʜᴇ ᴛɪᴍᴇʀ ᴇɴᴅѕ.\n\n" +
                " • ᴀꜰᴛᴇʀ ᴛʜᴇ ᴛɪᴍᴇʀ, ᴛʜᴇ ᴍᴀᴘ ᴡɪʟʟ ᴄʟᴇᴀʀ ᴀʟʟ ʙʟᴏᴄᴋѕ ᴇxᴄᴇᴘᴛ ᴛʜᴇ ᴄʜᴏѕᴇɴ ᴄᴏʟᴏᴜʀ ʙʟᴏᴄᴋѕ. ᴛᴏ ᴡɪɴ, ѕᴜʀᴠɪᴠᴇ ᴛʜᴇ ᴍᴏѕᴛ ʀᴏᴜɴᴅѕ!",
        2,
        MapSinglePoint(616.5, 140.0, 800.5, 0F, 90F),
        listOf(
            MapRegion(
                MapSinglePoint(624, 111, 808),
                MapSinglePoint(604, 111, 788)
            )
        ),
        listOf(
            MapSinglePoint(677, 148, 755, 48.056213F, 85.83555F),
            MapSinglePoint(673, 142, 760, 52.780945F, 64.90654F),
            MapSinglePoint(667, 137, 765, 57.357727F, 32.46672F),
            MapSinglePoint(663, 132, 772, 63.205994F, 14.779753F),
            MapSinglePoint(660, 132, 780, 69.97186F, 10.741306F),
            MapSinglePoint(657, 132, 787, 75.96295F, 17.760796F),
            MapSinglePoint(653, 132, 794, 82.90668F, 27.45739F),
            MapSinglePoint(648, 132, 801, 93.19208F, 34.85472F),
            MapSinglePoint(642, 132, 807, 103.764404F, 38.339962F),
            MapSinglePoint(635, 132, 811, 114.30951F, 40.91138F),
            MapSinglePoint(627, 132, 814, 127.10217F, 44.83077F),
            MapSinglePoint(619, 132, 815, 144.70087F, 50.181454F),
            MapSinglePoint(611, 132, 814, 163.02673F, 54.77303F),
            MapSinglePoint(604, 132, 811, -172.36804F, 56.458115F),
            MapSinglePoint(599, 130, 805, -134.58829F, 57.185158F),
            MapSinglePoint(598, 127, 797, -98.27765F, 52.314674F),
            MapSinglePoint(602, 121, 791, -68.66922F, 44.33963F),
            MapSinglePoint(609, 117, 787, -40.830444F, 34.319355F),
            MapSinglePoint(617, 117, 787, -5.60672F, 21.597097F),
        ),
        listOf(
            MapSinglePoint(645.5, 112, 799.5, 90, 0),
            MapSinglePoint(636.5, 113, 820.5, 135, 0),
            MapSinglePoint(616.5, 112, 829.5, -180, 0),
            MapSinglePoint(596.5, 113, 820.5, -135, 0),
            MapSinglePoint(587.5, 112, 800.5, -90, 0),
            MapSinglePoint(596.5, 113, 780.5, -45, 0),
            MapSinglePoint(616.5, 112, 771.5, 0, 0),
            MapSinglePoint(636.5, 113, 780.5, 45, 0)
        ),
        listOf(
            MapSinglePoint(616.5, 118, 819.5, 180, 35),
            MapSinglePoint(597.5, 118, 800.5, -90, 35),
            MapSinglePoint(616.5, 118, 781.5, 0, 35),
            MapSinglePoint(635.5, 118, 800.5, 90, 35)
        )
    ),
    //endregion

    //region KING_OF_THE_HILL
    KING_OF_THE_HILL(
        KingHill::class,
        "<colour:#f70123>King of the Hill".style(),
        "<colour:#f70123>ᴋɪɴɢ ᴏғ ᴛʜᴇ ʜɪʟʟ".style(),
        ItemStack(Material.STICK),
        TextColor.fromHexString("#f70123")!!,
        " • ʙᴇ ᴛʜᴇ ᴋɪɴɢ ᴏꜰ ᴛʜᴇ ʜɪʟʟ! ѕᴛᴀɴᴅ ɪɴ ᴛʜᴇ ᴄᴇɴᴛʀᴇ ᴏꜰ ᴛʜᴇ ᴍᴀᴘ ᴛᴏ ɢᴀɪɴ ᴘᴏɪɴᴛѕ ᴇᴠᴇʀʏ ѕᴇᴄᴏɴᴅ.\n\n" +
                " • ᴋɴᴏᴄᴋ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀѕ ᴏꜰꜰ ᴛʜᴇ ᴍᴀᴘ ᴡɪᴛʜ ʏᴏᴜʀ ᴋɴᴏᴄᴋʙᴀᴄᴋ ѕᴛɪᴄᴋ. ᴀᴠᴏɪᴅ ꜰᴀʟʟɪɴɢ ᴏꜰꜰ ʏᴏᴜʀѕᴇʟꜰ!\n\n" +
                " • ᴛʜᴇ ᴘʟᴀʏᴇʀ ѕᴛᴏᴏᴅ ɪɴ ᴛʜᴇ ᴄᴇɴᴛʀᴇ ᴏꜰ ᴛʜᴇ ᴍᴀᴘ ꜰᴏʀ ᴛʜᴇ ʟᴏɴɢᴇѕᴛ ᴛɪᴍᴇ, ᴡɪɴѕ!",
        1,
        MapSinglePoint(827.5, 111, 630.5, -90F, 90F),
        listOf(
            MapRegion(
                MapSinglePoint(826.5, 88, 680.5, 180, 0),
                MapSinglePoint(836.5, 88, 675.5, 180, 0)
            ),
            MapRegion(
                MapSinglePoint(861.5, 85, 656.5, 135, 0),
                MapSinglePoint(855.5, 85, 660.5, 135, 0)
            ),
            MapRegion.single(MapSinglePoint(798.5, 86, 662.5, -135, 0)),
            MapRegion.single(MapSinglePoint(787.5, 86, 630.5, -90, 0)),
            MapRegion(
                MapSinglePoint(792.5, 85, 601.5, -45, 0),
                MapSinglePoint(797.5, 85, 604.5, -45, 0)
            ),
            MapRegion(
                MapSinglePoint(820.5, 85, 587.5, 0, 0),
                MapSinglePoint(825.5, 85, 588.5, 0, 0)
            ),
            MapRegion(
                MapSinglePoint(857.5, 85, 597.5, 45, 0),
                MapSinglePoint(853.5, 85, 601.5, 45, 0)
            ),
            MapRegion(
                MapSinglePoint(867.5, 86, 627.5, 90, 0),
                MapSinglePoint(866.5, 86, 626.5, 90, 0)
            ),
            MapRegion(
                MapSinglePoint(861.5, 85, 660.5, 135, 0),
                MapSinglePoint(857.5, 85, 656.5, 135, 0)
            )
        ),
        listOf(
            MapSinglePoint(827, 88, 587, 179.57143F, 74.42298F),
            MapSinglePoint(827, 88, 594, 179.85931F, 67.05221F),
            MapSinglePoint(827, 88, 602, 179.89876F, 56.5931F),
            MapSinglePoint(827, 88, 610, 179.67241F, 46.63012F),
            MapSinglePoint(827, 88, 618, 179.56989F, 35.635384F),
            MapSinglePoint(827, 88, 626, -179.87363F, 24.453075F),
            MapSinglePoint(827, 88, 634, -179.41673F, 15.988203F),
            MapSinglePoint(826, 88, 642, -176.94571F, 9.467426F),
            MapSinglePoint(821, 88, 648, -164.89076F, 6.4826865F),
            MapSinglePoint(814, 88, 650, -149.47144F, 6.4154325F),
            MapSinglePoint(808, 88, 646, -131.84286F, 7.456919F),
            MapSinglePoint(802, 88, 641, -114.1622F, 7.854647F),
            MapSinglePoint(799, 88, 634, -97.394775F, 7.868369F),
            MapSinglePoint(798, 88, 626, -78.14661F, 8.061364F),
            MapSinglePoint(799, 88, 618, -62.56653F, 7.6483693F),
            MapSinglePoint(803, 88, 611, -48.001343F, 7.379387F),
            MapSinglePoint(808, 88, 605, -33.123077F, 7.502257F),
            MapSinglePoint(815, 88, 600, -17.081573F, 8.007061F),
            MapSinglePoint(823, 88, 598, -4.5716248F, 8.615353F),
            MapSinglePoint(833, 90, 605, 13.289856F, 13.035935F),
            MapSinglePoint(841, 96, 611, 44.075134F, 23.785404F),
            MapSinglePoint(847, 101, 617, 70.41861F, 34.081383F),
            MapSinglePoint(850, 107, 625, 90.50464F, 43.07204F),
            MapSinglePoint(850, 113, 633, 105.48248F, 49.530624F),
            MapSinglePoint(849, 118, 640, 119.14905F, 54.422348F),
            MapSinglePoint(845, 124, 648, 131.03406F, 57.888565F),
            MapSinglePoint(842, 125, 652, 139.14163F, 60.207817F),
        ),
        listOf(
            MapSinglePoint(810.5, 106, 604.5, -32, 32)
        ),
        listOf(
            MapSinglePoint(827, 90, 630, 90, 90),
            MapSinglePoint(847, 93, 664, 150, 25),
            MapSinglePoint(814, 93, 667, -160, 25),
            MapSinglePoint(840, 93, 595, 22, 25),
            MapSinglePoint(809, 93, 596, -27, 25)
        ),
        false
    ),
    //endregion

    //region MUSICAL_MINECARTS
    MUSICAL_MINECARTS(
        MusicalMinecarts::class,
        "<colour:#a1a4ff>Musical Minecarts".style(),
        "<colour:#a1a4ff>ᴍᴜꜱɪᴄᴀʟ ᴍɪɴᴇᴄᴀʀᴛꜱ".style(),
        ItemStack(Material.MINECART),
        TextColor.fromHexString("#a1a4ff")!!,
        " • ᴅᴀɴᴄᴇ ᴀʀᴏᴜɴᴅ ᴛʜᴇ ᴘʟᴀᴛꜰᴏʀᴍ ᴜɴᴛɪʟ ᴛʜᴇ ᴄʜʀɪѕᴛᴍᴀѕ ᴍᴜѕɪᴄ ѕᴛᴏᴘѕ.\n\n" +
                " • ʀᴜɴ ᴛᴏ ᴛʜᴇ ɴᴇᴀʀᴇѕᴛ ᴍɪɴᴇᴄᴀʀᴛ ᴀɴᴅ ɢᴇᴛ ɪɴѕɪᴅᴇ ʙᴇꜰᴏʀᴇ ᴛʜᴇ ᴛɪᴍᴇʀ ᴇɴᴅѕ.\n\n" +
                " • ɪꜰ ʏᴏᴜ ꜰᴀɪʟ ᴛᴏ ꜰɪɴᴅ ᴀ ᴍɪɴᴇᴄᴀʀᴛ ʙᴇꜰᴏʀᴇ ᴛʜᴇ ᴛɪᴍᴇʀ ᴇɴᴅѕ, ʏᴏᴜ ᴡɪʟʟ ʙᴇ <b>ᴇʟɪᴍɪɴᴀᴛᴇᴅ!\n\n" +
                " • ᴅᴏ <b>ɴᴏᴛ</b> ᴄʟɪᴄᴋ ᴛʜᴇ ᴍɪɴᴇᴄᴀʀᴛѕ ᴜɴᴛɪʟ ᴛʜᴇ ᴍᴜѕɪᴄ ʜᴀѕ ѕᴛᴏᴘᴘᴇᴅ... ᴏʀ ᴇʟѕᴇ ʏᴏᴜ ᴡɪʟʟ ʙᴇ <b>ѕᴛᴜɴɴᴇᴅ!",
        2,
        MapSinglePoint(616.5, 140.0, 800.5, 0F, 90F),
        listOf(
            MapRegion(MapSinglePoint(597, 113, 819), MapSinglePoint(635, 113, 781))
        ),
        listOf(
            MapSinglePoint(581, 122, 805, -111.17755F, 32.429695F),
            MapSinglePoint(591, 122, 799, -94.262634F, 44.97366F),
            MapSinglePoint(595, 122, 792, -67.55487F, 50.719604F),
            MapSinglePoint(603, 122, 785, -45.5412F, 54.28047F),
            MapSinglePoint(617, 122, 786, 0.7520752F, 58.326912F),
            MapSinglePoint(629, 122, 793, 35.310303F, 60.754776F),
            MapSinglePoint(631, 122, 806, 87.59271F, 64.39655F),
            MapSinglePoint(623, 122, 817, 126.925964F, 64.63933F),
            MapSinglePoint(609, 122, 820, 179.69403F, 60.431057F),
            MapSinglePoint(604, 122, 811, -142.9961F, 59.864555F),
            MapSinglePoint(608, 119, 797, -89.015564F, 50.7196F),
            MapSinglePoint(618, 119, 788, -11.158569F, 42.950436F),
            MapSinglePoint(623, 119, 794, 38.534058F, 44.73087F),
            MapSinglePoint(619, 119, 804, 103.84668F, 41.089073F),
            MapSinglePoint(607, 119, 803, 140.50916F, 39.95607F),
            MapSinglePoint(601, 119, 793, -132.65015F, 36.47613F),
            MapSinglePoint(608, 119, 786, -54.22644F, 34.938484F),
            MapSinglePoint(617, 119, 790, 7.7679443F, 48.12988F),
            MapSinglePoint(620, 122, 792, 38.19861F, 61.078487F),
            MapSinglePoint(621, 126, 793, 53.575928F, 67.63352F),
        ),
        listOf(
            MapSinglePoint(645.5, 112, 799.5, 90, 0),
            MapSinglePoint(636.5, 113, 820.5, 135, 0),
            MapSinglePoint(616.5, 112, 829.5, -180, 0),
            MapSinglePoint(596.5, 113, 820.5, -135, 0),
            MapSinglePoint(587.5, 112, 800.5, -90, 0),
            MapSinglePoint(596.5, 113, 780.5, -45, 0),
            MapSinglePoint(616.5, 112, 771.5, 0, 0),
            MapSinglePoint(636.5, 113, 780.5, 45, 0)
        ),
        listOf(
            MapSinglePoint(616.5, 118, 819.5, 180, 35),
            MapSinglePoint(597.5, 118, 800.5, -90, 35),
            MapSinglePoint(616.5, 118, 781.5, 0, 35),
            MapSinglePoint(635.5, 118, 800.5, 90, 35)
        )
    ),
    //endregion

    //region PAINTBALL
    PAINTBALL(
        Paintball::class,
        "<colour:#89bdf5>Paintball".style(),
        "<colour:#89bdf5>ᴘᴀɪɴᴛʙᴀʟʟ".style(),
        ItemStack(Material.HEART_OF_THE_SEA),
        TextColor.fromHexString("#89bdf5")!!,
        " • ѕʜᴏᴏᴛ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀѕ ᴡɪᴛʜ ʏᴏᴜʀ ᴘᴀɪɴᴛʙᴀʟʟ ɢᴜɴ ᴛᴏ ɢᴇᴛ ᴀ ᴘᴏɪɴᴛ.\n\n" +
                " • ᴀᴠᴏɪᴅ ʙᴇɪɴɢ ѕʜᴏᴛ ʙʏ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀѕ ᴛᴏ ѕᴜʀᴠɪᴠᴇ ᴛʜᴇ ʟᴏɴɢᴇѕᴛ.\n\n" +
                " • ᴛᴏ ᴡɪɴ, ʜɪᴛ ᴛʜᴇ ᴍᴏѕᴛ ᴘʟᴀʏᴇʀѕ ᴀɴᴅ ɢᴇᴛ ᴛʜᴇ ᴍᴏѕᴛ ᴘᴏɪɴᴛѕ!",
        2,
        MapSinglePoint(181.5, 140.0, 300.5, -135, 90),
        listOf(
            MapRegion.single(MapSinglePoint(193, 80, 193, 0, 0)),
            MapRegion.single(MapSinglePoint(207, 82, 221, 0, 0)),
            MapRegion.single(MapSinglePoint(206, 80, 251, 0, 0)),
            MapRegion.single(MapSinglePoint(188, 76, 262, 0, 0)),
            MapRegion.single(MapSinglePoint(191, 72, 290, 0, 0)),
            MapRegion.single(MapSinglePoint(209, 71, 295, 0, 0)),
            MapRegion.single(MapSinglePoint(229, 73, 300, 0, 0)),
            MapRegion.single(MapSinglePoint(250, 76, 294, 0, 0)),
            MapRegion.single(MapSinglePoint(265, 78, 304, 0, 0)),
            MapRegion.single(MapSinglePoint(271, 76, 319, 0, 0)),
            MapRegion.single(MapSinglePoint(277, 75, 334, 0, 0)),
            MapRegion.single(MapSinglePoint(285, 75, 350, 0, 0)),
            MapRegion.single(MapSinglePoint(298, 75, 370, 0, 0)),
            MapRegion.single(MapSinglePoint(291, 75, 383, 0, 0)),
            MapRegion.single(MapSinglePoint(281, 75, 398, 0, 0)),
            MapRegion.single(MapSinglePoint(262, 75, 403, 0, 0)),
            MapRegion.single(MapSinglePoint(249, 77, 415, 0, 0)),
            MapRegion.single(MapSinglePoint(233, 79, 422, 0, 0)),
            MapRegion.single(MapSinglePoint(211, 81, 428, 0, 0)),
            MapRegion.single(MapSinglePoint(201, 77, 407, 0, 0)),
            MapRegion.single(MapSinglePoint(194, 76, 394, 0, 0)),
            MapRegion.single(MapSinglePoint(201, 74, 378, 0, 0)),
            MapRegion.single(MapSinglePoint(206, 72, 364, 0, 0)),
            MapRegion.single(MapSinglePoint(208, 71, 347, 0, 0)),
            MapRegion.single(MapSinglePoint(199, 71, 333, 0, 0)),
            MapRegion.single(MapSinglePoint(172, 73, 298, 0, 0)),
            MapRegion.single(MapSinglePoint(143, 79, 279, 0, 0)),
            MapRegion.single(MapSinglePoint(141, 77, 244, 0, 0)),
            MapRegion.single(MapSinglePoint(97, 89, 254, 0, 0)),
            MapRegion.single(MapSinglePoint(114, 86, 215, 0, 0))
        ),
        listOf(
            MapSinglePoint(173, 99, 307, 175.59639F, 24.933584F),
            MapSinglePoint(172, 93, 293, 172.44003F, 21.049002F),
            MapSinglePoint(172, 88, 278, 150.75089F, 10.609256F),
            MapSinglePoint(166, 86, 264, 148.64664F, 3.6494362F),
            MapSinglePoint(156, 86, 252, -178.25386F, 1.3834488F),
            MapSinglePoint(147, 88, 239, -110.35533F, 3.8112924F),
            MapSinglePoint(151, 93, 226, -85.26666F, 10.4474F),
            MapSinglePoint(161, 99, 215, -74.25983F, 10.932967F),
            MapSinglePoint(175, 105, 206, -61.796204F, 15.060287F),
            MapSinglePoint(190, 109, 204, -24.243927F, 27.28051F),
            MapSinglePoint(198, 105, 214, -11.05191F, 30.517662F),
            MapSinglePoint(202, 100, 229, -11.132843F, 25.25729F),
            MapSinglePoint(206, 94, 245, -13.479889F, 19.349497F),
            MapSinglePoint(209, 88, 260, -12.2659F, 17.73092F),
            MapSinglePoint(213, 83, 276, -16.474396F, 14.493787F),
            MapSinglePoint(218, 77, 292, -21.168488F, 12.065951F),
            MapSinglePoint(223, 73, 307, -24.32486F, 4.2968564F)
        ),
        listOf(),
        listOf(),
        false
    ),
    //endregion

    //region PAINT_WARS
    PAINT_WARS(
        PaintWars::
        class,
        "<colour:#75e01d>Paint Wars".style(),
        "<colour:#75e01d>ᴘᴀɪɴᴛ ᴡᴀʀꜱ".style(),
        ItemStack(Material.BRUSH),
        TextColor.fromHexString("#75e01d")!!,
        " • ᴜѕᴇ ʏᴏᴜʀ ᴘᴀɪɴᴛ ɢᴜɴ ᴛᴏ ѕʜᴏᴏᴛ ʙʟᴏᴄᴋѕ ᴀʀᴏᴜɴᴅ ᴛʜᴇ ᴍᴀᴘ ɪɴᴛᴏ ʏᴏᴜʀ ᴄʜᴏѕᴇɴ ʙʟᴏᴄᴋ.\n\n" +
                " • ʏᴏᴜ ᴄᴀɴ ѕʜᴏᴏᴛ ᴏᴠᴇʀ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀ'ѕ ʙʟᴏᴄᴋѕ ᴛᴏ ʀᴇᴅᴜᴄᴇ ᴛʜᴇɪʀ ѕᴄᴏʀᴇ!\n\n" +
                " • ᴛᴏ ᴡɪɴ, ᴄᴏᴠᴇʀ ᴛʜᴇ ᴍᴏѕᴛ ᴀᴍᴏᴜɴᴛ ᴏꜰ ᴛʜᴇ ᴍᴀᴘ ᴡɪᴛʜ ʏᴏᴜʀ ʙʟᴏᴄᴋ/ᴄᴏʟᴏᴜʀ!",
        1,
        MapSinglePoint(622.5, 98, -150.5, 180, 90),
        listOf(
            MapRegion.single(MapSinglePoint(630, 94, -208, 10.461055F, 8.056724F)),
            MapRegion.single(MapSinglePoint(652, 94, -192, 47.526287F, 9.432502F)),
            MapRegion.single(MapSinglePoint(684, 93, -179, 65.33065F, 7.5712233F)),
            MapRegion.single(MapSinglePoint(654, 91, -163, 51.485233F, 12.211981F)),
            MapRegion.single(MapSinglePoint(673, 88, -139, 119.86949F, -7.129863F)),
            MapRegion.single(MapSinglePoint(670, 93, -121, 128.20517F, 6.3042083F)),
            MapRegion.single(MapSinglePoint(630, 94, -108, 174.73938F, 8.246563F)),
            MapRegion.single(MapSinglePoint(606, 104, -83, -163.81433F, 16.339424F)),
            MapRegion.single(MapSinglePoint(573, 102, -96, -132.90025F, 14.397221F)),
            MapRegion.single(MapSinglePoint(548, 116, -127, -128.69142F, 16.744123F)),
            MapRegion.single(MapSinglePoint(535, 94, -147, -102.38962F, 4.8477216F)),
            MapRegion.single(MapSinglePoint(634, 91, -152, -87.49902F, 7.0329227F)),
            MapRegion.single(MapSinglePoint(617, 91, -144, 47.328888F, 5.2525268F)),
            MapRegion.single(MapSinglePoint(592, 100, -256, -36.994843F, -1.9498547F)),
            MapRegion.single(MapSinglePoint(621, 94, -196, -8.994965F, 21.6811F))
        ),
        listOf(
            MapSinglePoint(574, 180, -28, -164.78236F, 45.797764F),
            MapSinglePoint(581, 159, -51, -164.6205F, 45.878693F),
            MapSinglePoint(591, 127, -90, -164.45863F, 46.12148F),
            MapSinglePoint(604, 97, -136, -166.40102F, 40.861107F),
            MapSinglePoint(609, 97, -155, -160.16922F, 30.906864F),
            MapSinglePoint(613, 97, -165, -134.02844F, 24.513489F),
            MapSinglePoint(619, 97, -172, -108.37331F, 19.495903F),
            MapSinglePoint(626, 97, -174, -84.17459F, 17.553612F),
            MapSinglePoint(634, 97, -173, -62.646515F, 17.877327F),
            MapSinglePoint(641, 97, -170, -55.848175F, 17.63454F),
            MapSinglePoint(647, 97, -166, -30.273499F, 16.177822F),
            MapSinglePoint(652, 97, -159, -8.98822F, 15.44947F),
            MapSinglePoint(653, 97, -151, 10.840271F, 14.721119F),
            MapSinglePoint(652, 97, -144, 40.056915F, 11.888644F),
            MapSinglePoint(647, 97, -137, 58.914215F, 12.212356F),
            MapSinglePoint(641, 97, -133, 74.53418F, 12.131428F),
            MapSinglePoint(633, 97, -130, 92.09656F, 12.697923F),
            MapSinglePoint(625, 97, -130, 109.335205F, 13.669057F),
            MapSinglePoint(618, 97, -132, 126.25012F, 13.992768F),
            MapSinglePoint(610, 97, -133, 150.93454F, 14.802047F),
            MapSinglePoint(602, 97, -135, 178.9372F, 13.426273F),
            MapSinglePoint(596, 97, -140, -164.22882F, 13.102562F),
            MapSinglePoint(589, 97, -144, -146.18085F, 12.940706F),
            MapSinglePoint(582, 97, -149, -128.53754F, 11.7267885F),
            MapSinglePoint(577, 97, -155, -108.385315F, 10.836582F),
            MapSinglePoint(571, 97, -160, -93.574646F, 11.241221F),
            MapSinglePoint(565, 97, -166, -77.06439F, 10.755654F),
            MapSinglePoint(560, 98, -173, -68.32367F, 11.7267885F),
            MapSinglePoint(561, 103, -180, -59.906677F, 16.25875F),
            MapSinglePoint(564, 109, -187, -52.29901F, 21.600052F),
            MapSinglePoint(569, 114, -193, -47.92865F, 26.698566F),
            MapSinglePoint(574, 120, -199, -44.20575F, 29.450146F),
            MapSinglePoint(580, 126, -205, -40.15912F, 30.987793F),
            MapSinglePoint(586, 131, -210, -34.412903F, 32.282654F),
            MapSinglePoint(593, 137, -215, -29.556946F, 33.33473F)
        ),
        listOf(
            MapSinglePoint(621, 200, -234, 0, 90)
        ),
        listOf(
            MapSinglePoint(655, 107, -141, 108.178734F, 30.104355F)
        ),
        false
    ),
    //endregion

    //region SLED_RACING, #80ffea
    //endregion

    //region SPLEEF
    SPLEEF(
        Spleef::class,
        "<colour:#ffc642>Spleef".style(),
        "<colour:#ffc642>ꜱᴘʟᴇᴇꜰ".style(),
        ItemStack(Material.DIAMOND_SHOVEL),
        TextColor.fromHexString("#ffc642")!!,
        " • ᴅᴇѕᴛʀᴏʏ ᴛʜᴇ ѕɴᴏᴡ ʙʟᴏᴄᴋѕ ᴜɴᴅᴇʀ ᴏᴛʜᴇʀ ᴘʟᴀʏᴇʀѕ ᴛᴏ ᴇʟɪᴍɪɴᴀᴛᴇ ᴛʜᴇᴍ.\n\n" +
                " • ᴀᴠᴏɪᴅ ꜰᴀʟʟɪɴɢ ɪɴᴛᴏ ᴛʜᴇ ʟᴀᴠᴀ ʙʏ ᴊᴜᴍᴘɪɴɢ ᴏᴠᴇʀ ʜᴏʟᴇѕ ɪɴ ᴛʜᴇ ɢʀᴏᴜɴᴅ.\n\n" +
                " • ᴛᴏ ᴡɪɴ, ʙᴇ ᴛʜᴇ ʟᴀѕᴛ ᴘʟᴀʏᴇʀ ѕᴛᴀɴᴅɪɴɢ ᴏɴ ᴛʜᴇ ᴍᴀᴘ!",
        2,
        MapSinglePoint(616.5, 140.0, 800.5, 0F, 90F),
        listOf(
            MapRegion(MapSinglePoint(597, 113, 819), MapSinglePoint(635, 113, 781))
        ),
        listOf(
            MapSinglePoint(624, 30, 791, 53.888947F, 88.462425F),
            MapSinglePoint(624, 62, 791, 53.888947F, 67.17887F),
            MapSinglePoint(624, 80, 791, 54.860138F, 57.95312F),
            MapSinglePoint(624, 82, 791, 56.397858F, 49.536526F),
            MapSinglePoint(624, 88, 791, 57.207184F, 42.819435F),
            MapSinglePoint(624, 93, 791, 59.31143F, 33.75541F),
            MapSinglePoint(624, 99, 791, 60.120758F, 28.98061F),
            MapSinglePoint(624, 105, 791, 59.635162F, 23.55838F),
            MapSinglePoint(624, 110, 791, 57.854645F, 13.685094F),
            MapSinglePoint(625, 116, 792, 63.681793F, 15.384578F),
            MapSinglePoint(628, 121, 798, 75.25516F, 25.257872F),
            MapSinglePoint(630, 121, 806, 90.87515F, 27.44295F),
            MapSinglePoint(630, 121, 813, 114.183044F, 28.980597F),
            MapSinglePoint(627, 121, 821, 136.11444F, 30.1136F),
            MapSinglePoint(621, 121, 825, 157.23659F, 31.651247F),
            MapSinglePoint(614, 121, 823, 176.49736F, 31.084745F),
            MapSinglePoint(608, 121, 818, -164.80821F, 31.084745F),
            MapSinglePoint(604, 121, 811, -139.72075F, 30.841959F),
            MapSinglePoint(604, 117, 805, -132.518F, 26.14809F),
            MapSinglePoint(610, 114, 802, -144.0911F, 14.57532F),
        ),
        listOf(
            MapSinglePoint(645.5, 112, 799.5, 90, 0),
            MapSinglePoint(636.5, 113, 820.5, 135, 0),
            MapSinglePoint(616.5, 112, 829.5, -180, 0),
            MapSinglePoint(596.5, 113, 820.5, -135, 0),
            MapSinglePoint(587.5, 112, 800.5, -90, 0),
            MapSinglePoint(596.5, 113, 780.5, -45, 0),
            MapSinglePoint(616.5, 112, 771.5, 0, 0),
            MapSinglePoint(636.5, 113, 780.5, 45, 0)
        ),
        listOf(
            MapSinglePoint(635.5, 118, 800.5, 90, 35),
            MapSinglePoint(635.5, 107, 800.5, 90, 35),
            MapSinglePoint(635.5, 100, 800.5, 90, 35),
            MapSinglePoint(635.5, 94, 800.5, 90, 35),
            MapSinglePoint(635.5, 89, 800.5, 90, 35),
            MapSinglePoint(635.5, 83, 800.5, 90, 35),
            MapSinglePoint(635.5, 77, 800.5, 90, 35),
            MapSinglePoint(635.5, 77, 800.5, 90, 35),
        ),
    )
    //endregion
}
// TODO<Map> once final map is ready, add the spectator boxes.