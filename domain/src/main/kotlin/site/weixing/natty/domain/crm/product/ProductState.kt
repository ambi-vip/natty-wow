package site.weixing.natty.domain.crm.product

import me.ahoo.wow.api.Identifier
import me.ahoo.wow.api.annotation.OnSourcing
import site.weixing.natty.api.crm.product.ProductCreated
import site.weixing.natty.api.crm.product.ProductUpdated
import java.math.BigDecimal
import java.time.LocalDateTime

class ProductState(override val id: String) : Identifier {
    var name: String? = null
        private set
    var category: String? = null
        private set
    var price: BigDecimal? = null
        private set
    var description: String? = null
        private set
    var createTime: LocalDateTime? = null
        private set
    var updateTime: LocalDateTime? = null
        private set

    @OnSourcing
    fun onCreated(event: ProductCreated) {
        name = event.name
        category = event.category
        price = event.price
        description = event.description
        createTime = LocalDateTime.now()
        updateTime = createTime
    }

    @OnSourcing
    fun onUpdated(event: ProductUpdated) {
        name = event.name ?: name
        category = event.category ?: category
        price = event.price ?: price
        description = event.description ?: description
        updateTime = LocalDateTime.now()
    }
} 
