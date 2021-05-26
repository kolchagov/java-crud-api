package com.ivanceras.fluent;


import com.ivanceras.fluent.sql.SQL;
import org.junit.*;

import static com.ivanceras.fluent.sql.SQL.Statics.*;

public class TestStaticSelects {

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
        String expected =
                "SELECT *" +
                        "	 FROM Users";

        SQL sql = SELECT("*").FROM("Users");
        System.out.println(sql.build().getSql());
        CTest.cassertEquals(expected, sql.build().getSql());
    }

    @Test
    public void test2() {
        String expected =
                "SELECT *" +
                        "	 FROM Users";
        SQL sql2 = SELECT("*").FROM("Users");
        System.out.println(sql2.build().getSql());
        CTest.cassertEquals(expected, sql2.build().getSql());
    }

    @Test
    public void test3() {
        String expected = " SELECT *" +
                "	 FROM Users" +
                "	 WHERE name = ?";
        SQL sql3 = SELECT("*").FROM("Users").WHERE("name").EQUAL_TO("Lee");
        System.out.println(sql3.build().getSql());
        CTest.cassertEquals(expected, sql3.build().getSql());
    }

    @Test
    public void test4() {
        String expected = "SELECT MAX ( SUM ( length ) )" +
                "	 FROM Users" +
                "	 WHERE name = ?";
        SQL sql4 = SELECT(MAX(SUM("length"))).FROM("Users").WHERE("name").EQUAL_TO("Lee");
        System.out.println(sql4.build().getSql());
        CTest.cassertEquals(expected, sql4.build().getSql());
    }

    @Test
    public void test5() {
        String expected = "SELECT SUM ( SUM ( SUM ( length ) ) )  FROM Users WHERE name = ?";
        SQL sql5 = SELECT(SUM(SUM(SUM("length")))).FROM("Users").WHERE("name").EQUAL_TO("Lee");
        System.out.println(sql5.build().getSql());
        CTest.cassertEquals(expected, sql5.build().getSql());
    }

    @Test
    public void test6() {
        String expected =
                " SELECT SUM ( SUM ( SUM ( length ) ) )" +
                        "	 FROM Users" +
                        "	 WHERE name = ?";
        SQL sql6 = SELECT(SUM(SUM(SUM("length")))).FROM("Users").WHERE("name").EQUAL_TO("Lee");
        System.out.println(sql6.build().getSql());
        CTest.cassertEquals(expected, sql6.build().getSql());
    }

    @Test
    public void test7() {
        String expected =
                " SELECT  SUM ( SUM ( SUM ( length ) ) ) , " +
                        "	 MAX ( MAX ( SELECT" +
                        "	 FROM dual )  )" +
                        "	 FROM Users" +
                        "		 LEFT JOIN Role" +
                        "		 USING name , firstname" +
                        "		 RIGHT JOIN User_role" +
                        "		 ON" +
                        "		 id =  id" +
                        "	 WHERE name = ?" +
                        "	 AND description IN ( ? , ? , ? )" +
                        "	 UNION SELECT DISTINCT name ," +
                        "		 description" +
                        "	 WHERE name IN ( SUM ( SUM ( lee ) ) ) GROUP BY name , description" +
                        "		 ORDER BY name DESC LIMIT 10 OFFSET 20";

        SQL sql7 = SELECT(SUM(SUM(SUM("length")))
                .FN(MAX(MAX(SELECT().FROM("dual"))))
                .FROM("Users")
                .LEFT_JOIN("Role")
                .USING("name", "firstname")
                .RIGHT_JOIN("User_role")
                .ON("id", "id")
                .WHERE("name").EQUAL_TO("Lee")
                .AND("description").IN("desc1", "desc2", "desc3")
                .UNION(
                        SELECT()
                                .DISTINCT("name", "description")
                                .WHERE("name").IN(SUM(SUM("lee"))))
                .GROUP_BY("name", "description")
                .ORDER_BY("name").DESC()
                .LIMIT(10)
                .OFFSET(20)
        );
        System.out.println(sql7.build().getSql());
        CTest.cassertEquals(expected, sql7.build().getSql());
    }


    @Test
    public void test8() {
        String expected =
                " SELECT  SUM ( MAX ( MIN ( length ) ) ) , AVG (  SELECT" +
                        "	 FROM dual  )" +
                        "	 FROM Users" +
                        "		 LEFT JOIN Role" +
                        "		 USING name , firstname" +
                        "		 RIGHT JOIN User_role" +
                        "		 ON" +
                        "		 id =  id" +
                        "	 WHERE name = ?" +
                        "	 AND description IN ( ? , ? , ? )" +
                        "	 UNION SELECT DISTINCT name ," +
                        "		 description" +
                        "	 WHERE name IN ( SUM ( SUM ( lee ) ) ) " +
                        "   AND SUM ( ID ) > ? " +
                        "	 GROUP BY lower ( name ) , description " +
                        "   HAVING name = ?" +
                        "		 ORDER BY name DESC LIMIT 10 OFFSET 30 ";

        SQL sql8 = SELECT(SUM(MAX(MIN("length")))
                .FN(AVG(SELECT().FROM("dual"))))
                .FROM("Users")
                .LEFT_JOIN("Role")
                .USING("name", "firstname")
                .RIGHT_JOIN("User_role")
                .ON("id", "id")
                .WHERE("name").EQUAL_TO("Lee")
                .AND("description").IN("desc1", "desc2", "desc3")
                .UNION(
                        SELECT()
                                .DISTINCT("name", "description")
                                .WHERE("name").IN(SUM(SUM("lee")))
                                .AND(SUM("ID")).GREATER_THAN(2)
                                .GROUP_BY(LOWER("name"))
                                .FIELD("description")
                                .HAVING("name").EQUAL_TO("lee")
                                .ORDER_BY("name").DESC()
                                .LIMIT(10)
                                .OFFSET(30)
                );
        System.out.println(sql8.build().getSql());
        CTest.cassertEquals(expected, sql8.build().getSql());
    }

    @Test
    public void test9() {
        String expected =
                " SELECT name , SUM ( MAX ( MIN ( length ) ) ) , AVG (  SELECT" +
                        "	 FROM dual  ) " +
                        "	 FROM Users" +
                        "		 LEFT JOIN Role" +
                        "		 USING name , firstname" +
                        "		 RIGHT JOIN User_role" +
                        "		 ON" +
                        "		 id =  id" +
                        "	 WHERE name = ?" +
                        "	 AND description IN ( ? , ? , ? )" +
                        "	 UNION SELECT DISTINCT name ," +
                        "		 description" +
                        "	 WHERE name IN ( SELECT SUM ( SUM ( lee ) ) ) " +
                        "	 GROUP BY lower ( name ) , description " +
                        "   HAVING name = ?" +
                        "		 ORDER BY name DESC LIMIT 10 OFFSET 30 ";

        SQL sql9 = SELECT()
                .FIELD("name")
                .FN(SUM(MAX(MIN("length"))))
                .FN(AVG(SELECT().FROM("dual")))
                .FROM("Users")
                .LEFT_JOIN("Role")
                .USING("name", "firstname")
                .RIGHT_JOIN("User_role")
                .ON("id", "id")
                .WHERE("name").EQUAL_TO("Lee")
                .AND("description").IN("desc1", "desc2", "desc3")
                .UNION(
                        SELECT()
                                .DISTINCT("name", "description")
                                .WHERE("name").IN(SELECT(SUM(SUM("lee"))))
                                .GROUP_BY(LOWER("name"))
                                .FIELD("description")
                                .HAVING("name").EQUAL_TO("lee")
                                .ORDER_BY("name").DESC()
                                .LIMIT(10)
                                .OFFSET(30)
                );
        System.out.println(sql9.build().getSql());
        CTest.cassertEquals(expected, sql9.build().getSql());
    }
}
