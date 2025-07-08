package site.weixing.natty.api.common

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.api.common.dictionary.CreateDictionary
import site.weixing.natty.api.common.filestorage.folder.CreateFileFolder
import site.weixing.natty.api.common.filestorage.file.UploadFile
import site.weixing.natty.api.common.filestorage.storage.CreateStorageConfig
import site.weixing.natty.api.common.filestorage.cdn.CreateCdnConfig

@BoundedContext(
    name = CommonService.SERVICE_NAME,
    alias = CommonService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(CommonService.DICT_AGGREGATE_NAME, packageScopes = [CreateDictionary::class]),
        BoundedContext.Aggregate(CommonService.FILE_FOLDER_AGGREGATE_NAME, packageScopes = [CreateFileFolder::class]),
        BoundedContext.Aggregate(CommonService.FILE_AGGREGATE_NAME, packageScopes = [UploadFile::class]),
        BoundedContext.Aggregate(CommonService.STORAGE_CONFIG_AGGREGATE_NAME, packageScopes = [CreateStorageConfig::class]),
        BoundedContext.Aggregate(CommonService.CDN_CONFIG_AGGREGATE_NAME, packageScopes = [CreateCdnConfig::class]),
    ],
)
object CommonService {
    const val SERVICE_NAME = "common-service"
    const val SERVICE_ALIAS = "common"
    const val DICT_AGGREGATE_NAME = "dict"
    const val FILE_FOLDER_AGGREGATE_NAME = "file-folder"
    const val FILE_AGGREGATE_NAME = "file"
    const val STORAGE_CONFIG_AGGREGATE_NAME = "storage-config"
    const val CDN_CONFIG_AGGREGATE_NAME = "cdn-config"
}
