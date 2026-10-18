package org.babyfish.jimmer.sql.kt.model.filter.scope

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 多对多的目标端 —— 挂在**别的组织**下,用来验中间表路径。
 *
 * [Registration.tags] 指向它时会先经过 `SCOPE_REGISTRATION_TAG` 中间表。
 * 中间表不是 entity,过滤器的 `isRootQuery` 对它是否适用需要单独确认。
 */
@Entity
@Table(name = "SCOPE_TAG")
interface ScopeTag {

    @Id
    val id: Long

    val name: String
}
