package site.weixing.natty.demo.client

import me.ahoo.coapi.api.CoApi
import site.weixing.natty.demo.NattyService
import site.weixing.natty.demo.api.demo.DemoApi

@CoApi(serviceId = NattyService.SERVICE_NAME)
interface DemoClient : DemoApi
