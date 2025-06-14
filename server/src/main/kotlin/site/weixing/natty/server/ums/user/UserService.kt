package site.weixing.natty.server.ums.user

import me.ahoo.wow.exception.throwNotFoundIfEmpty
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.domain.ums.user.UserState

/**
 * UserService
 * @author ambi
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class UserService(
    private val queryService: SnapshotQueryService<UserState>
) {

    fun getById(id: String): Mono<UserState> {
        return singleQuery {
            condition {
                id(id)
            }
        }.query(queryService).toState().throwNotFoundIfEmpty()
    }

}