package github.rikacelery.reverser.util.convert

import java.nio.ByteBuffer
import java.util.Locale
import kotlin.experimental.or

fun Long.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
    buffer.putLong(this)
    return buffer.array()
}

fun Int.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
    buffer.putInt(this)
    String.format(Locale.US, "%d", 1)
    return buffer.array()
}

fun Long.toVarIntByteArray(): ByteArray {
    // GO impl
    var x = this.toULong()
    val buffer = ByteArray(10)
    var ux = this.toULong() shl 1
    x = ux
    // println(ux)
    // println(0x80U)
    if (x < 0U) {
        ux = ux.inv()
    }
    var i = 0
    while (x >= 0x80U) {
        // println("loop %d b:%02x ".format(i,(x).toByte()))
        buffer[i] = (x).toByte() or 0x80.toByte()
        // println("loop %d b|0x80:%02x ".format(i,(x).toByte() or 0x80.toByte()))
        x = x shr 7
        i++
    }
    buffer[i] = (x).toByte()
    return buffer
}

fun ByteArray.toVarIntLong(): Long {
    var x: ULong = 0U
    var s: Int = 0
    this.forEachIndexed { i, b ->
        if (i == 10) {
            // Catch byte reads past MaxVarintLen64.
            // See issue https://golang.org/issues/41185
            return 0 // overflow
        }
        if (b.toUByte() < 0x80U) {
            if (i == 10 - 1 && b > 1) {
                return 0 // overflow
            }

//            println("%02x %02x %02x\t".format(x.toLong(), ((b.toLong())) shl s,x.toLong() or (b.toLong()) shl s))
//            println("${x}\t${b.toULong()}\t${b.toUByte()}\t${(b.toULong()) shl s}\t${x.toLong()+(((b.toULong())) shl s).toLong()}")
            val ux = (x.toLong() or (b.toULong() shl s).toLong()).toULong()
            var x = (ux shr 1).toLong()
//            println(x)
            if (ux and 1u != 0UL) {
                x = x.inv()
            }
            return x
        }
//        println("x\tb\tul(b)&7f\t<<${s}\tx|")
//        print(x)
//        print('\t')
//        print(b)
//        print('\t')
//        print(b.toULong() and 0x7fU)
//        print("       ")
//        print('\t')
//        print((b.toULong() and 0x7fU) shl s)
//        print('\t')
//        print((x or (b.toULong() and 0x7fU) shl s.toInt()))
//        println()
        x = x or (b.toULong() and 0x7fU) shl s
        s += 7
    }
    return 0
}

fun ByteArray.hex(): String {
    return map { b ->
        ("%02x".format(b))
    }.joinToString(" ")
}
