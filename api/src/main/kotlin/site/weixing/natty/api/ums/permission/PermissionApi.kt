package site.weixing.natty.api.ums.permission

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.api.ums.UmsService

@HttpExchange(UmsService.PERMISSION_AGGREGATE_NAME)
interface PermissionApi
