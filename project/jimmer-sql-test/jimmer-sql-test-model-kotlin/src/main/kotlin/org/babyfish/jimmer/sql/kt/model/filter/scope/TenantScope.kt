package org.babyfish.jimmer.sql.kt.model.filter.scope

import org.babyfish.jimmer.sql.MappedSuperclass

/**
 * 租户域数据共有的两级维度 —— 复刻 hubao_erp 的 `TenantScope`。
 *
 * 两个维度的**性质完全不同**:
 *
 * - [tenantId] 是硬隔离边界。任何上下文下都只该看本集团的数据,永远必须匹配。
 * - [organizationId] 只是「当前站在哪个组织上看数据」的视角开关,不是隔离边界。
 *   为 null 表示这条数据是集团级的。
 *
 * 问题就出在:全局过滤器是**按实体类型**绑定的,没法只对其中一个维度生效。
 * 一旦把两者写进同一个 filter,组织维度就会跟着下钻到关联表,把
 * 「属于别的组织、但已经真实发生」的数据整个滤掉。
 */
@MappedSuperclass
interface TenantScope {

    val tenantId: Long

    val organizationId: Long?
}
