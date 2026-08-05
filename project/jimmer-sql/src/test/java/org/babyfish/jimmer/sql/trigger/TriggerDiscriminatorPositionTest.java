package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.trigger.TriggerDiscriminatorPositionEntityTable;
import org.junit.jupiter.api.Test;

public class TriggerDiscriminatorPositionTest extends AbstractMutationTest {

    @Override
    protected JSqlClient getSqlClient(java.util.function.Consumer<JSqlClient.Builder> block) {
        return super.getSqlClient(builder -> {
            if (block != null) {
                block.accept(builder);
            }
            builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
        });
    }

    @Test
    public void testUpdateSelectHasDiscriminatorAtColumn2() {
        TriggerDiscriminatorPositionEntityTable table = TriggerDiscriminatorPositionEntityTable.$;

        executeAndExpectRowCount(
                getSqlClient()
                        .createUpdate(table)
                        .set(table.before(), "new_before")
                        .where(table.id().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.KIND, tb_1_.BEFORE, tb_1_.AFTER " +
                                        "from TRIGGER_DISCRIMINATOR_POSITION_ENTITY tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update TRIGGER_DISCRIMINATOR_POSITION_ENTITY tb_1_ " +
                                        "set BEFORE = ? " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables("new_before", 1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.KIND, tb_1_.BEFORE, tb_1_.AFTER " +
                                        "from TRIGGER_DISCRIMINATOR_POSITION_ENTITY tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(1L);
                    });
                    ctx.rowCount(1);
                }
        );
    }
}
