//package site.weixing.natty.server.dictionary
//
//import me.ahoo.wow.command.CommandGateway
//// import org.springframework.web.bind.annotation.DeleteMapping
//// import org.springframework.web.bind.annotation.GetMapping
//// import org.springframework.web.bind.annotation.PathVariable
//// import org.springframework.web.bind.annotation.PostMapping
//// import org.springframework.web.bind.annotation.PutMapping
//// import org.springframework.web.bind.annotation.RequestBody
//import org.springframework.web.bind.annotation.RequestMapping
//import org.springframework.web.bind.annotation.RestController
//import reactor.core.publisher.Mono
//import site.weixing.natty.api.dictionary.dictionary.ChangeDictionaryStatus
//import site.weixing.natty.api.dictionary.dictionary.CreateDictionary
//import site.weixing.natty.api.dictionary.dictionary.DeleteDictionary
//import site.weixing.natty.api.dictionary.dictionary.UpdateDictionary
//import site.weixing.natty.api.dictionary.item.ChangeDictionaryItemStatus
//import site.weixing.natty.api.dictionary.item.CreateDictionaryItem
//import site.weixing.natty.api.dictionary.item.DeleteDictionaryItem
//import site.weixing.natty.api.dictionary.item.UpdateDictionaryItem
//import site.weixing.natty.server.dictionary.dictionary.DictionaryEntity
//import site.weixing.natty.server.dictionary.dictionary.DictionaryRepository
//import site.weixing.natty.server.dictionary.item.DictionaryItemEntity
//import site.weixing.natty.server.dictionary.item.DictionaryItemRepository
//
///**
// * 字典配置控制器
// *
// * 提供字典和字典项的RESTful API接口。
// *
// * @param commandGateway 命令网关
// * @param dictionaryRepository 字典仓库
// * @param dictionaryItemRepository 字典项仓库
// */
//@RestController
//@RequestMapping("/dictionaries")
//class DictionaryController(
//    private val commandGateway: CommandGateway,
//    private val dictionaryRepository: DictionaryRepository,
//    private val dictionaryItemRepository: DictionaryItemRepository
//) {
//
//    /**
//     * 查询所有字典
//     *
//     * @return 字典列表
//     */
//    // @GetMapping
//    fun getAllDictionaries(): List<DictionaryEntity> {
//        return dictionaryRepository.findAll()
//    }
//
//    /**
//     * 根据字典编码查询字典及其所有字典项
//     *
//     * @param dictionaryCode 字典编码
//     * @return 字典实体，包含字典项列表
//     */
//    // @GetMapping("/{dictionaryCode}")
//    fun getDictionaryByCode(@PathVariable dictionaryCode: String): DictionaryEntity? {
//        val dictionary = dictionaryRepository.findByCode(dictionaryCode)
//        dictionary?.let {
//            val items = dictionaryItemRepository.findAllByDictionaryId(it.id)
//            // TODO: 这里需要一个DTO来组合字典和字典项信息，DictionaryEntity不包含items字段
//            // For now, returning DictionaryEntity and items separately or handle in service layer.
//        }
//        return dictionary
//    }
//
//    /**
//     * 创建字典
//     *
//     * @param command 创建字典命令
//     * @return 命令发送结果
//     */
//    // @PostMapping
//    fun createDictionary(@RequestBody command: CreateDictionary): Mono<Void> {
//        return commandGateway.send(command)
//    }
//
//    /**
//     * 更新字典
//     *
//     * @param dictionaryId 字典ID
//     * @param command 更新字典命令
//     * @return 命令发送结果
//     */
//    // @PutMapping("/{dictionaryId}")
//    fun updateDictionary(
//        @PathVariable dictionaryId: String,
//        @RequestBody command: UpdateDictionary
//    ): Mono<Void> {
//        // 确保命令中的ID与路径变量中的ID一致
//        require(dictionaryId == command.id) { "Path variable ID does not match command ID." }
//        return commandGateway.send(command)
//    }
//
//    /**
//     * 改变字典状态
//     *
//     * @param dictionaryId 字典ID
//     * @param command 改变字典状态命令
//     * @return 命令发送结果
//     */
//    // @PutMapping("/{dictionaryId}/status")
//    fun changeDictionaryStatus(
//        @PathVariable dictionaryId: String,
//        @RequestBody command: ChangeDictionaryStatus
//    ): Mono<Void> {
//        require(dictionaryId == command.id) { "Path variable ID does not match command ID." }
//        return commandGateway.send(command)
//    }
//
//    /**
//     * 删除字典 (逻辑删除)
//     *
//     * @param dictionaryId 字典ID
//     * @return 命令发送结果
//     */
//    // @DeleteMapping("/{dictionaryId}")
//    fun deleteDictionary(@PathVariable dictionaryId: String): Mono<Void> {
//        return commandGateway.send(DeleteDictionary(id = dictionaryId))
//    }
//
//    /**
//     * 获取某个字典下的所有字典项
//     *
//     * @param dictionaryCode 字典编码
//     * @return 字典项列表
//     */
//    // @GetMapping("/{dictionaryCode}/items")
//    fun getDictionaryItemsByDictionaryCode(@PathVariable dictionaryCode: String): List<DictionaryItemEntity> {
//        val dictionary = dictionaryRepository.findByCode(dictionaryCode)
//            ?: throw IllegalArgumentException("字典[${dictionaryCode}]不存在。")
//        return dictionaryItemRepository.findAllByDictionaryId(dictionary.id)
//    }
//
//    /**
//     * 在某个字典下创建字典项
//     *
//     * @param dictionaryCode 字典编码
//     * @param command 创建字典项命令
//     * @return 命令发送结果
//     */
//    // @PostMapping("/{dictionaryCode}/items")
//    fun createDictionaryItem(
//        @PathVariable dictionaryCode: String,
//        @RequestBody command: CreateDictionaryItem
//    ): Mono<Void> {
//        val dictionary = dictionaryRepository.findByCode(dictionaryCode)
//            ?: throw IllegalArgumentException("字典[${dictionaryCode}]不存在。")
//        // 确保命令中的dictionaryId与路径变量中的dictionaryCode对应的字典ID一致
//        require(dictionary.id == command.dictionaryId) { "Command dictionaryId does not match path variable dictionaryCode." }
//        return commandGateway.send(command)
//    }
//
//    /**
//     * 更新某个字典下的字典项
//     *
//     * @param dictionaryCode 字典编码
//     * @param itemCode 字典项编码
//     * @param command 更新字典项命令
//     * @return 命令发送结果
//     */
//    // @PutMapping("/{dictionaryCode}/items/{itemCode}")
//    fun updateDictionaryItem(
//        @PathVariable dictionaryCode: String,
//        @PathVariable itemCode: String,
//        @RequestBody command: UpdateDictionaryItem
//    ): Mono<Void> {
//        val dictionary = dictionaryRepository.findByCode(dictionaryCode)
//            ?: throw IllegalArgumentException("字典[${dictionaryCode}]不存在。")
//        val item = dictionaryItemRepository.findByDictionaryIdAndItemCode(dictionary.id, itemCode)
//            ?: throw IllegalArgumentException("字典项[${itemCode}]在字典[${dictionaryCode}]下不存在。")
//
//        require(item.id == command.id) { "Path variable itemCode does not match command ID." }
//        return commandGateway.send(command)
//    }
//
//    /**
//     * 改变某个字典下的字典项状态
//     *
//     * @param dictionaryCode 字典编码
//     * @param itemCode 字典项编码
//     * @param command 改变字典项状态命令
//     * @return 命令发送结果
//     */
//    // @PutMapping("/{dictionaryCode}/items/{itemCode}/status")
//    fun changeDictionaryItemStatus(
//        @PathVariable dictionaryCode: String,
//        @PathVariable itemCode: String,
//        @RequestBody command: ChangeDictionaryItemStatus
//    ): Mono<Void> {
//        val dictionary = dictionaryRepository.findByCode(dictionaryCode)
//            ?: throw IllegalArgumentException("字典[${dictionaryCode}]不存在。")
//        val item = dictionaryItemRepository.findByDictionaryIdAndItemCode(dictionary.id, itemCode)
//            ?: throw IllegalArgumentException("字典项[${itemCode}]在字典[${dictionaryCode}]下不存在。")
//
//        require(item.id == command.id) { "Path variable itemCode does not match command ID." }
//        return commandGateway.send(command)
//    }
//
//    /**
//     * 删除某个字典下的字典项 (逻辑删除)
//     *
//     * @param dictionaryCode 字典编码
//     * @param itemCode 字典项编码
//     * @return 命令发送结果
//     */
//    // @DeleteMapping("/{dictionaryCode}/items/{itemCode}")
//    fun deleteDictionaryItem(
//        @PathVariable dictionaryCode: String,
//        @PathVariable itemCode: String
//    ): Mono<Void> {
//        val dictionary = dictionaryRepository.findByCode(dictionaryCode)
//            ?: throw IllegalArgumentException("字典[${dictionaryCode}]不存在。")
//        val item = dictionaryItemRepository.findByDictionaryIdAndItemCode(dictionary.id, itemCode)
//            ?: throw IllegalArgumentException("字典项[${itemCode}]在字典[${dictionaryCode}]下不存在。")
//
//        return commandGateway.send(DeleteDictionaryItem(id = item.id))
//    }
//}