package site.weixing.natty.domain.common.dictionary

import jakarta.annotation.PostConstruct
import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import org.springframework.stereotype.Component

@Component
class DictionaryPrepares(
    private val prepareKeyFactory: PrepareKeyFactory
) {

    @PostConstruct
    fun init() {
        CODE = prepareKeyFactory.create("com_dict_code", String::class.java)
    }

    companion object {
        @JvmStatic
        lateinit var CODE: PrepareKey<String>
            private set
    }
}
