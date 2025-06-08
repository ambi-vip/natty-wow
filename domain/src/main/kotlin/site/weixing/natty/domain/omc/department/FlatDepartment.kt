import me.ahoo.wow.models.tree.CopySortIdFlat

data class FlatDepartment(
    override val name: String,
    override val code: String,
    override val sortId: Int
) : CopySortIdFlat<FlatDepartment> {
    override fun withSortId(sortId: Int): FlatDepartment {
        return copy(sortId = sortId)
    }

    fun toLeaf(): LeafDepartment {
        return LeafDepartment(name = name, code = code, sortId = sortId, children = emptyList())
    }
}
