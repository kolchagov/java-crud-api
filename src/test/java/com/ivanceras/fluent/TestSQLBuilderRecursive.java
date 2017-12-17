package com.ivanceras.fluent;

import com.ivanceras.fluent.sql.Breakdown;
import com.ivanceras.fluent.sql.SQL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.ivanceras.fluent.sql.SQL.Statics.*;
import static org.junit.Assert.assertArrayEquals;


public class TestSQLBuilderRecursive {


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
    public void testMultipleTables() {
        String expected =
                " WITH LatestOrders ( sum_count_idm , count_max_items , cname , colour ) AS (" +
                        "		SELECT " +
                        "				CustomerName , " +
                        "               SUM ( COUNT ( ID ) ) ," +
                        "				COUNT ( MAX ( n_items ) ) , " +
                        "				? as color" +
                        "			FROM dbo.Orders , customers , persons" +
                        "			RIGHT JOIN Customers" +
                        "				on Orders.Customer_ID = Customers.ID " +
                        "			LEFT JOIN Persons" +
                        "				ON Persons.name = Customer.name" +
                        "				AND Persons.lastName = Customer.lastName" +
                        "			GROUP BY CustomerID" +
                        "		) " +
                        " SELECT " +
                        "    Customers.* , " +
                        "    Orders.OrderTime AS LatestOrderTime , " +
                        "    ( SELECT COUNT ( * ) " +
                        "		FROM dbo.OrderItems " +
                        "		WHERE OrderID IN " +
                        "        ( SELECT ID FROM dbo.Orders WHERE CustomerID = Customers.ID )  " +
                        "       )     AS TotalItemsPurchased " +
                        " FROM dbo.Customers , people , pg_tables " +
                        " INNER JOIN dbo.Orders " +
                        "        USING ID" +
                        " WHERE " +
                        "	Orders.n_items > ? " +
                        "   AND Orders.ID IN ( SELECT ID FROM LatestOrders )";

        SQL sql = WITH("LatestOrders")
                .openParen()
                .FIELD("sum_count_idm", "count_max_items", "cname", "colour")
                .closeParen()
                .AS()
                .FIELD(
                        SELECT("CustomerName")
                                .FN(SUM(COUNT("ID")))
                                .FN(COUNT(MAX("n_items")))
                                .VALUE("Red").AS("color")
                                .FROM("dbo.Orders", "customers", "persons")
                                .RIGHT_JOIN("Customers")
                                .ON("Orders.customer_ID", "Customers.ID")
                                .LEFT_JOIN("Persons")
                                .ON("Persons.name", "Customer.name")
                                .AND_ON("Persons.lastName", "Customer.lastName")
                                .GROUP_BY("CustomerID")
                )
                .append(
                        SELECT()
                                .FIELD("Customers.*")
                                .FIELD("Orders.OrderTime").AS("LatestOrderTime")
                                .FIELD(SELECT(COUNT("*"))
                                        .FROM("dbo.OrderItems")
                                        .WHERE("OrderID").IN(
                                                SELECT("ID")
                                                        .FROM("dbo.Orders")
                                                        .WHERE("CustomerID").EQUAL_TO_FIELD("Customers.ID"))

                                ).AS("TotalItemsPurchased")
                                .FROM("dbo.Customers", "people", "pg_tables")
                                .INNER_JOIN("dbo.Orders")
                                .USING("ID")
                                .WHERE("Orders.n_items").GREATER_THAN(0)
                                .AND("Orders.ID").IN(SELECT("ID").FROM("LatestOrders"))
                );

        Breakdown actual = sql.build();
        System.out.println("expected: \n" + expected);
        System.out.println("actual: \n" + actual.getSql());
        CTest.cassertEquals(expected, actual.getSql());
        for (Object p : actual.getParameters()) {
            System.out.println(p.toString());
        }
        assertArrayEquals(new Object[]{"Red", 0}, actual.getParameters());
    }

    @Test
    public void testWhereInQuery() {
        String expected =
                " WITH LatestOrders AS (" +
                        "		SELECT CustomerName , " +
                        "				SUM ( COUNT ( ID ) ) ," +
                        "				COUNT ( MAX ( n_items ) ) , " +
                        "				Red as color" +
                        "			FROM dbo.Orders" +
                        "			RIGHT JOIN Customers" +
                        "				on Orders.Customer_ID = Customers.ID " +
                        "			LEFT JOIN Persons" +
                        "				ON Persons.name = Customer.name" +
                        "				AND Persons.lastName = Customer.lastName" +
                        "			GROUP BY CustomerID" +
                        "		) " +
                        "  SELECT " +
                        "    Customers.* , " +
                        "    Orders.OrderTime AS LatestOrderTime , " +
                        "    ( SELECT COUNT ( * ) " +
                        "		FROM dbo.OrderItems " +
                        "		WHERE OrderID IN " +
                        "        ( SELECT ID FROM dbo.Orders WHERE CustomerID = Customers.ID )  " +
                        "      )      AS TotalItemsPurchased " +
                        " FROM dbo.Customers " +
                        " INNER JOIN dbo.Orders " +
                        "        USING ID" +
                        " WHERE " +
                        "   Orders.ID IN ( SELECT ID FROM LatestOrders )" +
                        "	AND Orders.n_items > ? ";

        SQL sql = WITH("LatestOrders",
                SELECT("CustomerName")
                        .FN(SUM(COUNT("ID")))
                        .FN(COUNT(MAX("n_items")))
                        .FIELD("Red").AS("color")
                        .FROM("dbo.Orders")
                        .RIGHT_JOIN("Customers")
                        .ON("Orders.customer_ID", "Customers.ID")
                        .LEFT_JOIN("Persons")
                        .ON("Persons.name", "Customer.name")
                        .AND_ON("Persons.lastName", "Customer.lastName")
                        .GROUP_BY("CustomerID")
        )
                .append(SELECT()
                        .FIELD("Customers.*")
                        .FIELD("Orders.OrderTime").AS("LatestOrderTime")
                        .FIELD(SELECT(COUNT("*"))
                                .FROM("dbo.OrderItems")
                                .WHERE("OrderID").IN(
                                        SELECT("ID")
                                                .FROM("dbo.Orders")
                                                .WHERE("CustomerID").EQUAL_TO_FIELD("Customers.ID"))

                        ).AS("TotalItemsPurchased")
                        .FROM("dbo.Customers")
                        .INNER_JOIN("dbo.Orders")
                        .USING("ID")
                        .WHERE("Orders.ID").IN(SELECT("ID").FROM("LatestOrders"))
                        .AND("Orders.n_items").GREATER_THAN(0)
                );

        Breakdown actual = sql.build();

        System.out.println("expected: \n" + expected);
        System.out.println("actual: \n" + actual.getSql());
        CTest.cassertEquals(expected, actual.getSql());
        for (Object p : actual.getParameters()) {
            System.out.println(p.toString());
        }
//		assertArrayEquals(new Object[]{"Red",0} , actual.getParameters());
    }

}
