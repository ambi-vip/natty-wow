package site.weixing.natty.api.common.dictionary

import org.springframework.web.service.annotation.HttpExchange
import site.weixing.natty.api.common.CommonService

@HttpExchange(CommonService.DICT_AGGREGATE_NAME)
interface DictionaryApi
