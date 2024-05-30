package github.rikacelery.reverser.reverser

import android.util.Log
import github.rikacelery.reverser.util.convert.toVarIntByteArray
import github.rikacelery.reverser.util.convert.toVarIntLong
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

object Core {
    val TAG = "reverser.Core"
    val MAGIC =
        byteArrayOf(
            0x7c,
            0xc2.toByte(),
            0x32,
            0x00,
            (0xff).toByte(),
        )
    const val BUFFER_SIZE = 1024 * 1024 * 10

    fun readMagic(i: InputStream): ByteArray {
        val buf = ByteArray(5)
        i.read(buf)
        return buf
    }

    fun isMagicMatch(magic: ByteArray): Boolean {
        return magic.contentEquals(MAGIC)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun readMeta(i: InputStream): MetaData {
        var buf = ByteArray(21)
        if (i.read(buf) != buf.size) throw MetaReadException()
        Log.i(TAG, "metadata bytes ${buf.toHexString(HexFormat.UpperCase)}")
        try {
            val blockSize = buf.copyOfRange(0, 10).toVarIntLong()
            val createdDate = buf.copyOfRange(10, 20).toVarIntLong()
            val get = buf.last()
            val noFileName = get == Byte.MAX_VALUE
            var fileName = ""
            if (!noFileName) {
                buf = ByteArray(10)
                i.read(buf)
                val length = buf.toVarIntLong()
                Log.i(TAG, "file name legth $length")

                buf = ByteArray(length.toInt())
                i.read(buf)
                Log.i(TAG, "file name UTF16 bytes ${buf.toHexString(HexFormat.UpperCase)}")
                fileName = String(buf, Charsets.UTF_16)
                buf = ByteArray(522 - length.toInt())
                i.read(buf)
            }

            val meta = MetaData(blockSize, createdDate, noFileName, fileName)
            return meta
        } catch (e: Exception) {
            throw MetaReadException("Failed to read metadata.", e)
        }
    }

    /**
     * @throws java.io.IOException
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun writeMeta(
        o: OutputStream,
        metaData: MetaData,
    ) {
        o.write(MAGIC)
        o.write(metaData.blockSize.toVarIntByteArray())
        o.write(metaData.createdDate.toVarIntByteArray())

        o.write(metaData.noFileName.let { if (it) Byte.MAX_VALUE.toInt() else 0 })
        if (metaData.noFileName.not()) {
            val l = metaData.originalFileName.toByteArray(Charsets.UTF_16)
            o.write(l.size.toLong().toVarIntByteArray())
            o.write(l)

            val paddingLength = (522 - l.size).coerceAtLeast(0)
            o.write(ByteArray(paddingLength))
        }
    }

    /**
     * @throws java.io.IOException 当输入输出流操作失败时
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun reverse(
        i: InputStream,
        o: OutputStream,
        blockSize: Int,
        progressCallback: (block: Int) -> Unit = {},
    ) {
        val buffer = ByteArray(blockSize)
        val iStreamBuffered = BufferedInputStream(i, BUFFER_SIZE)
        val oStreamBuffered = BufferedOutputStream(o, BUFFER_SIZE)

        try {
            var len = 0
            while (iStreamBuffered.read(buffer).also { len = it } != -1) {
                val bb = if (len != buffer.size) buffer.copyOfRange(0, len) else buffer
//                Log.d("Core", "before:" + bb.toHexString())
                bb.reverse()
                oStreamBuffered.write(bb, 0, len)
//                Log.d("Core", "after:" + bb.toHexString())
                progressCallback(len)
            }
            oStreamBuffered.flush()
        } finally {
            iStreamBuffered.close()
            oStreamBuffered.close()
        }
    }
}
