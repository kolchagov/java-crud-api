package com.ivanceras.fluent;

import com.ivanceras.fluent.sql.Breakdown;
import org.junit.*;

import static com.ivanceras.fluent.sql.SQL.Statics.INSERT;
import static com.ivanceras.fluent.sql.SQL.Statics.SELECT;
import static org.junit.Assert.assertArrayEquals;

public class TestSQLBuilderInsert {

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
    public void test() {
        String expected = "INSERT INTO films ( SELECT * FROM tmp_films WHERE date_prod < ?  )";
        Breakdown actual = INSERT().INTO("films").FIELD(SELECT("*").FROM("tmp_films").WHERE("date_prod").LESS_THAN("2004-05007")).build();
        CTest.cassertEquals(expected, actual.getSql());
        assertArrayEquals(new Object[]{"2004-05007"}, actual.getParameters());
    }

}
