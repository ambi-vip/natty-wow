package site.weixing.natty.domain.crm.product

import me.ahoo.wow.api.annotation.AggregateRoot
import me.ahoo.wow.api.annotation.OnCommand
import me.ahoo.wow.api.annotation.OnError
import reactor.core.publisher.Mono
import site.weixing.natty.api.crm.product.CreateProduct
import site.weixing.natty.api.crm.product.DeleteProduct
import site.weixing.natty.api.crm.product.ProductCreated
import site.weixing.natty.api.crm.product.ProductDeleted
import site.weixing.natty.api.crm.product.ProductUpdated
import site.weixing.natty.api.crm.product.UpdateProduct

@AggregateRoot
class Product(private val state: ProductState) {
    @OnCommand
    fun onCreate(command: CreateProduct): ProductCreated {
        // 业务规则校验
        require(command.name.isNotBlank()) { "产品名称不能为空" }
        require(command.category.isNotBlank()) { "产品分类不能为空" }
        require(command.price.signum() > 0) { "产品价格必须大于0" }

        // 返回事件
        return ProductCreated(
            name = command.name,
            category = command.category,
            price = command.price,
            description = command.description
        )
    }

    @OnCommand
    fun onUpdate(command: UpdateProduct): Mono<ProductUpdated> {
        // 业务规则校验
        require(state.name != null) { "产品不存在" }
        command.price?.let { require(it.signum() > 0) { "产品价格必须大于0" } }
        // TODO: 更详细的业务规则校验，例如名称和分类的唯一性等

        // 返回事件
        return Mono.just(
            ProductUpdated(
                name = command.name,
                category = command.category,
                price = command.price,
                description = command.description
            )
        )
    }

    @OnCommand
    fun onDelete(command: DeleteProduct): Mono<ProductDeleted> {
        // 业务规则校验
        require(state.name != null) { "产品不存在" }
        // TODO: 考虑关联的业务数据，例如是否允许删除有合同或商机引用的产品
        // 例如: require(state.status != ProductStatus.ACTIVE) { "已启用的产品不能删除" }

        // 返回事件
        return Mono.just(ProductDeleted(command.id))
    }

    @OnError
    fun onError(command: CreateProduct, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing CreateProduct command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: UpdateProduct, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing UpdateProduct command: ${error.message}")
        return Mono.empty()
    }

    @OnError
    fun onError(command: DeleteProduct, error: Throwable): Mono<Void> {
        // TODO: 更详细的错误处理逻辑，例如记录日志到特定系统，或者向上抛出特定业务异常
        println("Error processing DeleteProduct command: ${error.message}")
        return Mono.empty()
    }
}
