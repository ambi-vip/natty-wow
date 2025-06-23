package site.weixing.natty.server.ums.user

import me.ahoo.wow.exception.throwNotFoundIfEmpty
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.query.snapshot.query
import me.ahoo.wow.query.snapshot.toState
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import site.weixing.natty.domain.ums.user.UserState
import site.weixing.natty.domain.ums.user.UserStateProperties

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

    fun getByUserName(userName: String): Mono<UserState> {
        return singleQuery {
            condition {
                nestedState()
                UserStateProperties.STATUS
            }
        }.query(queryService).toState().throwNotFoundIfEmpty()
    }

}