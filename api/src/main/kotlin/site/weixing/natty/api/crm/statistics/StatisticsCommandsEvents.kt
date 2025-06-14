package site.weixing.natty.api.crm.statistics

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.annotation.CreateAggregate
import me.ahoo.wow.api.command.DeleteAggregate
import java.math.BigDecimal

@CreateAggregate
@CommandRoute(
    method = CommandRoute.Method.POST,
    action = "",
    summary = "创建统计数据"
)
data class CreateStatistic(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val periodType: String, // e.g., "DAILY", "MONTHLY", "YEARLY"

    @field:NotBlank
    val periodValue: String, // e.g., "2023-10-26", "2023-10", "2023"

    @field:NotNull
    val value: BigDecimal,

    val remark: String? = null
)

@CommandRoute(
    method = CommandRoute.Method.PUT,
    action = "",
    summary = "更新统计数据"
)
data class UpdateStatistic(
    @field:NotBlank
    val id: String,

    val name: String? = null,

    val periodType: String? = null,

    val periodValue: String? = null,

    val value: BigDecimal? = null,

    val remark: String? = null
)

@CommandRoute(
    summary = "删除统计数据"
)
data class DeleteStatistic(@CommandRoute.PathVariable val id: String) : DeleteAggregate

data class StatisticCreated(
    val name: String,
    val periodType: String,
    val periodValue: String,
    val value: BigDecimal,
    val remark: String?
)

data class StatisticUpdated(
    val name: String?,
    val periodType: String?,
    val periodValue: String?,
    val value: BigDecimal?,
    val remark: String?
)

data class StatisticDeleted(val id: String)