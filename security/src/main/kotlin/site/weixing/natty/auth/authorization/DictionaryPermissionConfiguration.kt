// package site.weixing.natty.auth.authorization
//
// import me.ahoo.cosec.api.permission.AppPermission
// import me.ahoo.cosec.permission.AppPermissionData
// import org.slf4j.LoggerFactory
// import org.springframework.context.annotation.Configuration
// import org.springframework.context.annotation.Bean
//
// /**
// * 字典权限配置
// *
// * 定义并注册字典相关的权限点。
// */
// @Configuration
// class DictionaryPermissionConfiguration {
//
//    companion object {
//        private val log = LoggerFactory.getLogger(DictionaryPermissionConfiguration::class.java)
//    }
//
//    /**
//     * 定义并设置字典相关权限
//     *
//     * @param appRolePermissionRepository 应用角色权限仓库
//     * @return Unit
//     */
//    @Bean
//    fun dictionaryAppPermissions(appRolePermissionRepository: InMemoryAppRolePermissionRepository): Unit {
//        log.info("Initializing dictionary app permissions...")
//
//        val dictionaryCreatePermission = AppPermissionData(
//            id = "dictionary:create",
//            name = "创建字典",
//            description = "允许创建新的字典",
//            appId = "natty-wow" // 假设应用ID为natty-wow
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryCreatePermission)
//
//        val dictionaryReadPermission = AppPermissionData(
//            id = "dictionary:read",
//            name = "读取字典",
//            description = "允许读取字典信息",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryReadPermission)
//
//        val dictionaryUpdatePermission = AppPermissionData(
//            id = "dictionary:update",
//            name = "更新字典",
//            description = "允许更新字典信息",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryUpdatePermission)
//
//        val dictionaryDeletePermission = AppPermissionData(
//            id = "dictionary:delete",
//            name = "删除字典",
//            description = "允许删除字典信息",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryDeletePermission)
//
//        val dictionaryItemCreatePermission = AppPermissionData(
//            id = "dictionary_item:create",
//            name = "创建字典项",
//            description = "允许创建新的字典项",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryItemCreatePermission)
//
//        val dictionaryItemReadPermission = AppPermissionData(
//            id = "dictionary_item:read",
//            name = "读取字典项",
//            description = "允许读取字典项信息",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryItemReadPermission)
//
//        val dictionaryItemUpdatePermission = AppPermissionData(
//            id = "dictionary_item:update",
//            name = "更新字典项",
//            description = "允许更新字典项信息",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryItemUpdatePermission)
//
//        val dictionaryItemDeletePermission = AppPermissionData(
//            id = "dictionary_item:delete",
//            name = "删除字典项",
//            description = "允许删除字典项信息",
//            appId = "natty-wow"
//        )
//        appRolePermissionRepository.setAppPermission(dictionaryItemDeletePermission)
//
//        log.info("Dictionary app permissions initialized.")
//    }
// }
