package site.weixing.natty.api.crm

import me.ahoo.wow.api.annotation.BoundedContext
import site.weixing.natty.api.crm.business.CreateBusiness
import site.weixing.natty.api.crm.clue.CreateClue
import site.weixing.natty.api.crm.contact.CreateContact
import site.weixing.natty.api.crm.contract.CreateContract
import site.weixing.natty.api.crm.customer.CreateCustomer
import site.weixing.natty.api.crm.followup.CreateFollowup
import site.weixing.natty.api.crm.operatelog.CreateOperateLog
import site.weixing.natty.api.crm.product.CreateProduct
import site.weixing.natty.api.crm.receivable.CreateReceivable
import site.weixing.natty.api.crm.statistics.CreateStatistic

@BoundedContext(
    name = CrmService.SERVICE_NAME,
    alias = CrmService.SERVICE_ALIAS,
    aggregates = [
        BoundedContext.Aggregate(CrmService.CLUE_AGGREGATE_NAME, packageScopes = [CreateClue::class]),
        BoundedContext.Aggregate(CrmService.CUSTOMER_AGGREGATE_NAME, packageScopes = [CreateCustomer::class]),
        BoundedContext.Aggregate(CrmService.CONTACT_AGGREGATE_NAME, packageScopes = [CreateContact::class]),
        BoundedContext.Aggregate(CrmService.BUSINESS_AGGREGATE_NAME, packageScopes = [CreateBusiness::class]),
        BoundedContext.Aggregate(CrmService.CONTRACT_AGGREGATE_NAME, packageScopes = [CreateContract::class]),
        BoundedContext.Aggregate(CrmService.RECEIVABLE_AGGREGATE_NAME, packageScopes = [CreateReceivable::class]),
        BoundedContext.Aggregate(CrmService.PRODUCT_AGGREGATE_NAME, packageScopes = [CreateProduct::class]),
        BoundedContext.Aggregate(CrmService.FOLLOWUP_AGGREGATE_NAME, packageScopes = [CreateFollowup::class]),
        BoundedContext.Aggregate(CrmService.OPERATELOG_AGGREGATE_NAME, packageScopes = [CreateOperateLog::class]),
        BoundedContext.Aggregate(CrmService.STATISTICS_AGGREGATE_NAME, packageScopes = [CreateStatistic::class]),
    ],
)
object CrmService {
    const val SERVICE_NAME = "crm-service"
    const val SERVICE_ALIAS = "crm"
    const val CLUE_AGGREGATE_NAME = "clue"
    const val CUSTOMER_AGGREGATE_NAME = "customer"
    const val CONTACT_AGGREGATE_NAME = "contact"
    const val BUSINESS_AGGREGATE_NAME = "business"
    const val CONTRACT_AGGREGATE_NAME = "contract"
    const val RECEIVABLE_AGGREGATE_NAME = "receivable"
    const val PRODUCT_AGGREGATE_NAME = "product"
    const val FOLLOWUP_AGGREGATE_NAME = "followup"
    const val OPERATELOG_AGGREGATE_NAME = "operatelog"
    const val STATISTICS_AGGREGATE_NAME = "statistics"
} 
