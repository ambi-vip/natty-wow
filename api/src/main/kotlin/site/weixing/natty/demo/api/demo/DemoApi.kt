package site.weixing.natty.demo.api.demo

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.demo.NattyService

@HttpExchange(NattyService.DEMO_AGGREGATE_NAME)
interface DemoApi
