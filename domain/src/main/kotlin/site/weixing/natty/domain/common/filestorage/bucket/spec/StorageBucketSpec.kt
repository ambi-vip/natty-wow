package site.weixing.natty.domain.common.filestorage.bucket.spec

import me.ahoo.wow.exception.throwNotFoundIfEmpty
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.domain.common.filestorage.bucket.StorageBucketState

/**
 * StorageBucketSpec
 * @author ambi
 */
@Component
class StorageBucketSpec(
    private val bucketSnapshotQueryService: SnapshotQueryService<StorageBucketState>,
) {

    /**
     * 获取存储桶状态
     */
    private fun getBucketState(bucketId: String): Mono<StorageBucketState> {
        return singleQuery {
            condition {
                id(bucketId)
            }
        }.query(bucketSnapshotQueryService).toState().throwNotFoundIfEmpty("存储桶不存在: $bucketId");
    }


    /**
     * 验证存储桶
     */
    fun validateBucket(bucketId: String, fileSize: Long, contentType: String, uploaderId: String?): Mono<StorageBucketState> {
        return getBucketState(bucketId)
            .flatMap { bucketState ->
                // 验证存储桶状态
                if (!bucketState.exists()) {
                    return@flatMap Mono.error(IllegalArgumentException("存储桶不存在"))
                }

                if (!bucketState.isAvailableForUpload()) {
                    return@flatMap Mono.error(IllegalArgumentException("存储桶状态不允许上传: ${bucketState.status}"))
                }

                // 验证容量
                if (!bucketState.hasCapacity(fileSize)) {
                    return@flatMap Mono.error(IllegalArgumentException("存储桶容量不足"))
                }

                // 验证文件大小
                if (!bucketState.isFileSizeAllowed(fileSize)) {
                    return@flatMap Mono.error(IllegalArgumentException("文件大小超过限制"))
                }

                // 验证文件类型
                if (!bucketState.isFileTypeAllowed(contentType)) {
                    return@flatMap Mono.error(IllegalArgumentException("不支持的文件类型: $contentType"))
                }

                // 验证权限
                if (!bucketState.hasAccess(uploaderId)) {
                    return@flatMap Mono.error(IllegalArgumentException("权限不足"))
                }

                Mono.just(bucketState)
            }
    }

}