package org.babyfish.jimmer.sql.kt.model.audit

import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator

class LogicalDeletedTimeGenerator: LogicalDeletedValueGenerator<Long> {
    override fun generate(): Long {
        return 1L
    }
}