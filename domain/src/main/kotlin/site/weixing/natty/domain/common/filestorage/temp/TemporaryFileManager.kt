package site.weixing.natty.domain.common.filestorage.temp

import reactor.core.publisher.Mono
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux

/**
 * 临时文件管理器接口
 * 
 * 负责临时文件的生命周期管理，包括创建、访问、删除和清理功能。
 * 临时文件用于在文件上传过程中避免在内存中传递大文件内容，提高系统性能。
 * 
 * 实现类应该确保：
 * 1. 线程安全 - 支持并发访问
 * 2. 资源清理 - 自动清理过期文件
 * 3. 异常安全 - 妥善处理各种异常情况
 * 4. 性能优化 - 高效的文件I/O操作
 */
interface TemporaryFileManager {
    
    /**
     * 创建临时文件（流式）
     * 
     * @param originalFileName 原始文件名
     * @param fileSize 文件大小（字节）
     * @param contentType 文件内容类型
     * @param dataBufferFlux 文件内容数据流（响应式）
     * @return 临时文件引用的 Mono 包装
     */
    fun createTemporaryFile(
        originalFileName: String,
        fileSize: Long,
        contentType: String,
        dataBufferFlux: Flux<DataBuffer>
    ): Mono<TemporaryFileReference>

    /**
     * 获取临时文件的 DataBuffer 流（流式读取）
     * 
     * @param referenceId 临时文件引用ID
     * @return 文件内容的 Flux<DataBuffer>
     */
    fun getFileStreamAsFlux(referenceId: String): Flux<DataBuffer>
    
    /**
     * 删除临时文件
     * 
     * 根据引用ID删除对应的临时文件和相关元数据。
     * 此操作是幂等的，重复删除不会产生错误。
     * 
     * @param referenceId 临时文件引用ID
     * @return 删除是否成功的 Mono 包装（true=成功删除，false=文件不存在）
     */
    fun deleteTemporaryFile(referenceId: String): Mono<Boolean>
    
    /**
     * 清理过期的临时文件
     * 
     * 扫描所有临时文件，删除已过期的文件和相关元数据。
     * 此方法通常由定时任务调用，确保系统不会积累过多临时文件。
     * 
     * @return 清理的文件数量的 Mono 包装
     */
    fun cleanupExpiredFiles(): Mono<Long>
    
    /**
     * 获取临时文件引用
     * 
     * 根据引用ID获取临时文件的完整引用信息，不读取文件内容。
     * 
     * @param referenceId 临时文件引用ID
     * @return 临时文件引用的 Mono 包装
     * @throws TemporaryFileNotFoundException 文件不存在时抛出
     */
    fun getTemporaryFileReference(referenceId: String): Mono<TemporaryFileReference>
    
    /**
     * 检查临时文件是否存在且有效
     * 
     * @param referenceId 临时文件引用ID
     * @return 文件是否存在且未过期的 Mono 包装
     */
    fun isTemporaryFileValid(referenceId: String): Mono<Boolean>
} 