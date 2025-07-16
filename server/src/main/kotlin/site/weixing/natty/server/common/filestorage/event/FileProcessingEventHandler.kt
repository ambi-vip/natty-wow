package site.weixing.natty.server.common.filestorage.event
//
//import io.github.oshai.kotlinlogging.KotlinLogging
//import me.ahoo.wow.event.EventHandler
//import org.springframework.stereotype.Component
//import reactor.core.publisher.Mono
//import reactor.kotlin.core.publisher.toMono
//import site.weixing.natty.api.common.filestorage.file.*
//import site.weixing.natty.domain.common.filestorage.processing.ProcessingCoordinator
//import site.weixing.natty.domain.common.filestorage.service.FileStorageService
//import site.weixing.natty.domain.common.filestorage.processing.ProcessingOptions as DomainProcessingOptions
//
///**
// * 文件处理事件处理器
// * 负责监听文件上传事件，并根据需要启动异步处理
// */
//@Component
//class FileProcessingEventHandler(
//    private val processingCoordinator: ProcessingCoordinator,
//    private val fileStorageService: FileStorageService
//) : EventHandler<FileUploaded> {
//
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }
//
//    /**
//     * 处理文件上传完成事件
//     * 如果需要处理，启动异步处理流程
//     */
//     fun handle(event: FileUploaded): Mono<Void> {
//        logger.info { "收到文件上传事件: ${event.fileName} (${event.actualStoragePath})" }
//
//        return if (event.processingRequired) {
//            startAsyncProcessing(event)
//        } else {
//            logger.debug { "文件无需处理: ${event.fileName}" }
//            Mono.empty()
//        }
//    }
//
//    /**
//     * 启动异步处理
//     */
//    private fun startAsyncProcessing(event: FileUploaded): Mono<Void> {
//        logger.info { "启动文件异步处理: ${event.fileName}" }
//
//        return Mono.fromCallable {
//            // 从事件中重建处理选项
//            val processingOptions = buildProcessingOptionsFromEvent(event)
//
//            // 构建文件元数据
//            val fileMetadata = FileMetadata(
//                fileName = event.fileName,
//                contentType = event.contentType,
//                size = event.fileSize,
//                checksum = event.checksum
//            )
//
//            Triple(processingOptions, fileMetadata, event)
//        }
//        .flatMap { (options, metadata, uploadEvent) ->
//            // 从存储服务获取文件内容流
//            fileStorageService.readFile(uploadEvent.actualStoragePath)
//                .flatMap { contentStream ->
//                    // 启动处理流程
//                    processingCoordinator.processFile(contentStream, options, metadata)
//                        .doOnNext { result ->
//                            logger.info {
//                                "文件处理完成: ${uploadEvent.fileName}, 成功: ${result.success}" +
//                                if (result.success) ", 输出路径: ${result.outputPath}" else ", 错误: ${result.errorMessage}"
//                            }
//                        }
//                        .doOnError { error ->
//                            logger.error(error) { "文件处理失败: ${uploadEvent.fileName}" }
//                        }
//                }
//        }
//        .then()
//        .onErrorResume { error ->
//            logger.error(error) { "异步处理启动失败: ${event.fileName}" }
//            Mono.empty()
//        }
//    }
//
//    /**
//     * 从事件中重建处理选项
//     */
//    private fun buildProcessingOptionsFromEvent(event: FileUploaded): DomainProcessingOptions {
//        // 从自定义元数据中解析处理选项
//        val metadata = event.customMetadata
//
//        return DomainProcessingOptions(
//            requireEncryption = metadata["requireEncryption"]?.toBoolean() ?: false,
//            enableCompression = metadata["enableCompression"]?.toBoolean() ?: false,
//            generateThumbnail = metadata["generateThumbnail"]?.toBoolean() ?: false,
//            customProcessors = metadata["customProcessors"]?.split(",") ?: emptyList(),
//            maxProcessingTimeMinutes = metadata["maxProcessingTimeMinutes"]?.toLong() ?: 5L
//        )
//    }
//}
//
///**
// * 文件处理状态事件处理器
// * 负责处理处理过程中的状态变化事件
// */
//@Component
//class FileProcessingStatusEventHandler : EventHandler<FileProcessingStarted> {
//
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }
//
//    override fun handle(event: FileProcessingStarted): Mono<Void> {
//        logger.info {
//            "文件开始处理: ${event.fileName}, 类型: ${event.processingType}, " +
//            "选项: 加密=${event.processingOptions.requireEncryption}, " +
//            "压缩=${event.processingOptions.enableCompression}, " +
//            "缩略图=${event.processingOptions.generateThumbnail}"
//        }
//
//        // 这里可以实现处理状态的持久化、监控等
//        return Mono.empty()
//    }
//}
//
///**
// * 文件处理完成事件处理器
// */
//@Component
//class FileProcessingCompletedEventHandler : EventHandler<FileProcessingCompleted> {
//
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }
//
//    override fun handle(event: FileProcessingCompleted): Mono<Void> {
//        logger.info {
//            "文件处理完成: ${event.fileName}, 类型: ${event.processingType}, " +
//            "耗时: ${event.processingDurationMs}ms, 成功: ${event.processingResult.success}"
//        }
//
//        // 这里可以实现：
//        // 1. 更新文件状态
//        // 2. 发送通知
//        // 3. 清理临时文件
//        // 4. 更新统计信息
//
//        if (event.processingResult.success) {
//            logger.debug { "处理产物: ${event.processingResult.artifacts.size} 个" }
//            event.processingResult.artifacts.forEach { artifact ->
//                logger.debug { "  - ${artifact.type}: ${artifact.path} (${artifact.size} bytes)" }
//            }
//        }
//
//        return Mono.empty()
//    }
//}
//
///**
// * 文件处理失败事件处理器
// */
//@Component
//class FileProcessingFailedEventHandler : EventHandler<FileProcessingFailed> {
//
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }
//
//    override fun handle(event: FileProcessingFailed): Mono<Void> {
//        logger.error {
//            "文件处理失败: ${event.fileName}, 类型: ${event.processingType}, " +
//            "错误: ${event.errorMessage}, 可重试: ${event.retryable}"
//        }
//
//        // 这里可以实现：
//        // 1. 重试逻辑
//        // 2. 错误通知
//        // 3. 失败统计
//        // 4. 告警机制
//
//        if (event.retryable) {
//            logger.info { "标记为可重试失败: ${event.fileName}" }
//            // 可以将任务加入重试队列
//        }
//
//        return Mono.empty()
//    }
//}
//
///**
// * 文件元数据
// */
//data class FileMetadata(
//    val fileName: String,
//    val contentType: String,
//    val size: Long,
//    val checksum: String
//)