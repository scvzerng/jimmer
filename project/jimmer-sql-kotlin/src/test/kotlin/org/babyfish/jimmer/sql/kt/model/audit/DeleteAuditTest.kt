package org.babyfish.jimmer.sql.kt.model.audit

import org.babyfish.jimmer.sql.ast.mutation.QueryReason
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.filter.common.FileFilter
import kotlin.test.Test

class DeleteAuditTest : AbstractMutationTest() {

    private val _sqlClient = sqlClient {
        addFilters(FileFilter())
    }

    @Test
    fun test() {
        FileFilter.withUser(2L) {
            executeAndExpectResult({
                _sqlClient.entities.delete(AuditFile::class, 1L, it)
            }){
                statement {
                    sql("select tb_1_.ID from AUDIT_FILE tb_1_ inner join AUDIT_FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID inner join AUDIT_FILE tb_3_ on tb_2_.PARENT_ID = tb_3_.ID where tb_3_.PARENT_ID = ? and tb_1_.DELETE_TIME is null and tb_3_.DELETE_TIME is null and tb_2_.DELETE_TIME is null")
                    variables(1L)
                    queryReason(QueryReason.TOO_DEEP)
                }
                statement {
                    sql("select tb_1_.ID from AUDIT_FILE tb_1_ inner join AUDIT_FILE tb_2_ on tb_1_.PARENT_ID = tb_2_.ID inner join AUDIT_FILE tb_3_ on tb_2_.PARENT_ID = tb_3_.ID inner join AUDIT_FILE tb_4_ on tb_3_.PARENT_ID = tb_4_.ID where tb_4_.ID = ? and tb_1_.DELETE_TIME is null and tb_4_.DELETE_TIME is null and tb_3_.DELETE_TIME is null and tb_2_.DELETE_TIME is null")
                    variables(211L)
                    queryReason(QueryReason.TOO_DEEP)
                }

                statement {
                    sql("update AUDIT_FILE tb_1_ set DELETE_TIME = ?,DELETE_BY_ID = ?,AUDIT1 = ? where exists(select * from AUDIT_FILE tb_2_ where tb_1_.PARENT_ID = tb_2_.ID and tb_2_.PARENT_ID = ?) and tb_1_.DELETE_TIME is null")
                    batchVariables(0, 1L, 1L,1L, 211L)
                    queryReason(QueryReason.NONE)
                }

                statement {
                    sql("update AUDIT_FILE set DELETE_TIME = ?,DELETE_BY_ID = ?,AUDIT1 = ? where PARENT_ID = ? and DELETE_TIME is null")
                    batchVariables(0, 1L, 1L,1L, 211L)
                    queryReason(QueryReason.NONE)
                }

                statement {
                    sql("update AUDIT_FILE set DELETE_TIME = ?,DELETE_BY_ID = ?,AUDIT1 = ? where ID = ? and DELETE_TIME is null")
                    batchVariables(0, 1L, 1L,1L, 211L)
                    queryReason(QueryReason.NONE)
                }

                statement {
                    sql("update AUDIT_FILE tb_1_ set DELETE_TIME = ?,DELETE_BY_ID = ?,AUDIT1 = ? where exists(select * from AUDIT_FILE tb_2_ where tb_1_.PARENT_ID = tb_2_.ID and tb_2_.PARENT_ID = ?) and tb_1_.DELETE_TIME is null")
                    batchVariables(0, 1L, 1L,1L, 1L)
                    queryReason(QueryReason.NONE)
                }

                statement {
                    sql("update AUDIT_FILE set DELETE_TIME = ?,DELETE_BY_ID = ?,AUDIT1 = ? where PARENT_ID = ? and DELETE_TIME is null")
                    batchVariables(0, 1L, 1L,1L, 1L)
                    queryReason(QueryReason.NONE)
                }

                statement {
                    sql("update AUDIT_FILE set DELETE_TIME = ?,DELETE_BY_ID = ?,AUDIT1 = ? where ID = ?")
                    batchVariables(0, 1L, 1L,1L, 1L)
                    queryReason(QueryReason.NONE)
                }
            }
        }
    }
}