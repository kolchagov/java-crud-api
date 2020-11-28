package com.ivanceras.fluent;

import com.ivanceras.fluent.sql.Breakdown;
import com.ivanceras.fluent.sql.SQL;
import org.junit.Test;

import static com.ivanceras.fluent.sql.SQL.Statics.*;
import static org.junit.Assert.assertArrayEquals;

public class TestSQL1 {

    @Test
    public void test() {
        String expected = "" +
                " SELECT Customers.CustomerName , Orders.OrderID " +
                " FROM Customers " +
                " INNER JOIN Orders " +
                " ON Customers.CustomerID = Orders.CustomerID " +
                " ORDER BY Customers.CustomerName ";
        String actual = SELECT("Customers.CustomerName", "Orders.OrderID")
                .FROM("Customers")
                .INNER_JOIN("Orders")
                .ON("Customers.CustomerID", "Orders.CustomerID")
                .ORDER_BY("Customers.CustomerName").build().getSql();
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void test2() {
        String expected =
                " WITH LatestOrders AS (" +
                        "		SELECT  MAX ( ID ) " +
                        "			FROM dbo.Orders " +
                        "			GROUP BY CustomerID" +
                        "		) " +
                        "  SELECT " +
                        "    Customers.* , " +
                        "    Orders.OrderTime AS LatestOrderTime , " +
                        "    ( SELECT  COUNT ( * ) " +
                        "		FROM dbo.OrderItems " +
                        "		WHERE OrderID IN " +
                        "        ( SELECT ID FROM dbo.Orders WHERE CustomerID = Customers.ID )  )" +
                        "            AS TotalItemsPurchased " +
                        " FROM dbo.Customers " +
                        " INNER JOIN dbo.Orders " +
                        "        ON Customers.ID = Orders.CustomerID " +
                        " WHERE " +
                        "    Orders.ID IN ( SELECT ID FROM LatestOrders ) ";

        SQL sql =
                WITH("LatestOrders",
                        SELECT(MAX("ID"))
                                .FROM("dbo.Orders")
                                .GROUP_BY("CustomerID")
                )
                        .append(SELECT()
                                .FIELD("Customers.*")

                                .FIELD("Orders.OrderTime").AS("LatestOrderTime")

                                .FIELD(SELECT(COUNT("*"))
                                        .FROM("dbo.OrderItems")
                                        .WHERE("OrderID").IN(SELECT("ID")
                                                .FROM("dbo.Orders")
                                                .WHERE("CustomerID").EQUAL_TO_FIELD("Customers.ID"))

                                ).AS("TotalItemsPurchased")
                                .FROM("dbo.Customers")
                                .INNER_JOIN("dbo.Orders")
                                .ON("Customers.ID", "Orders.CustomerID")
                                .WHERE("Orders.ID").IN(SELECT("ID").FROM("LatestOrders")));

        String actual = sql.build().getSql();

        System.out.println("expected: \n" + expected);
        System.out.println("actual: \n" + actual);
        CTest.cassertEquals(expected, actual);
    }

    @Test
    public void testDelete() {
        String expected = "DELETE FROM products WHERE price = ?";
        String actual = DELETE().FROM("products").WHERE("price").EQUAL_TO(10).build().getSql();
        String actual2 = DELETE().FROM("products").WHERE("price").EQUAL_TO("10").build().getSql();
        CTest.cassertEquals(expected, actual);
        CTest.cassertEquals(expected, actual2);
    }


    @Test
    public void testRecursiveComplexFunctions() {
        String expected =
                " WITH LatestOrders AS (" +
                        "		SELECT CustomerName ,  SUM ( COUNT ( ID ) )  ," +
                        "				 COUNT ( MAX ( n_items ) ) " +
                        "				" +
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
                        "    ( SELECT  COUNT ( * ) " +
                        "		FROM dbo.OrderItems " +
                        "		WHERE OrderID IN " +
                        "        ( SELECT ID FROM dbo.Orders WHERE CustomerID = Customers.ID )  " +
                        "      )      AS TotalItemsPurchased " +
                        " FROM dbo.Customers " +
                        " INNER JOIN dbo.Orders " +
                        "        USING ID" +
                        " WHERE " +
                        "	Orders.n_items > ? " +
                        "   AND Orders.ID IN ( SELECT ID FROM LatestOrders ) ";

        SQL sql = WITH("LatestOrders",
                SELECT("CustomerName")
                        .FN(SUM(COUNT("ID")))
                        .FN(COUNT(MAX("n_items")))
                        .FROM("dbo.Orders")
                        .RIGHT_JOIN("Customers")
                        .ON("Orders.customer_ID", "Customers.ID")
                        .LEFT_JOIN("Persons")
                        .ON("Persons.name", "Customer.name")
                        .AND_ON("Persons.lastName", "Customer.lastName")
                        .GROUP_BY("CustomerID")
        ).append(SELECT()
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
                .WHERE("Orders.n_items").GREATER_THAN(0)
                .AND("Orders.ID").IN(SELECT("ID").FROM("LatestOrders")));

        Breakdown actual = sql.build();

        System.out.println("expected: \n" + expected);
        System.out.println("actual: \n" + actual.getSql());
        CTest.cassertEquals(expected, actual.getSql());
    }

    @Test
    public void testInsert() {
        String expected = "INSERT INTO films ( SELECT * FROM tmp_films WHERE date_prod < ?  )";
        Breakdown actual = INSERT().INTO("films").FIELD(SELECT("*").FROM("tmp_films").WHERE("date_prod").LESS_THAN("2004-05007")).build();
        CTest.cassertEquals(expected, actual.getSql());
        assertArrayEquals(new Object[]{"2004-05007"}, actual.getParameters());
    }


}
