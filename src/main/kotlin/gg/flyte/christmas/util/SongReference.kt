package gg.flyte.christmas.util

import com.xxmicloxx.NoteBlockAPI.model.Song
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder
import gg.flyte.christmas.ChristmasEventPlugin
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.Throws

/**
 * Converts a file path to a NoteblockAPI [Song] object.
 *
 * @param fileName The name of the file to parse.
 * @return The mapped [Song] object.
 */
private fun parse(fileName: String): Song {
    @Throws(IOException::class)
    fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file, false).use { outputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
    }

    val file = File(fileName)
    copyInputStreamToFile(ChristmasEventPlugin.instance.getResource("music/$fileName")!!, file)

    return NBSDecoder.parse(file)
}

/**
 * An enum class containing all the songs that can be played. Stored in /resources/music.
 */
enum class SongReference(val title: String, val song: Song) {
    ALL_I_WANT_FOR_CHRISTMAS_IS_YOU("All I Want For Christmas Is You", parse("ALL_I_WANT_FOR_CHRISTMAS_IS_YOU.nbs")),
    CAROL_OF_THE_BELLS("Carol Of The Bells", parse("CAROL_OF_THE_BELLS.nbs")),
    FROSTY_THE_SNOWMAN("Frosty The Snowman", parse("FROSTY_THE_SNOWMAN.nbs")),
    ITS_BEGINNING_TO_LOOK_A_LOT_LIKE_CHRISTMAS("It's Beginning To Look A Lot Like Christmas", parse("ITS_BEGINNING_TO_LOOK_A_LOT_LIKE_CHRISTMAS.nbs")),
    JINGLE_BELLS("Jingle Bells", parse("JINGLE_BELLS.nbs")),
    JOY_TO_THE_WORLD("Joy To The World", parse("JOY_TO_THE_WORLD.nbs")),
    LAST_CHRISTMAS("Last Christmas", parse("LAST_CHRISTMAS.nbs")),
    O_COME_ALL_YE_FAITHFUL("O' Come All Ye Faithful", parse("O_COME_ALL_YE_FAITHFUL.nbs")),
    RUDOLPH_THE_RED_NOSED_REINDEER("Rudolph The Red Nosed Reindeer", parse("RUDOLPH_THE_RED_NOSED_REINDEER.nbs")),
    SILENT_NIGHT("Silent Night", parse("SILENT_NIGHT.nbs")),
    SLEIGH_RIDE("Sleigh Ride", parse("SLEIGH_RIDE.nbs")),
    TWELVE_DAYS_OF_CHRISTMAS("Twelve Days Of Christmas", parse("TWELVE_DAYS_OF_CHRISTMAS.nbs")),
    WE_WISH_YOU_A_MERRY_CHRISTMAS("We Wish You A Merry Christmas", parse("WE_WISH_YOU_A_MERRY_CHRISTMAS.nbs")),
}
