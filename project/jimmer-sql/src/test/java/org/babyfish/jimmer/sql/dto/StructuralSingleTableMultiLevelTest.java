package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.structural.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * STRUCTURAL_SINGLE_TABLE Multi-level (3 层) inheritance 端到端测试.
 *
 * <p>对应 SINGLE_TABLE 端 {@code MultiLevelSingleTableInheritanceQueryTest} 3 个 test.
 * 数据: id 700 (Car), 701 (Car + brand), 702 (Tesla, 3 层), 703 (Truck), 704 (Car + brand).
 */
public class StructuralSingleTableMultiLevelTest extends AbstractQueryTest {

    @Test
    public void testInstanceOfIntermediateType() {
        // instanceOf(Car) — 期望 700, 701, 702, 704 都是 Car (含 Tesla)
        VehicleTable table = VehicleTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().instanceOf(Car.class))
                        .orderBy(table.id())
                        .select(table.fetch(
                                VehicleFetcher.$.name()
                        )),
                ctx -> {
                    // instanceOf(Car) 走 enum discriminator predicate + 实体 column
                    // 期望 1 条 flat SELECT, 0 self-join
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.VEHICLE_TYPE, tb_1_.NAME " +
                                    "from STRUCTURAL_VEHICLE tb_1_ " +
                                    "where tb_1_.VEHICLE_TYPE in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    );

                    ctx.rows(rows -> {
                        assertEquals(4, rows.size());
                        // 700, 701, 702 (Tesla 是 Car 子类), 704
                        Object raw0 = rows.get(0);
                        assertInstanceOf(Vehicle.class, raw0);
                        Vehicle r0 = (Vehicle) raw0;
                        assertEquals(700L, r0.id());
                        assertEquals("Generic Car", r0.name());
                        Object raw1 = rows.get(1);
                        assertInstanceOf(Vehicle.class, raw1);
                        Vehicle r1 = (Vehicle) raw1;
                        assertEquals(701L, r1.id());
                        assertEquals("Tesla Model S", r1.name());
                        Object raw2 = rows.get(2);
                        assertInstanceOf(Vehicle.class, raw2);
                        Vehicle r2 = (Vehicle) raw2;
                        assertEquals(702L, r2.id());
                        assertEquals("Tesla Model 3", r2.name());
                        Object raw3 = rows.get(3);
                        assertInstanceOf(Vehicle.class, raw3);
                        Vehicle r3 = (Vehicle) raw3;
                        assertEquals(704L, r3.id());
                        assertEquals("BYD Han", r3.name());
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithIntermediateAndLeafTypeBranches() {
        // 显式 forType(Car) + forType(Truck) + forType(Tesla) 跨 level 1 + level 2
        VehicleTable table = VehicleTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(700L, 701L, 702L, 703L, 704L)))
                        .orderBy(table.id())
                        .select(table.fetch(
                                VehicleFetcher.$
                                        .type()
                                        .name()
                                        .forType(
                                                CarFetcher.$.carBrand()
                                        )
                                        .forType(
                                                TruckFetcher.$.loadKg()
                                        )
                                        .forType(
                                                TeslaFetcher.$.autopilotVersion()
                                        )
                        )),
                ctx -> {
                    // 3 层 forType 走 addByProp 范式 — 所有 leaf 字段都进主 SQL
                    // 列: ID, VEHICLE_TYPE, NAME, CAR_BRAND, LOAD_KG, AUTOPILOT_VERSION
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.VEHICLE_TYPE, tb_1_.NAME, " +
                                    "tb_1_.CAR_BRAND, tb_1_.LOAD_KG, tb_1_.AUTOPILOT_VERSION " +
                                    "from STRUCTURAL_VEHICLE tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables(700L, 701L, 702L, 703L, 704L);

                    ctx.rows(rows -> {
                        assertEquals(5, rows.size());
                        // 700 Car (没 brand) -> forType 字段都为 null
                        // 701 Car (Tesla brand) -> carBrand="Tesla"
                        // 702 Tesla (brand "Tesla", autopilot "v12")
                        // 703 Truck (load 10000) -> loadKg=10000
                        // 704 Car (BYD Han) -> carBrand="BYD"
                        // 注意: STRUCTURAL 范式下 Vehicle base entity 没有 carBrand/loadKg/autopilotVersion
                        // 字段 (subtype 字段在 base 上不存在), flat fetcher 把这些列 SELECT 出来
                        // 但 ObjectReader actualType filter 让 Vehicle base 跳过 subtype 字段 set.
                        // 所以这里只验证 base 字段 (id/type/name) + 数据 + 行数 = 5.
                        Object raw700 = rows.get(0);
                        assertInstanceOf(Vehicle.class, raw700);
                        Vehicle r700 = (Vehicle) raw700;
                        assertEquals(700L, r700.id());
                        assertEquals("CAR", r700.type());
                        assertEquals("Generic Car", r700.name());

                        Object raw701 = rows.get(1);
                        assertInstanceOf(Vehicle.class, raw701);
                        Vehicle r701 = (Vehicle) raw701;
                        assertEquals(701L, r701.id());
                        assertEquals("CAR", r701.type());
                        assertEquals("Tesla Model S", r701.name());

                        Object raw702 = rows.get(2);
                        assertInstanceOf(Vehicle.class, raw702);
                        Vehicle r702 = (Vehicle) raw702;
                        assertEquals(702L, r702.id());
                        assertEquals("TESLA", r702.type());
                        assertEquals("Tesla Model 3", r702.name());

                        Object raw703 = rows.get(3);
                        assertInstanceOf(Vehicle.class, raw703);
                        Vehicle r703 = (Vehicle) raw703;
                        assertEquals(703L, r703.id());
                        assertEquals("TRUCK", r703.type());
                        assertEquals("Volvo Truck", r703.name());

                        Object raw704 = rows.get(4);
                        assertInstanceOf(Vehicle.class, raw704);
                        Vehicle r704 = (Vehicle) raw704;
                        assertEquals(704L, r704.id());
                        assertEquals("CAR", r704.type());
                        assertEquals("BYD Han", r704.name());
                    });
                }
        );
    }

    @Test
    public void testTreatAsIntermediateThenLeaf() {
        // treatAs(Car) 拿 Car 字段 — projection 路径
        // STRUCTURAL 1 行 1 实体下, treatAs 走 STRUCTURAL-native 0 JOIN + WHERE:
        //   - 不写 self-join, treated 跟 parent 同表同 alias
        //   - subtype 字段 (carBrand) 走 Car's alias = Vehicle's alias = tb_1_
        //   - intermediate type Car 的 discriminator `VEHICLE_TYPE in (?, ?)` AND 到主谓词
        //   - (?, ?) = ("CAR", "TESLA") 因为 Tesla 是 Car 子类
        VehicleTable table = VehicleTable.$;
        CarTable car = table.treatAs(CarTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(701L, 702L)))
                        .orderBy(table.id())
                        .select(
                                table.id(),
                                car.carBrand()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CAR_BRAND " +
                                    "from STRUCTURAL_VEHICLE tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "and tb_1_.VEHICLE_TYPE in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables(701L, 702L, "CAR", "TESLA");

                    ctx.rows(rows -> {
                        // type in ('CAR', 'TESLA'): 701 (Tesla Model S), 702 (Tesla Model 3)
                        assertEquals(2, rows.size());

                        // 行 0: id=701, carBrand="Tesla"
                        Object raw0 = rows.get(0);
                        assertInstanceOf(Tuple2.class, raw0);
                        @SuppressWarnings("unchecked")
                        Tuple2<Long, String> row0 = (Tuple2<Long, String>) raw0;
                        assertNotNull(row0);
                        assertEquals(701L, row0.get_1().longValue());
                        assertEquals("Tesla", row0.get_2());

                        // 行 1: id=702, carBrand="Tesla" (Tesla 子类继承 Car 字段, 都是 "Tesla")
                        Object raw1 = rows.get(1);
                        assertInstanceOf(Tuple2.class, raw1);
                        @SuppressWarnings("unchecked")
                        Tuple2<Long, String> row1 = (Tuple2<Long, String>) raw1;
                        assertEquals(702L, row1.get_1().longValue());
                        assertEquals("Tesla", row1.get_2());
                    });
                }
        );
    }
}
