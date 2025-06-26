package site.weixing.natty.domain.omc.department

import DepartmentCreated
import DepartmentDeleted
import DepartmentMoved
import DepartmentUpdated
import FlatDepartment
import me.ahoo.wow.models.tree.Flat
import me.ahoo.wow.models.tree.aggregate.TreeState

/**
 * DepartmentState
 * @author ambi
 */
class DepartmentState(override val id: String) :
    TreeState<FlatDepartment, DepartmentCreated, DepartmentUpdated, DepartmentDeleted, DepartmentMoved>() {

    override fun Flat.toFlat(): FlatDepartment {
        return FlatDepartment(name, code, sortId)
    }
}
