package site.weixing.natty.api.demo

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.DemoService

@HttpExchange(DemoService.DEMO_AGGREGATE_NAME)
interface DemoApi
