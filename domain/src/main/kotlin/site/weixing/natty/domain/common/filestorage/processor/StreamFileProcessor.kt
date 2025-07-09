package site.weixing.natty.domain.common.filestorage.processor

import java.io.InputStream
import java.io.OutputStream

/**
 * 流式文件处理器
 * 支持大文件分块处理，避免内存溢出
 */
class StreamFileProcessor {
    /**
     * 将输入流内容分块处理并写入输出流
     * @param input 输入流
     * @param output 输出流
     * @param bufferSize 缓冲区大小，默认8MB
     * @param onChunkProcessed 每块处理后的回调
     * @return 总字节数
     */
    fun process(
        input: InputStream,
        output: OutputStream,
        bufferSize: Int = 8 * 1024 * 1024,
        onChunkProcessed: ((chunkSize: Int, totalBytes: Long) -> Unit)? = null
    ): Long {
        val buffer = ByteArray(bufferSize)
        var totalBytes = 0L
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalBytes += bytesRead
            onChunkProcessed?.invoke(bytesRead, totalBytes)
        }
        output.flush()
        return totalBytes
    }
} 