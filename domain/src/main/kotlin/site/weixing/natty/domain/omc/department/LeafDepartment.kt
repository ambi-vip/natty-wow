import me.ahoo.wow.models.tree.Leaf
import me.ahoo.wow.models.tree.ROOT_CODE

data class LeafDepartment(
    override val name: String,
    override val code: String,
    override val sortId: Int,
    override val children: List<LeafDepartment>
) : Leaf<LeafDepartment> {

    override fun withChildren(children: List<LeafDepartment>): LeafDepartment {
        return copy(children = children)
    }

    companion object {
        val ROOT = LeafDepartment(name = ROOT_CODE, code = ROOT_CODE, sortId = 0, children = emptyList())
    }
}
