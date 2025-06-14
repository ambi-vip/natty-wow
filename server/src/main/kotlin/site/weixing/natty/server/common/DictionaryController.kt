package site.weixing.natty.server.common

import me.ahoo.wow.command.CommandGateway
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 字典配置控制器
 *
 * 提供字典和字典项的RESTful API接口。
 *
 * @param commandGateway 命令网关
 * @param dictionaryRepository 字典仓库
 * @param dictionaryItemRepository 字典项仓库
 */
//@RestController
//@RequestMapping("/dictionaries")
//class DictionaryController(
//    private val commandGateway: CommandGateway,
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
//
//}