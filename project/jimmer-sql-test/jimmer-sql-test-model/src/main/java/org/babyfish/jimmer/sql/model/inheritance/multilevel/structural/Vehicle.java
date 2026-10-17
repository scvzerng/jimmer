package org.babyfish.jimmer.sql.model.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Inheritance;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.Table;

/**
 * STRUCTURAL_SINGLE_TABLE 的继承根 —— 「1 行 1 实体」范式:
 * 物理上只有一张表,语义上每行只属于一个具体类型。
 *
 * <p>与 {@code multilevel.singletable} 的差别在于:STRUCTURAL 下 treated 子类型跟父表
 * 物理同表、共享 alias,不会产生 self-join,判别谓词直接落主 WHERE。
 */
@Entity
@Table(name = "STRUCTURAL_VEHICLE")
@Inheritance(strategy = InheritanceType.STRUCTURAL_SINGLE_TABLE)
public interface Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "VEHICLE_TYPE")
    String type();

    String name();
}
