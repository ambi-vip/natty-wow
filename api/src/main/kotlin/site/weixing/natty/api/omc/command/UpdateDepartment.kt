import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.api.naming.Named
import me.ahoo.wow.models.tree.Flat
import me.ahoo.wow.models.tree.command.Update
import me.ahoo.wow.models.tree.command.Updated

@CommandRoute(
    method = CommandRoute.Method.PUT,
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "更新分类名称",
    description = "Id 为租户ID."
)
data class UpdateDepartment(
    override val name: String,
    override val code: String,
) : Update<DepartmentUpdated>, Named {
    override fun toEvent(previous: Flat): DepartmentUpdated {
        return DepartmentUpdated(name = name, code = code, sortId = previous.sortId)
    }
}

data class DepartmentUpdated(
    override val name: String,
    override val code: String,
    override val sortId: Int
) : Updated
