package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import org.springframework.stereotype.Component

@Component
class DictionaryPrepares(
    private val prepareKeyFactory: PrepareKeyFactory
)  : PrepareKey<String> by prepareKeyFactory.create(
    "com_dict_code",
    String::class.java
)
