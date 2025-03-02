package site.weixing.natty.api.volunteer

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.DemoService

@HttpExchange(DemoService.VOLUNTEER_PROJECT_AGGREGATE_NAME)
interface VolunteerProjectApi
