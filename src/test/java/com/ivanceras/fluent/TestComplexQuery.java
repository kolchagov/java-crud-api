package com.ivanceras.fluent;


import com.ivanceras.fluent.sql.SQL;
import org.junit.*;

import static com.ivanceras.fluent.sql.SQL.Statics.*;


public class TestComplexQuery {

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
                        "		SELECT  MAX ( ID )  " +
                        "			FROM dbo.Orders " +
                        "			GROUP BY CustomerID" +
                        "		) " +
                        "  SELECT " +
                        "    Customers.* , " +
                        "    Orders.OrderTime AS LatestOrderTime , " +
                        "    ( SELECT  COUNT ( * )  " +
                        "		FROM dbo.OrderItems " +
                        "		WHERE OrderID IN " +
                        "        ( SELECT ID FROM dbo.Orders WHERE CustomerID = Customers.ID )  " +
                        "     )       AS TotalItemsPurchased " +
                        " FROM dbo.Customers " +
                        " INNER JOIN dbo.Orders " +
                        "        ON Customers.ID = Orders.CustomerID " +
                        " WHERE " +
                        "    Orders.ID IN ( SELECT ID FROM LatestOrders ) ";

        SQL sql = WITH("LatestOrders", SELECT(MAX("ID"))
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


}
