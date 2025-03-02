package site.weixing.natty.client.volunteer

import me.ahoo.coapi.api.CoApi
import site.weixing.natty.DemoService
import site.weixing.natty.api.volunteer.VolunteerProjectApi

@CoApi(serviceId = DemoService.SERVICE_NAME)
interface ProjectClient : VolunteerProjectApi
