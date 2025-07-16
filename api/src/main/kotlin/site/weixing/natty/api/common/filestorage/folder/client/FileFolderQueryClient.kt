package site.weixing.natty.api.common.filestorage.folder.client

import me.ahoo.coapi.api.CoApi
import me.ahoo.wow.cache.source.QueryApiCacheSource
import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.api.common.CommonService
import site.weixing.natty.api.common.filestorage.folder.FileFolderData

/**
 * FileFolderQueryClient
 * @author ambi
 */
@CoApi(baseUrl = "http://localhost:8080")
@HttpExchange(CommonService.FILE_FOLDER_AGGREGATE_NAME)
interface FileFolderQueryClient: QueryApiCacheSource<FileFolderData>