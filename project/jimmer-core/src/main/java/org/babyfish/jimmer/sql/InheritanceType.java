package org.babyfish.jimmer.sql;

public enum InheritanceType {

    /**
     * 真·Inheritance: 1 个逻辑实体 = N 行共享 PK, 每行 1 个 type.
     * 物理上多表, 通过 JOIN on shared PK 拿完整数据. 适用: 实体版本控制.
     */
    JOINED,

    /**
     * 假设"共享 PK 兄弟行"的多 type 同表范式. query 路径走 self-join, 99% 业务不适用.
     * 1 行 1 实体业务应改用 {@link #STRUCTURAL_SINGLE_TABLE}.
     */
    SINGLE_TABLE,

    /**
     * 表的多态: 1 个逻辑实体 = 1 行, type tag 决定哪些列有效.
     * 物理上单表, 所有列 nullable. 适用 99% 业务 (事件流 / 独立实体 / "1 行 1 事件"模式).
     */
    STRUCTURAL_SINGLE_TABLE;
}
