package gg.flyte.christmas.util

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object Colors {
    val WHITE: TextColor = TextColor.color(0xF3F7F0)
    val RED: TextColor = TextColor.color(0xDA2C38)
    val GOLD: TextColor = TextColor.color(0xFF9800)
    val YELLOW: TextColor = TextColor.color(0xFFEE63)
    val ORANGE: TextColor = TextColor.color(0xF19C79)
    val GREEN: TextColor = TextColor.color(0xA7C957)
    val DARK_GREEN: TextColor = TextColor.color(0x226F54)
    val BLUE: TextColor = TextColor.color(0x384B70)
    val AQUA: TextColor = TextColor.color(0x42D9C8)
    val LIGHT_PURPLE: TextColor = TextColor.color(0xEE57FF)
    val PURPLE: TextColor = TextColor.color(0x6735CE)
    val PINK: TextColor = TextColor.color(0xE89EB8)

    fun tagResolver(): TagResolver = TagResolver.builder().resolvers(
        Placeholder.styling("orange", WHITE),
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
        Placeholder.styling("pink", PINK),
    ).build()
}