package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.kt.ast.query.KSortable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps

interface KFilterArgs<E : Any> : KSortable<KNonNullProps<E>> {

    /**
     * 当前被过滤的表是不是用户显式查询的那张表,而不是顺着关联拿到的表。
     *
     * 典型场景是多租户模型里还带一个「当前站在哪个组织上看」的维度:
     * 这些条件决定数据可见性,应该只作用在根表上;而顺着根表读出来的行,
     * 是业务发生时产生的记录。
     *
     * 组织 A 发起的申请单,完全可能引用组织 B 名下的资质 —— B2B2C 里
     * 这是业务常态,不是泄漏。再过滤一遍保护不了什么(能看到申请单就已经
     * 拿到了这份数据),只会让已经发生的事实读不出来:接口照样 200,
     * 节点却静默消失。
     *
     * 可见性属于根表。读到一条引用,前提本来就已经有权访问拥有它的那一行。
     *
     * 只要表有父表就返回 `false` —— 这样同时覆盖两种下钻形态:
     * data loader 另发的查询,以及 join 进用户语句里的表。
     *
     * @see org.babyfish.jimmer.sql.ast.impl.table.TableImplementor.getParent
     */
    val isRootQuery: Boolean
        get() = true
}
