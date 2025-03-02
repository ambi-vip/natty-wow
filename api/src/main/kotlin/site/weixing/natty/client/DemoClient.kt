package site.weixing.natty.client

import me.ahoo.coapi.api.CoApi
import site.weixing.natty.DemoService
import site.weixing.natty.api.demo.DemoApi

@CoApi(serviceId = DemoService.SERVICE_NAME)
interface DemoClient : DemoApi
