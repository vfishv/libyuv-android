package io.github.crow_misia.libyuv

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * I444 (jpeg) YUV Format. 4:4:4 24bpp
 */
class J444Buffer private constructor(
    internal val buffer: ByteBuffer,
    val bufferY: ByteBuffer,
    val bufferU: ByteBuffer,
    val bufferV: ByteBuffer,
    internal val strideY: Int,
    internal val strideU: Int,
    internal val strideV: Int,
    override val width: Int,
    override val height: Int,
    releaseCallback: Runnable?,
) : AbstractBuffer(releaseCallback) {
    override fun asBuffer() = buffer
    override fun asByteArray() = buffer.asByteArray()
    override fun asByteArray(dst: ByteArray) = buffer.asByteArray(dst)
    override fun asBitmap(): Bitmap {
        return AbgrBuffer.allocate(width, height).use {
            convertTo(it)
            it.asBitmap()
        }
    }

    companion object {
        @JvmStatic
        fun getStrideWithCapacity(width: Int, height: Int): IntArray {
            val capacity = width * height
            return intArrayOf(width, capacity, width, capacity, width, capacity)
        }

        @JvmStatic
        fun allocate(width: Int, height: Int): J444Buffer {
            val (strideY, capacityY, strideU, capacityU, strideV, capacityV) = getStrideWithCapacity(width, height)
            val buffer = createByteBuffer(capacityY + capacityU + capacityV)
            val (bufferY, bufferU, bufferV) = buffer.slice(capacityY, capacityU, capacityV)
            return J444Buffer(buffer, bufferY, bufferU, bufferV, strideY, strideU, strideV, width, height) {
                Yuv.freeNativeBuffer(buffer)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun wrap(buffer: ByteBuffer, width: Int, height: Int, releaseCallback: Runnable? = null): J444Buffer {
            val (strideY, capacityY, strideU, capacityU, strideV, capacityV) = getStrideWithCapacity(width, height)
            val (bufferY, bufferU, bufferV) = buffer.slice(capacityY, capacityU, capacityV)
            return J444Buffer(buffer, bufferY, bufferU, bufferV, strideY, strideU, strideV, width, height, releaseCallback)
        }
    }
}
