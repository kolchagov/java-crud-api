package com.ivanceras.fluent;

import com.ivanceras.fluent.sql.Breakdown;
import com.ivanceras.fluent.sql.SQL;
import org.junit.*;

import static com.ivanceras.fluent.sql.SQL.Statics.SELECT;
import static com.ivanceras.fluent.sql.SQL.Statics.WITH_RECURSIVE;
import static org.junit.Assert.assertArrayEquals;


public class TestSQLBuilderIn {


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testNotIn() {
        String expected = "SELECT * FROM User WHERE name NOT IN ( ? , ? , ? )";
        Breakdown result = SELECT("*").FROM("User").WHERE("name").NOT_IN("lee", "cesar", "anthony").build();
        CTest.cassertEquals(expected, result.getSql());
        assertArrayEquals(new Object[]{"lee", "cesar", "anthony"}, result.getParameters());
    }

    @Test
    public void testIn() {
        String expected = "SELECT * FROM User WHERE name IN ( ? , ? , ? )";
        Breakdown result = SELECT("*").FROM("User").WHERE("name").IN("lee", "cesar", "anthony").build();
        CTest.cassertEquals(expected, result.getSql());
        assertArrayEquals(new Object[]{"lee", "cesar", "anthony"}, result.getParameters());
    }

    @Test
    public void test2() {
        String expected = "SELECT * FROM User WHERE name = ? ORDER BY name , lastname , password DESC";
        SQL sql = SELECT("*").FROM("User").WHERE("name").EQUAL_TO("lee").ORDER_BY("name", "lastname").FIELD("password").DESC();
        Breakdown result = sql.build();
        System.out.println(result.getSql());
        CTest.cassertEquals(expected, result.getSql());
        assertArrayEquals(new Object[]{"lee"}, result.getParameters());
    }

    @Test
    public void testOriginal() {
        String expected = "" +
                " WITH RECURSIVE child AS " +
                "  ( SELECT Option.option_id , " +
                "          Option.clause , " +
                "          Option.description , " +
                "          Option.dax_clause , " +
                "          Option.parent_option_id " +
                "   FROM dax.Option " +
                "   WHERE parent_option_id = ? " +
                "   UNION SELECT Option.option_id , " +
                "                Option.clause , " +
                "                Option.description , " +
                "                Option.dax_clause , " +
                "                Option.parent_option_id " +
                "   FROM dax.Option " +
                "   INNER JOIN child ON Option.parent_option_id = child.option_id ) " +
                "  SELECT * " +
                " FROM child INTERSECT " +
                " SELECT * " +
                " FROM dax.option " +
                " WHERE option_id NOT IN " +
                "     ( SELECT DISTINCT parent_option_id " +
                "      FROM dax.Option " +
                "    WHERE parent_option_id IS NOT NULL ) ";


        SQL sql = WITH_RECURSIVE("child",
                SELECT()
                        .FIELD("Option.option_id")
                        .FIELD("Option.clause")
                        .FIELD("Option.description")
                        .FIELD("Option.dax_clause")
                        .FIELD("Option.parent_option_id")
                        .FROM("dax.Option")
                        .WHERE("parent_option_id").EQUAL_TO("Personal-Information")
                        .UNION(
                                SELECT()
                                        .FIELD("Option.option_id")
                                        .FIELD("Option.clause")
                                        .FIELD("Option.description")
                                        .FIELD("Option.dax_clause")
                                        .FIELD("Option.parent_option_id")
                                        .FROM("dax.Option")
                                        .INNER_JOIN("child")
                                        .ON("Option.parent_option_id", "child.option_id")
                        )
        ).append(
                SELECT("*")
                        .FROM("child")
                        .INTERSECT(
                                SELECT("*")
                                        .FROM("dax.Option")
                                        .WHERE("option_id").NOT_IN(
                                        SELECT()
                                                .DISTINCT("parent_option_id")
                                                .FROM("dax.Option")
                                                .WHERE("parent_option_id").IS_NOT_NULL()
                                )
                        ));
        Breakdown bd = sql.build();
        CTest.cassertEquals(expected, bd.getSql());
    }

}
