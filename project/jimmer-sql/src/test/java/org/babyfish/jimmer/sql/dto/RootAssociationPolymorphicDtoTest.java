package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.Company;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.Director;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.OrganizationTable;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.dto.OrganizationRootCommon;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.dto.OrganizationRootCommonInput;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.Employee;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.EmployeeTable;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.FullTimeEmployee;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.Responsibility;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.dto.EmployeeRootCommon;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.dto.EmployeeRootCommonInput;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Polymorphic DTO **根关联属性** 生成路径的运行时测试。
 *
 * 跟 DeepAssociationPolymorphicDtoTest 的差别: 关联 prop 不再写在每个 branch 内部,
 * 而是声明在 polymorphic 根上 — 语义上"所有子类共有"。验证:
 *
 *   1. 根 props 关联属性的 inner DTO 嵌在顶层 class (不嵌在 branch class)
 *   2. 顶层 interface 上有 abstract getX(),branch class 实现用顶层 inner DTO 类型
 *   3. 主 SQL 投影根关联的 FK 列到 root 主表 (tb_1_)
 *   4. 根关联 1-level 走 combined 1 条 SQL (不是 per-branch)
 *   5. 根关联 2-level 走 combined 1 条 SQL
 *   6. 根关联 (combined) + branch 关联 (per-branch) 互不干扰,各走各的 SQL
 */
public class RootAssociationPolymorphicDtoTest extends AbstractQueryTest {

    /*
     * Joined-inheritance DTO — 根上声明 director (1-level) + 嵌套 officeAddress (2-level),
     * branch 各自只有标量 (Company.shareCount, GovernmentAgency.budget)。
     *
     * SQL 行为 (因为 branch 没声明关联,只有根关联):
     *   - 1 主 (投影 DIRECTOR_ID 到 tb_1_ 根表)
     *   - 1 根 1-level combined (一次拿所有 director)
     *   - 1 根 2-level combined (一次拿所有 officeAddress)
     *   = 3 条 SQL
     */
    @Test
    public void testOrganizationRootCommon() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(5000L, 5001L, 5002L, 5003L)))
                        .orderBy(table.id())
                        .select(table.fetch(OrganizationRootCommon.class)),
                ctx -> {
                    // 主 SQL: polymorphic 根 LEFT JOIN 到两个子类型表
                    // (Company / GovernmentAgency),鉴别器写在 tb_1_ 根上
                    // (因为 ORG_TYPE 在 ORG_ORGANIZATION 表,joined inheritance)。
                    //
                    // 关键: DIRECTOR_ID 投影到 tb_1_ 根上(因为 director 是根关联属性,
                    // 跟 DeepAssociationPolymorphicDtoTest 里"在每个 branch 都声明"
                    // 时投影到 branch LEFT JOIN alias 是不同的)。
                    ctx.statement(0).sql(
                            "select tb_1_.ID, tb_1_.ORG_TYPE, tb_1_.NAME, " +
                                    "tb_1_.DIRECTOR_ID, " +
                                    "tb_2_.SHARE_COUNT, " +
                                    "tb_3_.BUDGET " +
                                    "from ORG_ORGANIZATION tb_1_ " +
                                    "left join ORG_COMPANY tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.ORG_TYPE = ? " +
                                    "left join ORG_GOVERNMENT_AGENCY tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_1_.ORG_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?, ?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("COMPANY", "GOV_AGENCY", 5000L, 5001L, 5002L, 5003L);
                    // 根 1-level SQL — combined 1 条。
                    // 拿所有 root 实体的 director (IN 列表包含所有 polymorphic 根实体,
                    // 不区分 branch 类型)。投影包含 OFFICE_ADDRESS_ID 列
                    // (因为 2-level officeAddress 需要 FK seed)。
                    // IN 列表: 5000→5900, 5001→5901, 5002→5902, 5003→5903 = {5900, 5901, 5902, 5903}
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.OFFICE_ADDRESS_ID " +
                                    "from ORG_DIRECTOR tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?, ?)"
                    ).variables(5900L, 5901L, 5902L, 5903L);
                    // 根 2-level SQL — combined 1 条。
                    // 拿所有 director 的 officeAddress。
                    // IN 列表: 5900→5800, 5901→5801, 5902→5802;
                    // 5903(Dave) 没 office_address 所以不在 IN 里 (null 过滤)。
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.STREET, tb_1_.CITY " +
                                    "from ORG_ADDRESS tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(5800L, 5801L, 5802L);
                    ctx.rows(4);
                    ctx.row(0, row -> {
                        assertTrue(row instanceof OrganizationRootCommon.Company);
                        OrganizationRootCommon.Company c =
                                (OrganizationRootCommon.Company) row;
                        assertEquals(5000L, c.getId());
                        assertEquals("Acme Corp", c.getName());
                        assertEquals(10000, c.getShareCount());
                        // director 是根关联,类型是顶层 inner DTO `OrganizationRootCommon.TargetOf_director`
                        // (不是 `OrganizationRootCommon.Company.TargetOf_director`)
                        assertEquals(
                                "OrganizationRootCommon",
                                c.getDirector().getClass().getEnclosingClass().getSimpleName()
                        );
                        assertDirectorAlice(c.getDirector());
                    });
                    ctx.row(1, row -> {
                        assertTrue(row instanceof OrganizationRootCommon.Company);
                        OrganizationRootCommon.Company c =
                                (OrganizationRootCommon.Company) row;
                        assertEquals(5001L, c.getId());
                        assertEquals("Globex Inc", c.getName());
                        assertEquals(20000, c.getShareCount());
                        assertDirectorBob(c.getDirector());
                    });
                    ctx.row(2, row -> {
                        assertTrue(row instanceof OrganizationRootCommon.GovernmentAgency);
                        OrganizationRootCommon.GovernmentAgency g =
                                (OrganizationRootCommon.GovernmentAgency) row;
                        assertEquals(5002L, g.getId());
                        assertEquals("FDA", g.getName());
                        assertEquals(1000000L, g.getBudget());
                        assertDirectorCarol(g.getDirector());
                    });
                    ctx.row(3, row -> {
                        assertTrue(row instanceof OrganizationRootCommon.GovernmentAgency);
                        OrganizationRootCommon.GovernmentAgency g =
                                (OrganizationRootCommon.GovernmentAgency) row;
                        assertEquals(5003L, g.getId());
                        assertEquals("NASA", g.getName());
                        assertEquals(5000000L, g.getBudget());
                        // Dave 没有 office address -> TargetOf_address 为 null
                        assertNotNull(g.getDirector());
                        assertEquals(5903L, g.getDirector().getId());
                        assertNull(g.getDirector().getOfficeAddress());
                    });
                }
        );
    }

    /*
     * Single-table-inheritance DTO — 根上声明 supervisor (1-level, 不带 2-level),
     * branch 各自声明 responsibilities (1-level) + 嵌套 project (2-level)。
     *
     * 验证: 根关联 (combined 1-level) + branch 关联 (per-branch 1-level + per-branch 2-level)
     * 互不干扰。
     *
     * SQL 行为:
     *   - 1 主 (投影 SUPERVISOR_ID 到 tb_1_ 根主表,不是 branch LEFT JOIN alias)
     *   - 1 根 1-level supervisor combined (跨所有 employee,拿所有 supervisor)
     *   - 0 根 2-level (supervisor 没嵌套任何 2-level)
     *   - 2 branch 1-level responsibilities per-branch (FT + PT 各 1)
     *   - 2 branch 2-level project per-branch (FT + PT 各 1)
     *   = 6 条 SQL
     */
    @Test
    public void testEmployeeRootCommon() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(6000L, 6001L, 6002L, 6003L)))
                        .orderBy(table.id())
                        .select(table.fetch(EmployeeRootCommon.class)),
                ctx -> {
                    // 主 SQL: 单表继承,polymorphic 根用 LEFT JOIN 自身并按 EMP_TYPE 过滤。
                    // 关键: SUPERVISOR_ID 投影到 tb_1_ 根上(根关联属性,
                    // 跟"在每个 branch 都声明"时投影到 branch LEFT JOIN alias 不同)。
                    // 分支按鉴别器值字典序 (FULL_TIME < PART_TIME)。
                    ctx.statement(0).sql(
                            "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME, " +
                                    "tb_1_.SUPERVISOR_ID, " +
                                    "tb_2_.ANNUAL_SALARY, " +
                                    "tb_3_.HOURLY_RATE " +
                                    "from STAFF_EMPLOYEE tb_1_ " +
                                    "left join STAFF_EMPLOYEE tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.EMP_TYPE = ? " +
                                    "left join STAFF_EMPLOYEE tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_3_.EMP_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?, ?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("FULL_TIME", "PART_TIME", 6000L, 6001L, 6002L, 6003L);
                    // 根 1-level supervisor SQL — combined 1 条。
                    // 拿所有 root 实体的 supervisor (跨 FT/PT,IN 列表是所有非 null supervisor)。
                    // IN 列表: 6000=null (无 supervisor), 6001.supervisor=6000, 6002.supervisor=6000, 6003.supervisor=6001
                    // 唯一 supervisor IDs = {6000, 6001}。
                    // 不投影 DEPARTMENT_ID (supervisor 没声明 2-level)。
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME " +
                                    "from STAFF_EMPLOYEE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(6000L, 6001L);
                    // branch 1-level responsibilities — FullTimeEmployee 分支专属 (per-branch)。
                    // FT 范围 owner IDs = {6000, 6001}。
                    // 投影包含 PROJECT_ID (2-level 需要的 FK seed)。
                    ctx.statement(2).sql(
                            "select tb_1_.OWNER_ID, tb_1_.ID, tb_1_.TITLE, tb_1_.PROJECT_ID " +
                                    "from STAFF_RESPONSIBILITY tb_1_ " +
                                    "where tb_1_.OWNER_ID in (?, ?)"
                    ).variables(6000L, 6001L);
                    // branch 1-level responsibilities — PartTimeEmployee 分支专属 (per-branch)。
                    // PT 范围 owner IDs = {6002, 6003}。
                    ctx.statement(3).sql(
                            "select tb_1_.OWNER_ID, tb_1_.ID, tb_1_.TITLE, tb_1_.PROJECT_ID " +
                                    "from STAFF_RESPONSIBILITY tb_1_ " +
                                    "where tb_1_.OWNER_ID in (?, ?)"
                    ).variables(6002L, 6003L);
                    // branch 2-level project — FullTimeEmployee 分支专属 (per-branch)。
                    // FT 范围 responsibilities (6100/6101/6102) 涉及的 project IDs = {6300, 6301}。
                    ctx.statement(4).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT " +
                                    "from STAFF_PROJECT tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(6300L, 6301L);
                    // branch 2-level project — PartTimeEmployee 分支专属 (per-branch)。
                    // PT 范围 responsibilities (6103/6104) 涉及的 project IDs = {6302}。
                    ctx.statement(5).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT " +
                                    "from STAFF_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(6302L);
                    ctx.rows(4);
                    ctx.row(0, row -> {
                        assertTrue(row instanceof EmployeeRootCommon.FullTimeEmployee);
                        EmployeeRootCommon.FullTimeEmployee ft =
                                (EmployeeRootCommon.FullTimeEmployee) row;
                        assertEquals(6000L, ft.getId());
                        assertEquals("Alice Allen", ft.getFullName());
                        assertEquals(120000L, ft.getAnnualSalary());
                        // Alice 没有 supervisor
                        assertNull(ft.getSupervisor());
                        assertEquals(2, ft.getResponsibilities().size());
                    });
                    ctx.row(1, row -> {
                        assertTrue(row instanceof EmployeeRootCommon.FullTimeEmployee);
                        EmployeeRootCommon.FullTimeEmployee ft =
                                (EmployeeRootCommon.FullTimeEmployee) row;
                        assertEquals(6001L, ft.getId());
                        assertEquals("Bob Bell", ft.getFullName());
                        // supervisor 是根关联,类型是顶层 inner DTO `EmployeeRootCommon.TargetOf_supervisor`
                        assertEquals(
                                "EmployeeRootCommon",
                                ft.getSupervisor().getClass().getEnclosingClass().getSimpleName()
                        );
                        assertEquals(6000L, ft.getSupervisor().getId());
                        assertEquals("Alice Allen", ft.getSupervisor().getFullName());
                        assertEquals(1, ft.getResponsibilities().size());
                    });
                    ctx.row(2, row -> {
                        assertTrue(row instanceof EmployeeRootCommon.PartTimeEmployee);
                        EmployeeRootCommon.PartTimeEmployee pt =
                                (EmployeeRootCommon.PartTimeEmployee) row;
                        assertEquals(6002L, pt.getId());
                        assertEquals(50, pt.getHourlyRate());
                        assertEquals(6000L, pt.getSupervisor().getId());
                        assertEquals(1, pt.getResponsibilities().size());
                    });
                    ctx.row(3, row -> {
                        assertTrue(row instanceof EmployeeRootCommon.PartTimeEmployee);
                        EmployeeRootCommon.PartTimeEmployee pt =
                                (EmployeeRootCommon.PartTimeEmployee) row;
                        assertEquals(6003L, pt.getId());
                        assertEquals(35, pt.getHourlyRate());
                        assertEquals(6001L, pt.getSupervisor().getId());
                        assertEquals(1, pt.getResponsibilities().size());
                    });
                }
        );
    }

    private static void assertDirectorAlice(OrganizationRootCommon.TargetOf_director director) {
        assertNotNull(director);
        assertEquals(5900L, director.getId());
        assertEquals("Alice Anderson", director.getFullName());
        assertNotNull(director.getOfficeAddress());
        assertEquals(5800L, director.getOfficeAddress().getId());
        assertEquals("100 Main St", director.getOfficeAddress().getStreet());
        assertEquals("Beijing", director.getOfficeAddress().getCity());
    }

    private static void assertDirectorBob(OrganizationRootCommon.TargetOf_director director) {
        assertNotNull(director);
        assertEquals(5901L, director.getId());
        assertEquals("Bob Brown", director.getFullName());
        assertNotNull(director.getOfficeAddress());
        assertEquals(5801L, director.getOfficeAddress().getId());
    }

    private static void assertDirectorCarol(OrganizationRootCommon.TargetOf_director director) {
        assertNotNull(director);
        assertEquals(5902L, director.getId());
        assertEquals("Carol Carter", director.getFullName());
        assertNotNull(director.getOfficeAddress());
        assertEquals(5802L, director.getOfficeAddress().getId());
        assertEquals("300 Pine Rd", director.getOfficeAddress().getStreet());
        assertEquals("Guangzhou", director.getOfficeAddress().getCity());
    }

    /*
     * Input toEntity — joined inheritance。
     * 验证根关联属性(director + 嵌套 officeAddress)正确转回 entity。
     */
    @Test
    public void testOrganizationRootCommonInputToEntity() {
        OrganizationRootCommonInput.Company input = new OrganizationRootCommonInput.Company();
        input.setId(5000L);
        input.setName("Acme Corp");
        input.setShareCount(10000);

        // director 是根关联,Input 嵌套类型是顶层 inner DTO
        // (OrganizationRootCommonInput.TargetOf_director,不是 .Company.TargetOf_director)
        OrganizationRootCommonInput.TargetOf_director director =
                new OrganizationRootCommonInput.TargetOf_director();
        director.setId(5900L);
        director.setFullName("Alice Anderson");
        OrganizationRootCommonInput.TargetOf_director.TargetOf_officeAddress office =
                new OrganizationRootCommonInput.TargetOf_director.TargetOf_officeAddress();
        office.setId(5800L);
        office.setStreet("100 Main St");
        office.setCity("Beijing");
        director.setOfficeAddress(office);
        input.setDirector(director);

        Company entity = input.toEntity();
        assertEquals(5000L, entity.id());
        assertEquals("Acme Corp", entity.name());
        assertEquals(10000, entity.shareCount());

        Director entityDirector = entity.director();
        assertNotNull(entityDirector);
        assertEquals(5900L, entityDirector.id());
        assertEquals("Alice Anderson", entityDirector.fullName());

        assertNotNull(entityDirector.officeAddress());
        assertEquals(5800L, entityDirector.officeAddress().id());
        assertEquals("100 Main St", entityDirector.officeAddress().street());
        assertEquals("Beijing", entityDirector.officeAddress().city());
    }

    /*
     * Input toEntity — single-table-inheritance。
     * 验证根关联属性(supervisor) + branch 专属关联(responsibilities + 嵌套 project)
     * 都正确转回 entity。
     */
    @Test
    public void testEmployeeRootCommonInputToEntity() {
        // FullTimeEmployee branch — 有 supervisor (根) + responsibilities (per-branch)
        EmployeeRootCommonInput.FullTimeEmployee ftInput =
                new EmployeeRootCommonInput.FullTimeEmployee();
        ftInput.setId(6001L);
        ftInput.setFullName("Bob Bell");
        ftInput.setType("FULL_TIME");
        ftInput.setAnnualSalary(95000L);

        // supervisor 是根关联
        EmployeeRootCommonInput.TargetOf_supervisor supervisor =
                new EmployeeRootCommonInput.TargetOf_supervisor();
        supervisor.setId(6000L);
        supervisor.setFullName("Alice Allen");
        ftInput.setSupervisor(supervisor);

        // responsibilities 是 FullTimeEmployee branch 专属关联
        EmployeeRootCommonInput.FullTimeEmployee.TargetOf_responsibilities resp =
                new EmployeeRootCommonInput.FullTimeEmployee.TargetOf_responsibilities();
        resp.setId(6102L);
        resp.setTitle("Bug Fixing");
        EmployeeRootCommonInput.FullTimeEmployee.TargetOf_responsibilities.TargetOf_project project =
                new EmployeeRootCommonInput.FullTimeEmployee.TargetOf_responsibilities.TargetOf_project();
        project.setId(6300L);
        project.setName("Phoenix");
        project.setClient("Internal");
        resp.setProject(project);
        ftInput.setResponsibilities(Collections.singletonList(resp));

        FullTimeEmployee entity = ftInput.toEntity();
        assertEquals(6001L, entity.id());
        assertEquals("Bob Bell", entity.fullName());
        assertEquals(95000L, entity.annualSalary());

        Employee entitySupervisor = entity.supervisor();
        assertNotNull(entitySupervisor);
        assertEquals(6000L, entitySupervisor.id());
        assertEquals("Alice Allen", entitySupervisor.fullName());

        assertEquals(1, entity.responsibilities().size());
        Responsibility entityResp = entity.responsibilities().get(0);
        assertEquals(6102L, entityResp.id());
        assertEquals("Bug Fixing", entityResp.title());
        assertNotNull(entityResp.project());
        assertEquals(6300L, entityResp.project().id());
        assertEquals("Phoenix", entityResp.project().name());
        assertEquals("Internal", entityResp.project().client());
    }
}
