package com.wayfarer.boosterupgrade.cards

import org.apache.tomcat.util.codec.binary.Base64
import java.io.ByteArrayOutputStream
import java.lang.Long.numberOfLeadingZeros
import kotlin.experimental.and

private fun encode(number: Long): ByteArray {
    var n = number
    val numRelevantBits = 64 - numberOfLeadingZeros(n)
    var numBytes = (numRelevantBits + 6) / 7
    if (numBytes == 0) numBytes = 1
    val output = ByteArray(numBytes)
    for (i in numBytes - 1 downTo 0) {
        var curByte = (n and 0x7F).toInt()
        if (i != numBytes - 1) curByte = curByte or 0x80
        output[i] = curByte.toByte()
        n = n ushr 7
    }
    return output
}

private fun decodeIds(b: ByteArray): List<Int> {
    val ids = ArrayList<Int>()
    var prev: Long = 0
    var n: Long = 0
    for (i in b.indices) {
        val curByte = b[i] and 0xFF.toByte()
        n = n shl 7 or (curByte and 0x7F).toLong()
        if (curByte and (0x80.toByte()) == 0.toByte()) {
            ids.add((n + prev).toInt())
            prev += n
            n = 0
        }
    }
    return ids
}

private fun compressIds(ints: List<Int>): ByteArray {
    require(ints.all { it > 0 })

    val baos = ByteArrayOutputStream()
    ints.sorted().let { sorted ->
        var lastInt = 0
        sorted.forEach {
            baos.write(encode(it.toLong() - lastInt))
            lastInt = it
        }
    }
    return baos.toByteArray()
}

fun compressDecklist(cards: List<MagicCard>): String {
    val baos = ByteArrayOutputStream()
    baos.use { bw ->
        bw.writeBytes(compressIds(cards.map { c ->
            c.id
        }))
    }

    return Base64.encodeBase64String(baos.toByteArray())
}

fun decompressDecklistIds(deckString: String): List<Int> {
    val bytes = Base64.decodeBase64(deckString)
    return decodeIds(bytes)
}
