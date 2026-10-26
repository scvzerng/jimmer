package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

/**
 * 第三个 sibling 子类型 —— **没有任何自己的列**。
 *
 * <p>它的作用是让「改判别列」的目标类型**既不是** {@link SharedColumnCar}
 * **也不是** {@link SharedColumnTruck} ——
 * 这样 Car 和 Truck 声明的**同一批物理列**会同时被当作「旧类型独占列」收集，
 * 从而复现「同一列在一条 UPDATE 里被 SET 两次」。
 *
 * <p>真实场景：hubao_erp 报废车的核销车 / 监销车共用行驶证等证件列，
 * 改成「废铁车」时两个旧类型的证件列都会被收集 → 同一列赋两次。
 */
@Entity
@DiscriminatorValue("BIKE")
public interface SharedColumnBike extends SharedColumnVehicle {
}
