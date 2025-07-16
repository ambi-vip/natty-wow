package site.weixing.natty.domain.common.filestorage.processing

import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.file.FileMetadata

/**
 * 文件处理器接口
 * 函数式、无状态、可组合的处理器设计
 */
interface FileProcessor {
    
    /**
     * 处理文件内容
     * @param content 输入的文件内容流
     * @param metadata 文件元数据
     * @return 处理后的结果
     */
    fun process(content: Flux<DataBuffer>, metadata: FileMetadata): Mono<ProcessingResult>
    
    /**
     * 判断处理器是否支持当前文件
     */
    fun supports(metadata: FileMetadata): Boolean
}