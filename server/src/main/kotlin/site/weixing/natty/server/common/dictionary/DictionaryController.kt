package site.weixing.natty.server.common.dictionary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import me.ahoo.wow.api.query.Condition
import me.ahoo.wow.api.query.PagedList
import me.ahoo.wow.api.query.Pagination
import me.ahoo.wow.api.query.Sort
import me.ahoo.wow.query.dsl.listQuery
import me.ahoo.wow.query.dsl.pagedQuery
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.query
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import site.weixing.natty.api.common.CommonService
import site.weixing.natty.api.common.dictionary.DictionaryStatus
import site.weixing.natty.domain.common.dictionary.DictionaryItem
import site.weixing.natty.domain.common.dictionary.DictionaryState

/**
 * Desc
 * @author ambi
 */
@Suppress("unused")
@RestController
@RequestMapping("/dictionary")
@Tag(name = CommonService.SERVICE_ALIAS + ".dictionary")
class DictionaryQueryController(
    private val queryService: SnapshotQueryService<DictionaryState>
) {

    @Operation(summary = "激活字典列表", description = "返回字典列表")
    @PostMapping("/active/list")
    fun listActiveDictionaries(@RequestBody request: ListQueryRequest? = null): Flux<DictionaryResponse> {
        val query = listQuery {
            condition {
                nestedState()
                "status" eq DictionaryStatus.ACTIVE
                // 如果有额外的查询条件，可以在这里添加
                request?.condition?.let { condition(it) }
            }
            request?.sort?.let { sort -> sort(sort) }
            request?.limit?.let { limit -> limit(limit) }
        }

        return query.query(queryService).map { snapshot -> snapshot.state.toResponse() }
    }

    @Operation(summary = "激活字典分页", description = "返回字典分页")
    @PostMapping("/active/paged")
    fun pagedActiveDictionaries(@RequestBody request: PagedQueryRequest): Mono<PagedList<DictionaryResponse>> {
        val query = pagedQuery {
            condition {
                nestedState()
                "status" eq DictionaryStatus.ACTIVE
            }
            request.sort?.let { sort -> sort(sort) }
            pagination {
                index(request.pagination.index)
                size(request.pagination.size)
            }
        }

        return query.query(queryService)
            .map { pagedResult ->
                PagedList(
                    total = pagedResult.total,
                    list = pagedResult.list.map { snapshot ->
                        snapshot.state.toResponse()
                    }
                )
            }
    }

    @Operation(summary = "字典项", description = "返回单个字典")
    @GetMapping("/{dictionaryId}/active")
    fun getActiveDictionary(@PathVariable dictionaryId: String): Mono<DictionaryResponse> {
        return singleQuery {
            condition {
                id(dictionaryId)
                "status" eq DictionaryStatus.ACTIVE
            }
        }.query(queryService)
            .map { snapshot -> snapshot.state.toResponse() }
            .switchIfEmpty(Mono.error(NoSuchElementException("Dictionary not found or not active")))
    }

}

// 响应数据类
data class DictionaryResponse(
    val id: String,
    val code: String,
    val name: String?,
    val items: List<DictionaryItemResponse>
)

data class DictionaryItemResponse(
    val itemCode: String,
    val itemName: String,
    val itemValue: String,
    val sortOrder: Int = 0
)

fun DictionaryState.toResponse(): DictionaryResponse {
    return DictionaryResponse(
        id = this.id,
        code = this.code,
        name = this.name,
        items = this.activeItems().map { it.toResponse() }
    )
}

fun DictionaryItem.toResponse(): DictionaryItemResponse {
    return DictionaryItemResponse(
        itemCode = this.itemCode,
        itemName = this.itemName,
        itemValue = this.itemValue,
        sortOrder = this.sortOrder
    )
}

// 请求数据类
data class ListQueryRequest(
    val condition: Condition? = null,
    val sort: List<Sort>? = null,
    val limit: Int? = null
)

data class PagedQueryRequest(
    val condition: Condition? = null,
    val sort: List<Sort>? = null,
    val pagination: Pagination
)