package site.weixing.natty.api.demo.client

import me.ahoo.coapi.api.CoApi
import site.weixing.natty.api.NattyService
import site.weixing.natty.api.demo.demo.DemoApi

@CoApi(serviceId = NattyService.SERVICE_NAME)
interface DemoClient : DemoApi
