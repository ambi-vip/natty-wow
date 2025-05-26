package site.weixing.natty.ums.api.user

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.ums.api.UmsService

@HttpExchange(UmsService.USER_AGGREGATE_NAME)
interface UserApi
