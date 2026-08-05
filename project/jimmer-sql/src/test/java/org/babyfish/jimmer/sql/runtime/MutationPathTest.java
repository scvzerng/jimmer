package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Asset;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Car;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Vehicle;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.VehicleOwner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 多级表继承下 {@link MutationPath#to(ImmutableProp)} 方向检查回归测试。
 *
 * <p>0.11.6 报错:
 * <pre>
 *     IllegalArgumentException: The declaring type of property "&lt;prop&gt;"
 *     is not assignable from "&lt;parentType&gt;"
 * </pre>
 */
public class MutationPathTest {

    @Test
    public void parentToChildDeclaredAssociation() {
        MutationPath path = MutationPath.root(ImmutableType.get(Asset.class));
        ImmutableProp owner = ImmutableType.get(Vehicle.class).getProp("owner");

        MutationPath next = Assertions.assertDoesNotThrow(
                () -> path.to(owner),
                "父路径 Asset 应当接受子类型 Vehicle 重新声明的关联 owner"
        );
        Assertions.assertEquals(ImmutableType.get(VehicleOwner.class), next.getType());
    }

    @Test
    public void parentToGrandchildDeclaredAssociation() {
        MutationPath path = MutationPath.root(ImmutableType.get(Asset.class));
        ImmutableProp driver = ImmutableType.get(Car.class).getProp("driver");

        MutationPath next = Assertions.assertDoesNotThrow(
                () -> path.to(driver),
                "父路径 Asset 应当接受孙类型 Car 重新声明的关联 driver"
        );
        Assertions.assertEquals(ImmutableType.get(VehicleOwner.class), next.getType());
    }

    @Test
    public void intermediateToChildDeclaredAssociation() {
        MutationPath path = MutationPath.root(ImmutableType.get(Vehicle.class));
        ImmutableProp driver = ImmutableType.get(Car.class).getProp("driver");

        MutationPath next = Assertions.assertDoesNotThrow(
                () -> path.to(driver),
                "中间路径 Vehicle 应当接受子类型 Car 重新声明的关联 driver"
        );
        Assertions.assertEquals(ImmutableType.get(VehicleOwner.class), next.getType());
    }

    /**
     * 基线:子路径走父类型上声明的关联,0.11.6 与修复后都应通过。
     */
    @Test
    public void childToParentDeclaredAssociation() {
        MutationPath path = MutationPath.root(ImmutableType.get(Car.class));
        ImmutableProp manager = ImmutableType.get(Asset.class).getProp("manager");

        MutationPath next = Assertions.assertDoesNotThrow(
                () -> path.to(manager),
                "子路径 Car 走父类型 Asset 声明的关联 manager 应保持通过"
        );
        Assertions.assertEquals(ImmutableType.get(VehicleOwner.class), next.getType());
    }

    /**
     * 基线:路径类型与 prop 声明类型完全相同,永远应通过。
     */
    @Test
    public void samePathToSameDeclaredAssociation() {
        MutationPath path = MutationPath.root(ImmutableType.get(Car.class));
        ImmutableProp driver = ImmutableType.get(Car.class).getProp("driver");

        MutationPath next = Assertions.assertDoesNotThrow(
                () -> path.to(driver),
                "同类型路径 Car -> Car.driver 永远应通过"
        );
        Assertions.assertEquals(ImmutableType.get(VehicleOwner.class), next.getType());
    }
}
