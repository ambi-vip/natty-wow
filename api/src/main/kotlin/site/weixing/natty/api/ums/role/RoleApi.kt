package site.weixing.natty.ums.api.role

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.api.ums.UmsService

@HttpExchange(UmsService.ROLE_AGGREGATE_NAME)
interface RoleApi
