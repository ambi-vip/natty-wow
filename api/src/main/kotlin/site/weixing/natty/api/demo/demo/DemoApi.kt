package site.weixing.natty.api.demo.demo

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.api.demo.DemoService

@HttpExchange(DemoService.DEMO_AGGREGATE_NAME)
interface DemoApi
