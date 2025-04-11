package site.weixing.natty.platform.api.ums

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.platform.NattyPlatformService

@HttpExchange(NattyPlatformService.USER_AGGREGATE_NAME)
interface UserApi
