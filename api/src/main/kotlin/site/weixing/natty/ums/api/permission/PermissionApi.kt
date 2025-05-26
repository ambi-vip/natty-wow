package site.weixing.natty.ums.api.permission

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.ums.api.UmsService

@HttpExchange(UmsService.PERMISSION_AGGREGATE_NAME)
interface PermissionApi
