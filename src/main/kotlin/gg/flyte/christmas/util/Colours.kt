package gg.flyte.christmas.util

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object Colours {
    val AQUA: TextColor = TextColor.fromHexString("#42D9C8")!!
    val WHITE: TextColor = TextColor.fromHexString("#F3F7F0")!!
    val YELLOW: TextColor = TextColor.fromHexString("#FFEE63")!!
    val GREEN: TextColor = TextColor.fromHexString("#A7C957")!!
    val DARK_GREEN: TextColor = TextColor.fromHexString("#226F54")!!
    val BLUE: TextColor = TextColor.fromHexString("#384B70")!!
    val PURPLE: TextColor = TextColor.fromHexString("#6735CE")!!
    val MAGENTA: TextColor = TextColor.fromHexString("#B119C2")!!
    val LIGHT_PURPLE: TextColor = TextColor.fromHexString("#EE57FF")!!
    val RED: TextColor = TextColor.fromHexString("#F23A3A")!!
    val GOLD: TextColor = TextColor.fromHexString("#FF9800")!!
    val ORANGE: TextColor = TextColor.fromHexString("#F19C79")!!
    val PINK: TextColor = TextColor.fromHexString("#E89EB8")!!

    fun tagResolver(): TagResolver = TagResolver.builder().resolvers(
        Placeholder.styling("yellow", YELLOW),
        Placeholder.styling("gold", GOLD),
        Placeholder.styling("white", WHITE),
        Placeholder.styling("red", RED),
        Placeholder.styling("orange", ORANGE),
        Placeholder.styling("green", GREEN),
        Placeholder.styling("dark_green", DARK_GREEN),
        Placeholder.styling("blue", BLUE),
        Placeholder.styling("aqua", AQUA),
        Placeholder.styling("light_purple", LIGHT_PURPLE),
        Placeholder.styling("purple", PURPLE),
        Placeholder.styling("magenta", MAGENTA),
        Placeholder.styling("pink", PINK),
    ).build()
}