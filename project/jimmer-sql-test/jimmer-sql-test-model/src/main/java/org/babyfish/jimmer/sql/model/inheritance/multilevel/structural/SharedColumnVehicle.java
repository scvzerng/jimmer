package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Inheritance;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

/**
 * 「两个 sibling 子类型各自声明同名同列」的继承根 ——
 * 用于复现**改判别列时同一列被 SET 两次**。
 *
 * <p>真实场景（hubao_erp 报废车申报）：核销车与监销车**各自**声明了
 * 「行驶证 / 登记证书 / 品牌」这几列 —— 同一个物理列
 * （{@code has_vehicle_license} 等），挂在两个 sibling 子类型上。
 * 把一台核销车改成监销车时，「行驶证」这一列：
 * - 既是**旧类型独占**的列（该置 null）
 * - 又是**新实体带来的**列（该写值）
 *
 * <p>同一列在一条 UPDATE 的 SET 子句里出现两次 → SQL Server 报
 * 「在 INSERT 的 SET 子句或列列表中多次指定了列名」；H2 容忍，所以这个缺陷
 * 只在 SQL Server 上暴露。
 *
 * <p>与 {@link Vehicle} 的差别：{@link Vehicle} 的 sibling 子类型列**不重叠**，
 * 改判别列时不会出现同一列被赋两次。
 */
@Entity
@Table(name = "SHARED_COLUMN_VEHICLE")
@Inheritance(strategy = InheritanceType.STRUCTURAL_SINGLE_TABLE)
public interface SharedColumnVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "VEHICLE_TYPE")
    String type();

    String name();
}
