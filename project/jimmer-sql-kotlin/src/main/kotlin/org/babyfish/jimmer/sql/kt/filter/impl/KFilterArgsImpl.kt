package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.ast.query.Sortable
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.query.Where
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose

internal class KFilterArgsImpl<E: Any>(
    val javaStatement: AbstractMutableStatementImpl,
    javaTable: TableImplementor<E>
) : KFilterArgs<E> {

    override val table: KNonNullProps<E> =
        KNonNullTableExImpl(javaTable, JOIN_DISABLED_REASON)

    // 判断「这张表是不是用户主动查的那张」—— 有父表就说明是顺着关联拿到的。
    //
    // 关联有两种形态,单个信号都盖不住:
    //  - data loader 下钻 : 另发一条独立语句,那条语句的根表同样没有父表,
    //    只有 LOAD purpose 能认出它;
    //  - join fetch      : join 进用户自己的语句,purpose 仍是 QUERY,
    //    但这张表有父表。
    //
    // 命中任意一个信号即认为不是根表。
    override val isRootQuery: Boolean = javaTable.parent == null &&
            javaStatement.context?.purpose?.type != ExecutionPurpose.Type.LOAD

    override val where: Where by lazy {
        Where(this)
    }

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaStatement.where(*predicates.mapNotNull { it?.toJavaPredicate() }.toTypedArray())
    }

    override fun where(block: () -> KNonNullExpression<Boolean>?) {
        where(block())
    }

    override fun orderBy(vararg expressions: KExpression<*>?) {
        (javaStatement as? Sortable)?.orderBy(*expressions.mapNotNull { it as Expression<*>? }.toTypedArray())
    }

    override fun orderBy(vararg orders: Order?) {
        (javaStatement as? Sortable)?.orderBy(*orders)
    }

    override fun orderBy(orders: List<Order?>) {
        (javaStatement as? Sortable)?.orderBy(orders)
    }

    override val subQueries: KSubQueries<KNonNullProps<E>> by lazy {
        KSubQueriesImpl(javaStatement, this.table as KNonNullTableEx<E>)
    }

    override val wildSubQueries: KWildSubQueries<KNonNullProps<E>> =
        KWildSubQueriesImpl(javaStatement, this.table as KNonNullTableEx<E>)

    companion object {
        @JvmStatic
        private val JOIN_DISABLED_REASON = "it is not allowed by cacheable filter"
    }
}