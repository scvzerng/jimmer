package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.jspecify.annotations.Nullable;

/**
 * 中间层子类型 —— {@link Tesla} 的父类型。
 *
 * <p>STRUCTURAL 下 {@code instanceOf(Car)} 会展开成 {@code VEHICLE_TYPE in ('CAR','TESLA')},
 * Tesla 作为后代一并命中。
 */
@Entity
@DiscriminatorValue("CAR")
public interface Car extends Vehicle {

    @Nullable
    @Column(name = "CAR_BRAND")
    String carBrand();
}
