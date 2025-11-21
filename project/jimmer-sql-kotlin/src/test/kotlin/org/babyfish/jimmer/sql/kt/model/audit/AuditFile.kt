package org.babyfish.jimmer.sql.kt.model.audit

import org.babyfish.jimmer.sql.AuditField
import org.babyfish.jimmer.sql.DissociateAction
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OnDissociate
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.OneToOne
import org.babyfish.jimmer.sql.kt.model.filter.User

@Entity
interface AuditFile {
    @Id
    val id: Long

    @Key
    val name: String

    @LogicalDeleted(generatorType = LogicalDeletedTimeGenerator::class)
    val deleteTime: Long?

    @AuditField(generatorType = LogicalDeletedTimeGenerator::class)
    @OneToOne
    val deleteBy: User?
    @AuditField(generatorType = LogicalDeletedTimeGenerator::class)
    val audit1: Long?

    @OnDissociate(DissociateAction.DELETE)
    @ManyToOne
    val parent: AuditFile?
    @OneToMany(mappedBy = "parent")
    val children: List<AuditFile>
}