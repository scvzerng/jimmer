package org.babyfish.jimmer.sql.mutation.inheritance.multilevel.structural;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.structural.SharedColumnBikeDraft;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.structural.SharedColumnCarDraft;
import org.babyfish.jimmer.sql.model.inheritance.multilevel.structural.SharedColumnTruckDraft;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * STRUCTURAL_SINGLE_TABLE 下「改判别列」的列去重测试。
 *
 * <p>背景：单表继承里改判别列（{@code setTypeChangeAllowed(true)}）时，jimmer 会把
 * **旧类型独占的列**置 null（内部叫 {@code nullGetters}），同时新实体带来的列
 * （内部叫 {@code assignments}）也要写值。两者可能命中**同一个物理列** ——
 * 当两个 sibling 子类型**各自声明同名同列**时（真实场景：hubao_erp 报废车的
 * 核销车 / 监销车共用行驶证、登记证书、品牌这几列），改判别列就会撞车。
 *
 * <p>同一列在一条 UPDATE 的 SET 子句里出现两次，SQL Server 会直接报
 * 「在 INSERT 的 SET 子句或列列表中多次指定了列名」，而 H2 容忍 —— 所以这个缺陷
 * 只在 SQL Server 上暴露。
 *
 * <p>修复：{@code Operator.excludeAssignedGetters} ——
 * ① 剔除已被赋值语句覆盖的冗余列（以赋值语句为准）；
 * ② 去掉冗余列**之间**的重复（按物理列名）。
 *
 * <p>🚩 **② 是更隐蔽的那一半**：jimmer 收集冗余列时会遍历**所有**派生类型，
 * 目标类型是第三个类型时，两个 sibling 声明的**同一批列**会**同时**被收集 ——
 * 同一列在 nullGetters 里出现两次。第一个用例正是这个场景。
 */
public class StructuralSingleTableTypeChangeDedupTest extends AbstractMutationTest {

    /**
     * 核销车 → 废铁车（目标类型是**第三种**类型）。
     *
     * <p>核销车与监销车声明**同一批列**（{@code shared_license_no} 等），
     * 目标类型是废铁车时两者都是「旧类型」—— 同一批列会被收集两次。
     */
    @Test
    public void testChangeToThirdTypeDoesNotDuplicateSharedColumns() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    SharedColumnBikeDraft.$.produce(bike -> {
                                        bike.setId(800L);
                                        bike.setName("Shared Car");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return sharedColumnRow(con, 800L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        // 🚩 **每个物理列只出现一次** ——
                        //   shared_license_no / shared_brand / shared_has_license
                        //   被核销车和监销车**各声明一次**，去重前会 SET 两次。
                        it.sql(
                                "update SHARED_COLUMN_VEHICLE " +
                                        "set VEHICLE_TYPE = ?, " +
                                        "SHARED_LICENSE_NO = null, " +
                                        "SHARED_BRAND = null, " +
                                        "SHARED_HAS_LICENSE = null, " +
                                        "CAR_ENGINE_NO = null, " +
                                        "TRUCK_LOAD_KG = null, " +
                                        "NAME = ? " +
                                        "where ID = ?"
                        );
                        it.variables("BIKE", "Shared Car", 800L);
                    });
                    ctx.value("[BIKE, Shared Car, null, null, null]");
                }
        );
    }

    /**
     * 核销车 → 监销车（目标是 sibling 之一）。
     *
     * <p>共用列既是旧类型独占列、又是新实体带来的列 —— 也只 SET 一次。
     */
    @Test
    public void testChangeDiscriminatorBetweenSiblingsWithSharedColumn() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    SharedColumnTruckDraft.$.produce(truck -> {
                                        truck.setId(800L);
                                        truck.setName("Shared Car");
                                        truck.setSharedLicenseNo("CAR-001");
                                        truck.setSharedBrand("Brand A");
                                        truck.setSharedHasLicense(true);
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setTypeChangeAllowed(true)
                            .execute(con);
                    return sharedColumnRow(con, 800L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        // car_engine_no 是旧类型（Car）独占的列 → 置 null。
                        it.sql(
                                "update SHARED_COLUMN_VEHICLE " +
                                        "set VEHICLE_TYPE = ?, CAR_ENGINE_NO = null, " +
                                        "NAME = ?, SHARED_LICENSE_NO = ?, SHARED_BRAND = ?, " +
                                        "SHARED_HAS_LICENSE = ? " +
                                        "where ID = ?"
                        );
                        it.variables("TRUCK", "Shared Car", "CAR-001", "Brand A", true, 800L);
                    });
                    ctx.value("[TRUCK, Shared Car, CAR-001, Brand A, null]");
                }
        );
    }

    private static String sharedColumnRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select VEHICLE_TYPE, NAME, SHARED_LICENSE_NO, SHARED_BRAND, CAR_ENGINE_NO " +
                        "from SHARED_COLUMN_VEHICLE where ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "[" +
                        rs.getString(1) +
                        ", " +
                        rs.getString(2) +
                        ", " +
                        rs.getString(3) +
                        ", " +
                        rs.getString(4) +
                        ", " +
                        rs.getString(5) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
