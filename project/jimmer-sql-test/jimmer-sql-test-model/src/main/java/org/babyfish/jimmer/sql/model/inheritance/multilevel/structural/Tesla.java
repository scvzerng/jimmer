package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.jspecify.annotations.Nullable;

/**
 * 第三层叶子类型,继承 {@link Car}。
 *
 * <p>覆盖跨 3 层的 {@code forType} 与 {@code treatAs}:{@code treatAs(Car)} 的判别条件是
 * {@code VEHICLE_TYPE in ('CAR','TESLA')},所以 Tesla 行也会被当成 Car 读出
 * (并继承 {@link Car#carBrand()})。
 */
@Entity
@DiscriminatorValue("TESLA")
public interface Tesla extends Car {

    @Nullable
    @Column(name = "AUTOPILOT_VERSION")
    String autopilotVersion();
}
