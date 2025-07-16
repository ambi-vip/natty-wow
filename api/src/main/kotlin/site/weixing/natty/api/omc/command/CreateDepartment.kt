import me.ahoo.wow.api.annotation.AllowCreate
import me.ahoo.wow.api.annotation.CommandRoute
import me.ahoo.wow.models.tree.command.Create
import me.ahoo.wow.models.tree.command.Created

@AllowCreate
@CommandRoute(
    method = CommandRoute.Method.POST,
    appendIdPath = CommandRoute.AppendPath.ALWAYS,
    action = "",
    summary = "添加树节点",
    description = "Id 为租户ID."
)
data class CreateDepartment(override val name: String, override val parentCode: String) : Create<DepartmentCreated> {

    override fun toEvent(code: String, sortId: Int): DepartmentCreated {
        return DepartmentCreated(name = name, code = code, sortId = sortId)
    }
}

data class DepartmentCreated(
    override val name: String,
    override val code: String,
    override val sortId: Int
) : Created
