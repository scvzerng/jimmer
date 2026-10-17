package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.jspecify.annotations.Nullable;

/** 第二层叶子类型,与 {@link Car} 同级。 */
@Entity
@DiscriminatorValue("TRUCK")
public interface Truck extends Vehicle {

    @Nullable
    @Column(name = "LOAD_KG")
    Integer loadKg();
}
