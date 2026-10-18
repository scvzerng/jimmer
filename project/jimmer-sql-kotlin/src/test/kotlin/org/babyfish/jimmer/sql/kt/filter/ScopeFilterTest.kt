package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.filter.common.QualificationScopeFilter
import org.babyfish.jimmer.sql.kt.filter.common.RegistrationScopeFilter
import org.babyfish.jimmer.sql.kt.filter.common.ScopeContext
import org.babyfish.jimmer.sql.kt.model.filter.scope.Qualification
import org.babyfish.jimmer.sql.kt.model.filter.scope.Registration
import org.babyfish.jimmer.sql.kt.model.filter.scope.id
import org.babyfish.jimmer.sql.kt.model.filter.scope.dto.BundledRegistrationInverseView
import org.babyfish.jimmer.sql.kt.model.filter.scope.dto.BundledRegistrationMiddleTableView
import org.babyfish.jimmer.sql.kt.model.filter.scope.dto.BundledQualificationView
import org.babyfish.jimmer.sql.kt.model.filter.scope.dto.BundledRegistrationJoinFetchView
import org.babyfish.jimmer.sql.kt.model.filter.scope.dto.BundledRegistrationView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 复现「两级维度(集团 + 组织)挤在同一个全局过滤器里」导致的问题。
 *
 * 对应 hubao_erp 的 `TenantFilter`:它同时加 `tenantId` 和 `organizationId`
 * 两个条件,而这两个条件的性质完全不同 ——
 *
 * - `tenantId` 是硬隔离边界,任何上下文下都该生效
 * - `organizationId` 只是「站在哪个组织上看数据」的视角开关
 *
 * ## 数据前提
 *
 * ```
 * SCOPE_QUALIFICATION  id=100 「报废车资质」 tenant=1000 organization=1001(组织A)
 * SCOPE_REGISTRATION   id=200 「集团代申报单」tenant=1000 organization=null  qualification=100
 * ```
 *
 * 申请单是**集团级**的(organization 为 null),它引用的资质挂在**组织 A** 下 ——
 * 这是已经真实发生、由组织 A 的人操作产生的数据。
 *
 * ## 要复现的现象
 *
 * 会话停在**组织 B(1002)** 时查这张申请单,`organizationId eq 1002` 会跟着
 * 下���到资质表,把 organization=1001 的资质整个滤掉。
 * 由于 `Registration.qualification` 是可空的,结果是**静默变 null** ——
 * 接口照常 200,只是这个节点不见了,排查起来极其困难。
 */
class ScopeFilterTest : AbstractQueryTest() {

    private val _sqlClient = sqlClient {
        addFilters(RegistrationScopeFilter(), QualificationScopeFilter())
        setConnectionManager(TestConnectionManager())
    }

    /**
     * 🚩 **核心复现用例**
     *
     * 会话停在组织 B(1002),查组织 B 名下的申请单 201。
     *
     * 数据前提:
     * - 申请单 201 organization=1002 → **主表对组织 B 可见**,过得了过滤器
     * - 它引用的资质 100 organization=1001 → **属于组织 A**
     *
     * 主表过了,下钻查询才会发生;而组织维度会随下钻查询一起生效,
     * 把这条「由组织 A 的人操作、已经真实发生」的关联整个滤掉。
     * 因为 `qualification` 可空,结果是**静默变 null** —— 接口照常返回,节点却没了。
     *
     * 期望:资质应该能查出来(集团维度相同,引用真实存在)。
     */
    @Test
    fun testOrganizationScopeKeepsQualificationOfAnotherOrganization() {
        val registration = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                // 🚩 必须显式 select 关联字段,才会触发下钻查询。
                //   findById 只取 id-only 对象,压根不会发第二条 SQL,
                //   过滤器的组织维度没有机会生效 —— 那测不到问题。
                where(table.id eq 201L)
                select(table.fetch(BundledRegistrationView::class))
            }.execute().single()
        }
        assertNotNull(registration, "申请单 201 属于组织 B,对组织 B 应该可见")
        assertEquals("组织B代申报单", registration.name)
        val qualification = registration.qualification
        println(">>> 组织B视角下,主表可见;它的 qualification = $qualification")
        assertNotNull(
            qualification,
            "🚩 复现成功:申请单属于组织 B、引用组织 A 的资质," +
                "这条引用真实存在,不应该因为组织维度跟着下钻就被滤掉"
        )
    }

    /**
     * 组织视角(organizationId = 1002,组织 B)下观察**集团级**申请单 200。
     *
     * 它 organization_id 为 null,对组织 B 不可见 —— 主表就先被滤掉了,
     * 下钻查询压根不会发生。这条用来固定「主表先被滤掉」这个对照事实:
     * 想观察关联问题,必须先让主表可见(见上面那条)。
     */
    @Test
    fun testOrganizationScopeDropsGroupLevelRegistration() {
        val registration = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                where(table.id eq 200L)
                select(table.fetch(BundledRegistrationView::class))
            }.execute().singleOrNull()
        }
        println(">>> 组织视角(1002)下的集团级申请单 = $registration")
        assertEquals(null, registration, "集团级申请单对组织 B 不可见")
    }

    /**
     * 🚩 **B2B2C 场景**
     *
     * 租户 1000 的组织向租户 2000 的组织下单,单据上引用了对方的资质。
     * 业务处理人员必须看得见 —— 跨租户引用在 B2B2C 里是天经地义的,不是越权。
     *
     * 这条同时验证「根表过滤、下钻不过滤」这个规则不会把跨租户业务数据打死。
     */
    @Test
    fun testCrossTenantReferenceIsVisible() {
        val registration = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                where(table.id eq 202L)
                select(table.fetch(BundledRegistrationView::class))
            }.execute().single()
        }
        assertNotNull(registration, "单据 202 属于本组织,应该可见")
        assertEquals("跨租户订单", registration.name)
        println(">>> B2B2C 跨租户单据的 qualification = ${registration.qualification}")
        assertNotNull(
            registration.qualification,
            "🚩 B2B2C 下跨租户引用本来就合法,处理人必须能看到对方资质"
        )
    }

    /**
     * 🚩 **主数据仍然被隔离**
     *
     * 只过滤根表,不代表谁都能查所有数据 —— 用户主动查资质列表时,
     * 组织 B 依然只能看到自己的资质。这条防止「只过滤根表」被误用成「不隔离」。
     */
    @Test
    fun testMasterDataIsStillIsolatedOnRootQuery() {
        val qualifications = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Qualification::class) {
                orderBy(table.id)
                select(table.fetch(BundledQualificationView::class))
            }.execute()
        }
        val names = qualifications.map { it.name }
        println(">>> 组织B主动查资质列表 = $names")
        assertEquals(
            listOf("采购加工资质"),
            names,
            "🚩 主数据必须隔离:组织 B 主动查询时只能看到自己的资质"
        )
    }

    /**
     * 🚩 **join fetch 场景**
     *
     * 与 data loader 下钻不同,关联表是 join 进**同一条 SQL** 的,过滤条件落在 ON 上。
     *
     * 这条曾经暴露一个更严重的问题:此时语句 purpose 仍是 QUERY,按 purpose 判根表会
     * 把关联表误当成根表,于是 `organizationId` 条件被加进 ON 子句 —— 而主表 201 引用
     * 的是组织 A 的资质,join 不匹配,**整行(连主表一起)消失**,接口表现成「单据不存在」。
     *
     * 比关联变 null 更难排查,所以这里断言主表必须仍然查得到。
     */
    @Test
    fun testJoinFetchDoesNotNarrowRootTable() {
        ScopeContext.clearCalls()
        val results = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                where(table.id eq 201L)
                select(table.fetch(BundledRegistrationJoinFetchView::class))
            }.execute()
        }
        println(">>> [join fetch] 过滤器调用序列 = ${ScopeContext.drainCalls()}")
        assertEquals(1, results.size, "🚩 join fetch 下关联表的过滤条件把主表也窄化掉了")
        assertEquals("组织B代申报单", results[0].name)
        assertNotNull(
            results[0].qualification,
            "🚩 join fetch 同样要能读到别的组织下的资质"
        )
    }

    /**
     * 🚩 **对照:data loader 下钻路径**
     *
     * 关联表在**另发的一条 SQL** 里,由 data loader 以 `ExecutionPurpose.LOAD` 发出。
     * 根表 true、下钻 false,过滤条件不落到关联上。
     */
    @Test
    fun testDataLoaderDrillDownSkipsFilter() {
        ScopeContext.clearCalls()
        ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                where(table.id eq 201L)
                select(table.fetch(BundledRegistrationView::class))
            }.execute().single()
        }
        println(">>> [data loader] 过滤器调用序列 = ${ScopeContext.drainCalls()}")
        assertEquals(
            "Registration(isRootQuery=true), Qualification(isRootQuery=false)",
            ScopeContext.drainCalls(),
            "根表要过滤,下钻表不���过滤"
        )
    }

    /**
     * 🚩 **反向关联路径(inverseJoinImplementor)**
     *
     * `qualificationLinks` 是 `@OneToMany(mappedBy)`,走的是和 `@ManyToOne`
     * 不同的 TableImpl 工厂 —— 它先查中间实体,再从中间实体指向资质。
     */
    @Test
    fun testInverseAssociationPath() {
        ScopeContext.clearCalls()
        val registration = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                where(table.id eq 201L)
                select(table.fetch(BundledRegistrationInverseView::class))
            }.execute().single()
        }
        println(">>> [反向关联] 调用序列 = ${ScopeContext.drainCalls()}")
        assertEquals("组织B代申报单", registration.name)
        assertEquals(1, registration.qualificationLinks.size, "反向关联查得到")
        assertNotNull(
            registration.qualificationLinks[0].qualification,
            "🚩 反向关联下也要能读到别的组织的资质"
        )
    }

    /**
     * 🚩 **多对多路径(中间表)**
     *
     * 这条会先查中间表 `SCOPE_REGISTRATION_TAG`(不是 entity),
     * 再查目标 `SCOPE_TAG`。中间表上过滤器是否被调用、`isRootQuery` 是什么,
     * 都要单独确认。
     */
    @Test
    fun testMiddleTablePath() {
        ScopeContext.clearCalls()
        val registration = ScopeContext.withScope(1000L, 1002L) {
            _sqlClient.createQuery(Registration::class) {
                where(table.id eq 201L)
                select(table.fetch(BundledRegistrationMiddleTableView::class))
            }.execute().single()
        }
        println(">>> [中间表] 调用序列 = ${ScopeContext.drainCalls()}")
        assertEquals("组织B代申报单", registration.name)
        assertEquals(listOf("重点客户"), registration.tags.map { it.name })
    }

    /** 直接查组织 A 的资质,确认数据本身是好的,排除「数据没插进去」这种可能。 */
    @Test
    fun testQualificationOfOrgAIsVisibleUnderItsOwnScope() {
        val qualification = ScopeContext.withScope(1000L, 1001L) {
            _sqlClient.createQuery(Qualification::class) {
                select(table.fetch(BundledQualificationView::class))
            }.execute().single()
        }
        assertEquals("报废车资质", qualification.name)
    }
}
