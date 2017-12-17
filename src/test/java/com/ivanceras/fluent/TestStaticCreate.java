package com.ivanceras.fluent;


import com.ivanceras.fluent.sql.SQL;
import org.junit.*;

import static com.ivanceras.fluent.sql.SQL.Statics.CREATE_TABLE;

public class TestStaticCreate {


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
    public void testReferences() {
        String expected = "CREATE TABLE portal.user\n" +
                "(\n" +
                "  name character varying (60) ,\n" +
                "  password character varying ,\n" +
                "  firstname character varying ,\n" +
                "  lastname character varying ,\n" +
                "  email character varying ,\n" +
                "  user_id character varying NOT NULL ,\n" +
                "  photo character varying ,\n" +
                "  CONSTRAINT user_pkey PRIMARY KEY ( user_id ) ,\n" +
                "  CONSTRAINT unique_name UNIQUE ( name )\n" +
                ")";

        SQL sql = CREATE_TABLE("portal.user")
                .openParen()
                .FIELD("name").keyword("character varying").keyword("(60)").comma()
                .FIELD("password").keyword("character varying").comma()
                .FIELD("firstname").keyword("character varying").comma()
                .FIELD("lastname").keyword("character varying").comma()
                .FIELD("email").keyword("character varying").comma()
                .FIELD("user_id").keyword("character varying").NOT_NULL().comma()
                .FIELD("photo").keyword("character varying").comma()
                .CONSTRAINT("user_pkey").PRIMARY_KEY("user_id").comma()
                .CONSTRAINT("unique_name").UNIQUE("name")
                .closeParen();
        System.out.println(sql.build().getSql());
        CTest.cassertEquals(expected, sql.build().getSql());
    }

}
