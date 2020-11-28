package com.ivanceras.fluent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.ivanceras.fluent.sql.SQL.Statics.SELECT;


public class TestSQLBuilderNamedColumns {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        String expected = "SELECT * FROM prefmgr WHERE prefmgr.name IS NOT NULL";
        String actual = SELECT("*").FROM("prefmgr").WHERE("prefmgr.name").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    /**
     * name = "prefmgr.name";
     * description = "prefmgr.description";
     * prefmgr_id = "prefmgr.prefmgr_id";
     * preftype = "prefmgr.preftype";
     * pid = "prefmgr.pid";
     * aid = "prefmgr.aid";
     * cid = "prefmgr.cid";
     * size = "prefmgr.size";
     */
    @Test
    public void testMultiField() {
        String expected = "SELECT prefmgr.name , prefmgr.description FROM ads.prefmgr WHERE prefmgr.name IS NOT NULL";
        String actual = SELECT("prefmgr.name", "prefmgr.description")
                .FROM("ads.prefmgr")
                .WHERE("prefmgr.name").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void testMultiFieldDistinct() {
        String expected = "SELECT DISTINCT prefmgr.name , prefmgr.description FROM ads.prefmgr WHERE prefmgr.name IS NOT NULL";
        String actual = SELECT().DISTINCT("prefmgr.name", "prefmgr.description")
                .FROM("ads.prefmgr")
                .WHERE("prefmgr.name").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void testMultiFieldDistinctOn() {
        String expected = "SELECT DISTINCT ON ( prefmgr.name , prefmgr.description ) prefmgr.name , prefmgr.description FROM ads.prefmgr WHERE prefmgr.name IS NOT NULL";
        String actual = SELECT()
                .DISTINCT_ON("prefmgr.name", "prefmgr.description")
                .FIELD("prefmgr.name", "prefmgr.description")
                .FROM("ads.prefmgr")
                .WHERE("prefmgr.name").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void testMultiFieldAsColumn() {
        String expected = "SELECT prefmgr.name AS prefmgr_name , prefmgr.description FROM ads.prefmgr WHERE prefmgr.name IS NOT NULL";
        String actual = SELECT("prefmgr.name").AS("prefmgr_name")
                .FIELD("prefmgr.description")
                .FROM("ads.prefmgr")
                .WHERE("prefmgr.name").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void testMultiFieldSQLAsColumn() {
        String expected = "SELECT prefmgr.name AS prefmgr_name , ( SELECT 1 from dual ) as one_dual , prefmgr.description FROM ads.prefmgr WHERE prefmgr.name IS NOT NULL";
        String actual = SELECT("prefmgr.name").AS("prefmgr_name")
                .FIELD(
                        SELECT("1")
                                .FROM("dual")
                ).AS("one_dual")

                .FIELD("prefmgr.description")
                .FROM("ads.prefmgr")
                .WHERE("prefmgr.name").IS_NOT_NULL().build().getSql();
        CTest.cassertEquals(expected, actual);
    }


}
