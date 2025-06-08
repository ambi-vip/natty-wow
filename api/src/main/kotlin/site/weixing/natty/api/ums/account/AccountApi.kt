package site.weixing.natty.api.ums.account

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.api.ums.UmsService

@HttpExchange(UmsService.ACCOUNT_AGGREGATE_NAME)
interface AccountApi
