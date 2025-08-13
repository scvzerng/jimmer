package org.babyfish.jimmer.benchmark;

import apijson.RequestMethod;
import apijson.framework.APIJSONCreator;
import apijson.orm.Parser;
import com.easy.query.core.api.client.EasyQueryClient;
import com.easy.query.core.bootstrapper.EasyQueryBootstrapper;
import com.easy.query.h2.config.H2DatabaseConfiguration;
import org.babyfish.jimmer.benchmark.easyquery.EasyQueryTest;
import org.babyfish.jimmer.benchmark.exposed.ExposedDataTable;
import org.babyfish.jimmer.benchmark.exposed.ExposedJavaHelperKt;
import org.babyfish.jimmer.benchmark.jdbc.JdbcDao;
import org.babyfish.jimmer.benchmark.jimmer.JimmerDataTable;
import org.babyfish.jimmer.benchmark.jimmer.kt.JimmerKtJavaHelperKt;
import org.babyfish.jimmer.benchmark.jooq.JooqData;
import org.babyfish.jimmer.benchmark.jooq.JooqDataTable;
import org.babyfish.jimmer.benchmark.ktorm.KtormDataTable;
import org.babyfish.jimmer.benchmark.mug.MugSafeSql;
import org.babyfish.jimmer.benchmark.mybatis.MyBatisDataMapper;
import org.babyfish.jimmer.benchmark.mybatis.plus.MyBatisPlusDataBaseMapper;
import org.babyfish.jimmer.benchmark.nutz.NutzData;
import org.babyfish.jimmer.benchmark.objsql.FakeObjSqlLoggerFactory;
import org.babyfish.jimmer.benchmark.objsql.ObjSqlData;
import org.babyfish.jimmer.benchmark.springjdbc.SpringJdbcDataRepository;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.jooq.DSLContext;
import org.ktorm.database.Database;
import org.ktorm.entity.EntitySequenceKt;
import org.nutz.dao.impl.NutDao;
import org.openjdk.jmh.annotations.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@State(Scope.Benchmark)
public class OrmBenchmark {

    private static final TransactionDefinition TRANSACTION_DEFINITION =
            new DefaultTransactionDefinition();

    @Param({"10", "20", "50", "100", "200", "500", "1000"})
    private int dataCount;

    private JSqlClient sqlClient;

    private KSqlClient kSqlClient;

    private MyBatisDataMapper myBatisDataMapper;

    private MyBatisPlusDataBaseMapper myBatisPlusDataBaseMapper;

    private EntityManagerFactory hibernateEntityManagerFactory;

    private EntityManagerFactory eclipseLinkEntityManagerFactory;

    private DSLContext dslContext;

    private SpringJdbcDataRepository springJdbcDataRepository;

    private Database database;

    private NutDao nutDao;

    private JdbcDao jdbcDao;
    private EasyQueryTest easyQueryTest;
    private EasyQueryClient easyQueryClient;

    private APIJSONCreator<Long> apijsonCreator;

    private MugSafeSql mug;

    @Setup
    public void initialize() throws SQLException, IOException {
        ApplicationContext ctx = SpringApplication.run(BenchmarkApplication.class);
        DatabaseInitializer databaseInitializer = ctx.getBean(DatabaseInitializer.class);
        databaseInitializer.initialize(dataCount);

        sqlClient = ctx.getBean(JSqlClient.class);
        kSqlClient = ctx.getBean(KSqlClient.class);
        myBatisDataMapper = ctx.getBean(MyBatisDataMapper.class);
        myBatisPlusDataBaseMapper = ctx.getBean(MyBatisPlusDataBaseMapper.class);
        hibernateEntityManagerFactory = ctx.getBean("hibernateEntityManagerFactory", EntityManagerFactory.class);
        eclipseLinkEntityManagerFactory = ctx.getBean("eclipseLinkEntityManagerFactory", EntityManagerFactory.class);
        dslContext = ctx.getBean(DSLContext.class);
        springJdbcDataRepository = ctx.getBean(SpringJdbcDataRepository.class);
        database = ctx.getBean(Database.class);

        TransactionAwareDataSourceProxy transactionAwareDataSource = new TransactionAwareDataSourceProxy(ctx.getBean(DataSource.class));
        ExposedJavaHelperKt.connect(transactionAwareDataSource);
        nutDao = new NutDao(transactionAwareDataSource);
        jdbcDao = new JdbcDao(transactionAwareDataSource);
        easyQueryClient = EasyQueryBootstrapper.defaultBuilderConfiguration().setDefaultDataSource(ctx.getBean(DataSource.class))
                .optionConfigure(op -> {
                    op.setPrintSql(false);
                })
                .useDatabaseConfigure(new H2DatabaseConfiguration())
                .build();
        easyQueryTest = new EasyQueryTest(easyQueryClient);

        FakeObjSqlLoggerFactory.init();

        apijsonCreator = ctx.getBean(APIJSONCreator.class);
        mug = ctx.getBean(MugSafeSql.class);
    }

    /*
     * All benchmark methods open/close the connection each time they are executed
     */

    @Benchmark
    public void runJimmer() {
        /*
         * For jimmer:
         *
         * `execute(Connection)` represents execution based on an existing connection.
         * `execute()` represents execution based on temporarily connection.
         *
         * So, this method will open/close connection by itself,
         * this is equivalent to the behavior of JPA tests
         */
        sqlClient
                .createQuery(JimmerDataTable.$)
                .select(JimmerDataTable.$)
                .execute();
    }

    @Benchmark
    public void runJimmerKt() {
        /*
         * For jimmer:
         *
         * `execute(Connection)` represents execution based on an existing connection.
         * `execute()` represents execution based on temporarily connection.
         *
         * So, this method will open/close connection by itself,
         * this is equivalent to the behavior of JPA tests
         */
        JimmerKtJavaHelperKt
                .createKtQuery(kSqlClient)
                .execute(null);
    }

    @Benchmark
    public void runMyBatis() {
        myBatisDataMapper.findAll();
    }

    @Benchmark
    public void runMyBatisPlus() {
        myBatisPlusDataBaseMapper.selectList(null);
    }

    @Benchmark
    public void runJpaByHibernate() {
        /*
         * All frameworks open/close session/connection for each benchmark test.
         * Use spring transaction aware connection pool to guarantee performance
         */
        EntityManager em = hibernateEntityManagerFactory.createEntityManager();
        try {
            em.createQuery("from JpaData").getResultList();
        } finally {
            em.close();
        }
    }

    @Benchmark
    public void runJpaByEclipseLink() {
        /*
         * All frameworks open/close session/connection for each benchmark test.
         * Use spring transaction aware connection pool to guarantee performance
         */
        EntityManager em = eclipseLinkEntityManagerFactory.createEntityManager();
        try {
            em.createQuery("select data from JpaData data").getResultList();
        } finally {
            em.close();
        }
    }

    @Benchmark
    public void runJooq() {
        dslContext.selectFrom(JooqDataTable.DATA).getQuery().fetchInto(JooqData.class);
    }

    @Benchmark
    public void runSpringDataJdbc() {
        springJdbcDataRepository.findAll();
    }

    @Benchmark
    public void runExposed() {
        ExposedJavaHelperKt.executeJavaRunnable(ExposedDataTable.INSTANCE::list);
    }

    @Benchmark
    public void runKtorm() {
        EntitySequenceKt.toList(
                EntitySequenceKt.sequenceOf(database, KtormDataTable.INSTANCE, true)
        );
    }

    @Benchmark
    public void runObjectiveSql() throws SQLException {
        ObjSqlData.queryAll();
    }

    @Benchmark
    public void runNutz() {
        nutDao.query(NutzData.class, null);
    }

    @Benchmark
    public void runJdbcByColumnIndex() throws SQLException {
        jdbcDao.findAllByColumnIndex();
    }

    @Benchmark
    public void runJdbcByColumnName() throws SQLException {
        jdbcDao.findAllByColumnName();
    }

    @Benchmark
    public void runEasyQuery() {
        easyQueryTest.selectAll();
    }

    @Benchmark
    public void runApiJson() {
        final Parser<Long> parser = apijsonCreator.createParser();
        parser.setNeedVerify(false);
        parser.setMethod(RequestMethod.GET);
        parser.parse("{\n" +
                "    \"[]\": {\n" +
                "        \"DATA\": {\n" +
                "            \"@column\": \"ID,VALUE_1,VALUE_2,VALUE_3,VALUE_4,VALUE_5,VALUE_6,VALUE_7,VALUE_8,VALUE_9\"\n" +
                "        }\n" +
                "    }\n" +
                "}");
    }

    @Benchmark
    public void runMug() {
        mug.execute();
    }

    @TearDown
    public void shutdown() {
        easyQueryClient.getRuntimeContext().getEasyTimeJobManager().shutdown();
    }
}
