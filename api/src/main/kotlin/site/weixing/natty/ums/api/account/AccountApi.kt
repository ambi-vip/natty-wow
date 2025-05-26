package site.weixing.natty.ums.api.account

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.ums.api.UmsService

@HttpExchange(UmsService.ACCOUNT_AGGREGATE_NAME)
interface AccountApi
