package site.weixing.natty.domain.omc.department

import CreateDepartment
import DeleteDepartment
import MoveDepartment
import UpdateDepartment
import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.StaticTenantId
import me.ahoo.wow.id.GlobalIdGenerator
import me.ahoo.wow.models.tree.aggregate.Tree

/**
 * Department
 * @author ambi
 */

@AggregateRoot
@StaticTenantId
class Department(state: DepartmentState) :
    Tree<DepartmentState, CreateDepartment, UpdateDepartment, DeleteDepartment, MoveDepartment>(state) {

    override fun generateCode(): String {
        return GlobalIdGenerator.generateAsString()
    }

    override fun maxLevel(): Int {
        return 10
    }
}