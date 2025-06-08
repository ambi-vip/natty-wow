import jakarta.validation.constraints.NotBlank
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.models.tree.Flat
import me.ahoo.wow.models.tree.command.Delete
import me.ahoo.wow.models.tree.command.Deleted

@CommandRoute(
    method = CommandRoute.Method.DELETE,
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "{code}",
    summary = "删除树节点",
    description = "Id 为租户ID."
)
data class DeleteDepartment(
    @field:NotBlank
    @CommandRoute.PathVariable
    override val code: String
) : Delete<DepartmentDeleted> {
    override fun toEvent(previous: Flat): DepartmentDeleted {
        return DepartmentDeleted(code = code)
    }
}

data class DepartmentDeleted(override val code: String) : Deleted
