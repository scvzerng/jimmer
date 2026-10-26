package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

/**
 * 车辆类型 —— **枚举判别列**（对应 hubao_erp 的 {@code DisposalCategory}）。
 *
 * <p>注意：判别列用 enum 而不是 String，是刻意对齐 hubao_erp 的报废车模型 ——
 * 那边 {@code type} 是 {@code DisposalCategory} 枚举，{@code @DiscriminatorValue}
 * 用的是枚举常量名。
 */
public enum SharedColumnVehicleType {
    CAR,
    TRUCK,
    BIKE
}
