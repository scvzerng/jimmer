package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.impl.KeyMatcherImpl;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Asset;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Car;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable.Vehicle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * 多级表继承下 {@link KeyMatcherImpl#missedProps} 方向检查回归测试。
 *
 * <p>0.11.6 行为:子类声明的 prop 传入父类型 keyMatcher 时,第一段循环单向
 * {@code prop.declaringType.isAssignableFrom(type)} 返回 false,方法返回
 * {@code null};调用方(如 {@code EntityInvestigator.fillMissedProps})对
 * {@code null} 调 {@code .isEmpty()} 时崩
 * <pre>
 *     NullPointerException
 *         at ...EntityInvestigator.fillMissedProps(...)
 * </pre>
 * 真实失败原因被掩盖,无法翻译成 {@code SaveException}。
 */
public class KeyMatcherImplMissedPropsTest {

    /**
     * 模拟运行时场景:keyMatcher 的内部 type 为父类型 Asset,但传入的 prop
     * 来自孙类型 Car(re-declared prop)。修复后应当返回空列表(单一 key
     * {@code name} 已具备),而不是 null。
     */
    @Test
    public void parentKeyMatcherWithGrandchildDeclaredProperty() throws Exception {
        KeyMatcherImpl matcher = new KeyMatcherImpl(
                ImmutableType.get(Car.class),
                Collections.singletonMap(
                        "",
                        Collections.singleton(ImmutableType.get(Car.class).getProp("name"))
                )
        );
        setInternalType(matcher, ImmutableType.get(Asset.class));

        List<ImmutableProp> missed = Assertions.assertDoesNotThrow(
                () -> matcher.missedProps(
                        Collections.singletonList(ImmutableType.get(Car.class).getProp("seatCount"))
                ),
                "父类型 Asset 的 keyMatcher 应接受孙类型 Car 重新声明的 prop"
        );
        Assertions.assertNotNull(missed, "missedProps 不得返回 null,否则下游 .isEmpty() 触发 NPE");
        Assertions.assertEquals(Collections.emptyList(), missed);
    }

    /**
     * 同上,prop 来自中间类型 Vehicle(re-declared prop)。
     */
    @Test
    public void parentKeyMatcherWithChildDeclaredProperty() throws Exception {
        KeyMatcherImpl matcher = new KeyMatcherImpl(
                ImmutableType.get(Vehicle.class),
                Collections.singletonMap(
                        "",
                        Collections.singleton(ImmutableType.get(Vehicle.class).getProp("name"))
                )
        );
        setInternalType(matcher, ImmutableType.get(Asset.class));

        List<ImmutableProp> missed = Assertions.assertDoesNotThrow(
                () -> matcher.missedProps(
                        Collections.singletonList(ImmutableType.get(Vehicle.class).getProp("manufacturer"))
                ),
                "父类型 Asset 的 keyMatcher 应接受子类型 Vehicle 重新声明的 prop"
        );
        Assertions.assertNotNull(missed, "missedProps 不得返回 null");
        Assertions.assertEquals(Collections.emptyList(), missed);
    }

    /**
     * 基线:keyMatcher 内部 type 与 prop 声明类型一致,行为应保持不变。
     */
    @Test
    public void sameTypeKeyMatcherWithSameTypeDeclaredProperty() {
        KeyMatcherImpl matcher = new KeyMatcherImpl(
                ImmutableType.get(Car.class),
                Collections.singletonMap(
                        "",
                        Collections.singleton(ImmutableType.get(Car.class).getProp("name"))
                )
        );

        List<ImmutableProp> missed = matcher.missedProps(
                Collections.singletonList(ImmutableType.get(Car.class).getProp("seatCount"))
        );
        Assertions.assertNotNull(missed);
        Assertions.assertEquals(Collections.emptyList(), missed);
    }

    /**
     * 通过 reflection 改写 final 字段 type,以模拟 EntityInvestigator
     * 中 "keyMatcher.type = ctx.path.getType()" 为父类型,但 prop 来自
     * 子类型 draft 的运行时场景。CTOR 不允许直接构造这种状态。
     */
    private static void setInternalType(KeyMatcherImpl matcher, ImmutableType newType) throws Exception {
        Field typeField = KeyMatcherImpl.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(matcher, newType);
    }
}
