package site.weixing.natty.server.platform.ums.user

import me.ahoo.cosid.mongo.Documents
import me.ahoo.wow.command.factory.CommandBuilder
import me.ahoo.wow.command.factory.CommandBuilderRewriter
import me.ahoo.wow.query.dsl.condition
import me.ahoo.wow.query.dsl.projection
import me.ahoo.wow.query.dsl.singleQuery
import me.ahoo.wow.query.snapshot.SnapshotQueryService
import me.ahoo.wow.query.snapshot.dynamicQuery
import me.ahoo.wow.query.snapshot.nestedState
import me.ahoo.wow.serialization.MessageRecords
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import site.weixing.natty.domain.platform.ums.user.UserState
import site.weixing.natty.domain.platform.ums.user.UserStateProperties.PHONE
import site.weixing.natty.platform.api.ums.ResetPwd

/**
 * 找回密码(`ResetPwd`)命令重写器。
 *
 * 该命令需要根据命令体中的手机号码查询用户聚合根ID，以便满足命令消息聚合根ID必填的要求。
 *
 */
@Service
class ResetPwdCommandBuilderRewriter(private val queryService: SnapshotQueryService<UserState>) :
    CommandBuilderRewriter {
    override val supportedCommandType: Class<ResetPwd>
        get() = ResetPwd::class.java

    override fun rewrite(commandBuilder: CommandBuilder): Mono<CommandBuilder> {
        return singleQuery {
            projection { include(Documents.ID_FIELD) }
            condition {
                nestedState()
//                PHONE_VERIFIED eq true
                PHONE eq commandBuilder.bodyAs<ResetPwd>().phone
            }
        }.dynamicQuery(queryService)
            .switchIfEmpty {
                IllegalArgumentException("手机号码尚未绑定。").toMono()
            }.map {
                commandBuilder.aggregateId(it.getValue(MessageRecords.AGGREGATE_ID))
            }
    }
}