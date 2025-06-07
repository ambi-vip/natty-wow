package site.weixing.natty.auth.authorization

import me.ahoo.cosec.api.policy.Policy
import me.ahoo.cosec.api.policy.PolicyType
import me.ahoo.cosec.authorization.PolicyRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.ConcurrentHashMap

/**
 * InMemoryPolicyRepository
 * @author ambi
 */
@Component
class InMemoryPolicyRepository : PolicyRepository {
    private val policies = ConcurrentHashMap<String, Policy>()
    private val globalPolicies = mutableSetOf<String>()

    override fun getGlobalPolicy(): Mono<List<Policy>> {
        return globalPolicies.mapNotNull { policies[it] }.toMono()
    }

    override fun getPolicies(policyIds: Set<String>): Mono<List<Policy>> {
        return policyIds.mapNotNull { policies[it] }.toMono()
    }

    override fun setPolicy(policy: Policy): Mono<Void> {
        return Mono.fromRunnable {
            policies[policy.id] = policy
            if (policy.type == PolicyType.GLOBAL) {
                globalPolicies.add(policy.id)
            }
        }
    }
}