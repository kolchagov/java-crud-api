package com.ivanceras.fluent;

import org.junit.*;

import static com.ivanceras.fluent.sql.SQL.Statics.DELETE;

public class TestSQLBuilderDelete {

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
        String expected = "DELETE FROM products WHERE price IS NOT NULL";
        String actual = DELETE().FROM("products").WHERE("price").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void test2() {
        String expected = "DELETE FROM products WHERE price = ? ";
        String actual2 = DELETE().FROM("products").WHERE("price").EQUAL_TO("10").build().getSql();
        CTest.cassertEquals(expected, actual2);

    }

}
