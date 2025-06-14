package site.weixing.natty.domain.common.dictionary

import me.ahoo.wow.infra.prepare.PrepareKey
import me.ahoo.wow.infra.prepare.PrepareKeyFactory
import org.springframework.stereotype.Component

// 用户名预处理服务，用于防重复
@Component
class DictionaryPrepare(
    private val prepareKeyFactory: PrepareKeyFactory
) : PrepareKey<String> by prepareKeyFactory.create("username", String::class.java)
