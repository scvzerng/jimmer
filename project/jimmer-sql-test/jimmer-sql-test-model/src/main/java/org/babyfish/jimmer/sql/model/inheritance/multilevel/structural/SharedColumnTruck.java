package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.jspecify.annotations.Nullable;

/**
 * sibling 子类型 B —— 与 {@link SharedColumnCar} **共用** {@code shared_license_no} 等列。
 *
 * <p>两者各自声明同名同列的属性，物理上落在同一张表的同一列上。
 */
@Entity
@DiscriminatorValue("TRUCK")
public interface SharedColumnTruck extends SharedColumnVehicle {

    @Nullable
    @Column(name = "SHARED_LICENSE_NO")
    String sharedLicenseNo();

    @Nullable
    @Column(name = "SHARED_BRAND")
    String sharedBrand();

    @Nullable
    @Column(name = "SHARED_HAS_LICENSE")
    Boolean sharedHasLicense();

    @Nullable
    @Column(name = "TRUCK_LOAD_KG")
    Integer truckLoadKg();
}
