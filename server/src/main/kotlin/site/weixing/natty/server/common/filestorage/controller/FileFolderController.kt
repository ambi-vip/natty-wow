package site.weixing.natty.server.common.filestorage.controller

import me.ahoo.wow.models.tree.Trees.toTree
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.domain.common.filestorage.folder.FileFolderState
import site.weixing.natty.domain.common.filestorage.folder.FlatFileFolder
import site.weixing.natty.domain.common.filestorage.folder.LeafFileFolder

/**
 * Desc
 * @author ambi
 */
@RestController
@RequestMapping("/file/folder")
class FileFolderController(
    private val fileFolderQueryService: SnapshotQueryService<FileFolderState>,
) {


    /**
     * 树形结构
     */
    @GetMapping("/leaf/{id}")
    fun leaf(@PathVariable id: String): Mono<LeafFileFolder> {
        return singleQuery {
            condition {
                id(id)
            }
        }.query(fileFolderQueryService)
            .toState()
            .flatMap { state -> state.children.toTree(LeafFileFolder.ROOT, FlatFileFolder::toLeaf).toMono() }
    }



}