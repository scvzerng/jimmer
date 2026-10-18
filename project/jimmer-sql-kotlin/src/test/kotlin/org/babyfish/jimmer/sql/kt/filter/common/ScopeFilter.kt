package org.babyfish.jimmer.sql.kt.filter.common

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.filter.KAssociationIntegrityAssuranceFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.model.filter.scope.Qualification
import org.babyfish.jimmer.sql.kt.model.filter.scope.Registration
import org.babyfish.jimmer.sql.kt.model.filter.scope.organizationId
import org.babyfish.jimmer.sql.kt.model.filter.scope.tenantId

/**
 * 复刻 hubao_erp `TenantFilter`:集团维度 + 组织维度绑在同一个过滤器里。
 *
 * Jimmer 的全局过滤器按实体类型绑定,所以集团级单据和组织级基础数据各注册一个;
 * 两者内部加的条件完全一样 —— 这正是要复现的问题:
 *
 * 🚩 两个维度的**性质不同**,却被塞进同一个 filter:
 * - `tenantId` 是硬隔离边界,任何上下文下都该生效
 * - `organizationId` 只是「站在哪个组织上看数据」的视角开关
 *
 * 于是在「集团视角下的申请单」引用「别的组织的资质」时,组织维度会把这条
 * **已经真实发生**的关联整个滤掉;又因为关联字段可空,结果是**静默变 null** ——
 * 接口照常返回,只是节点不见了。
 */

/**
 * 🚩 **修好后**的过滤器:只在根表生效。
 *
 * 这正是 `hubao_erp` 应该有的形态,也是 Jimmer 侧新增 `isRootQuery` 要支撑的语义。
 *
 * 规则只有一条:**最外层实体是主数据,顺着它读出来的都是业务数据。**
 *
 * - 主数据(用户主动发起的查询)→ 集团 + 组织条件都生效,负责数据可见性
 * - 业务数据(顺着已可见的主数据读出来的)→ 不过滤
 *
 * 为什么第二类不需要过滤:能看到主数据,就意味着已经拿到了这份数据。
 * 关联字段不是一次新的"主动查询",它是主数据的一部分。把它藏起来
 * 保护不了任何东西(B2B2C 里跨租户引用本来就合法),只会让一个
 * 已经发生的事实读不出来 —— 接口照常返回,节点却静默消失。
 *
 * 安全边界应该落在**根表**,而不是每张表。
 *
 * 实现 `KAssociationIntegrityAssuranceFilter` 是 Jimmer 的硬性要求:
 * 非空引用指向"会被过滤器处理的实体"时,过滤器必须承诺
 * 「引用不会指向看不见的实体」。本过滤器只作用于根表,对下钻的
 * 关联表不生效 —— 正是这个承诺的兑现方式。
 *
 * @see org.babyfish.jimmer.sql.filter.FilterArgs#isRootQuery
 * @see org.babyfish.jimmer.sql.kt.filter.KAssociationIntegrityAssuranceFilter
 */

/** 挂在集团级单据上的过滤器。 */
class RegistrationScopeFilter : KAssociationIntegrityAssuranceFilter<Registration> {

    override fun filter(args: KFilterArgs<Registration>) {
        ScopeContext.record("Registration", args.isRootQuery)
        if (!args.isRootQuery) {
            return
        }
        args.where(args.table.tenantId eq ScopeContext.currentTenantId)
        ScopeContext.currentOrganizationId?.let { organizationId ->
            args.where(args.table.organizationId eq organizationId)
        }
    }
}

/** 挂在组织级基础数据上的过滤器。 */
class QualificationScopeFilter : KAssociationIntegrityAssuranceFilter<Qualification> {

    override fun filter(args: KFilterArgs<Qualification>) {
        ScopeContext.record("Qualification", args.isRootQuery)
        if (!args.isRootQuery) {
            return
        }
        args.where(args.table.tenantId eq ScopeContext.currentTenantId)
        ScopeContext.currentOrganizationId?.let { organizationId ->
            args.where(args.table.organizationId eq organizationId)
        }
    }
}

/** 模拟会话上下文:当前站在哪个集团、哪个组织。 */
object ScopeContext {

    /** 每次过滤器被调用时记一笔,用来观察框架实际传下来的 isRootQuery。 */
    private val CALLS = mutableListOf<String>()

    fun record(entity: String, isRootQuery: Boolean) {
        CALLS.add("$entity(isRootQuery=$isRootQuery)")
    }

    fun drainCalls(): String = CALLS.joinToString(", ")

    fun clearCalls() {
        CALLS.clear()
    }

    private val TENANT_ID_LOCAL = ThreadLocal<Long>()
    private val ORGANIZATION_ID_LOCAL = ThreadLocal<Long>()

    /** 集团视角:organizationId 为 null,能看到本集团全部组织的数据。 */
    val currentTenantId: Long
        get() = TENANT_ID_LOCAL.get() ?: error("请先切换集团或组织")

    val currentOrganizationId: Long?
        get() = ORGANIZATION_ID_LOCAL.get()

    fun <T> withScope(tenantId: Long, organizationId: Long?, block: () -> T): T {
        val oldTenantId = TENANT_ID_LOCAL.get()
        val oldOrganizationId = ORGANIZATION_ID_LOCAL.get()
        TENANT_ID_LOCAL.set(tenantId)
        if (organizationId != null) {
            ORGANIZATION_ID_LOCAL.set(organizationId)
        } else {
            ORGANIZATION_ID_LOCAL.remove()
        }
        try {
            return block()
        } finally {
            if (oldTenantId != null) TENANT_ID_LOCAL.set(oldTenantId) else TENANT_ID_LOCAL.remove()
            if (oldOrganizationId != null) {
                ORGANIZATION_ID_LOCAL.set(oldOrganizationId)
            } else {
                ORGANIZATION_ID_LOCAL.remove()
            }
        }
    }
}
