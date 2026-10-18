package org.babyfish.jimmer.sql.kt.model.filter.scope

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 业务资质 —— **组织级**基础数据,对应 hubao_erp 的 `BusinessQualification`。
 *
 * 它由「那个组织的人」操作产生,所以挂在具体组织下,`organizationId` 非空。
 * 引用它的申请单可能属于别的组织,甚至属于集团本身。
 */
@Entity
@Table(name = "SCOPE_QUALIFICATION")
interface Qualification : TenantScope {

    @Id
    val id: Long

    val name: String
}
