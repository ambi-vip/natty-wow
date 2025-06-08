import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.models.tree.command.Move
import me.ahoo.wow.models.tree.command.Moved

@CommandRoute(
    method = CommandRoute.Method.PUT,
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "sort",
    summary = "移动分类节点",
    description = "只允许同级移动."
)
data class MoveDepartment(override val codes: List<String>) : Move<DepartmentMoved> {

    override fun toEvent(): DepartmentMoved {
        return DepartmentMoved(codes = codes)
    }
}

data class DepartmentMoved(override val codes: List<String>) : Moved
