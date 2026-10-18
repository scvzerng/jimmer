package org.babyfish.jimmer.sql.kt.model.filter.scope

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany

/**
 * 入驻申请单 —— **集团级**单据,对应 hubao_erp 的 `VendorJoinRequest`。
 *
 * 由集团的人发起,`organizationId` 通常为 null,
 * 但它引用了一条挂在具体组织下的资质。
 */
@Entity
@org.babyfish.jimmer.sql.Table(name = "SCOPE_REGISTRATION")
interface Registration : TenantScope {

    @Id
    val id: Long

    val name: String

    /**
     * 申报的资质。
     *
     * 🚩 必须可空 —— 与 hubao_erp 的 `VendorJoinRequestQualification.qualification` 同理:
     * 声明方不继承租户域(不受过滤器约束),目标方继承(受过滤器约束),
     * Jimmer 要求这种引用声明为可空,否则过滤后可能变成悬空引用。
     *
     * 也正因为可空,过滤发生时它是**静默变 null**,而不是抛异常 ——
     * 接口照样 200,只是这个节点消失了。
     */
    @ManyToOne
    val qualification: Qualification?

    /**
     * 🚩 反向关联(一对多)。FK 在 [ScopeRegistrationQualification] 上,
     * 这里走的是 `inverseJoinImplementor` —— 与 `@ManyToOne` 不同的入口。
     */
    @OneToMany(mappedBy = "registration")
    val qualificationLinks: List<ScopeRegistrationQualification>

    /**
     * 🚩 多对多,中间表 `SCOPE_REGISTRATION_TAG`。这条路径会先查中间表
     * (AssociationTable),中间表本身不是 entity,过滤器的判定对它是否有意义
     * 需要单独验。
     */
    @ManyToMany
    @JoinTable
    val tags: List<ScopeTag>
}
