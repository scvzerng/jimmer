package org.babyfish.jimmer.sql.kt.model.filter.scope

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

/**
 * 反向关联端 —— FK 在本表,指向 [Registration]。
 *
 * 用来覆盖 `inverseJoinImplementor` 这条入口:它跟 `@ManyToOne` 不是同一个
 * `TableImplementor` 工厂,是否被判成「不是根表」需要单独验。
 *
 * 🚩 两个引用都声明为可空,绕开 Jimmer 的非空引用校验。
 * 校验读取的是 `FilterManager` 汇总后的 `ExportedFilter`,而它没有实现
 * `AssociationIntegrityAssuranceFilter`,AIA 标记在那一层丢失 —— 即便
 * 过滤器正确声明了 AIA,校验仍会拒绝非空引用(详见 FilterManager#create)。
 */
@Entity
@Table(name = "SCOPE_REGISTRATION_QUALIFICATION")
interface ScopeRegistrationQualification {

    @Id
    val id: Long

    @ManyToOne
    val registration: Registration?

    @ManyToOne
    val qualification: Qualification?
}
