package com.ivanceras.fluent.sql;

import java.util.*;

/**
 * Dumbed down SQL class, acts as close to StringBuilder, to write the SQL statement
 * smart mode, takes care of commas, open and close parenthesis depending whether it follows such field,function, table etc.
 *
 * @author lee
 * @version $Id: $Id
 */
public class SQL {


    private final String FIELD = "FIELD";
    private final String FUNCTION = "FUNCTION";
    private final String KEYWORD = "KEYWORD";
    private final String TABLE = "TABLE";
    private final String VALUE = "VALUE";
    String lastCall = null;
    boolean smartMode = true;//if on smart mode, adds commas, and parenthesis automatically if possible.
    int tabs = 0;
    private List<Object> keywords = new LinkedList<>();// can be string and SQL
    private List<Object> values = new LinkedList<>();

    /**
     * <p>WITH.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WITH() {
        return keyword("WITH");
    }

    /**
     * <p>OVER.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL OVER() {
        return keyword("OVER");
    }

    /**
     * <p>WITH.</p>
     *
     * @param queryName a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WITH(String queryName) {
        return WITH().keyword(queryName);
    }

    /**
     * <p>RECURSIVE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RECURSIVE() {
        return keyword("RECURSIVE");
    }

    /**
     * <p>WITH.</p>
     *
     * @param queryName a {@link java.lang.String} object.
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WITH(String queryName, SQL sql) {
        return WITH(queryName).AS().FIELD(sql);
    }

    /**
     * <p>WITH_RECURSIVE.</p>
     *
     * @param queryName a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WITH_RECURSIVE(String queryName) {
        return WITH().RECURSIVE().keyword(queryName);
    }

    /**
     * <p>WITH_RECURSIVE.</p>
     *
     * @param queryName a {@link java.lang.String} object.
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WITH_RECURSIVE(String queryName, SQL sql) {
        return WITH().RECURSIVE().keyword(queryName).AS().FIELD(sql);
    }

    /**
     * <p>ALTER.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ALTER() {
        return keyword("ALTER");
    }

    /**
     * <p>ALTER_TABLE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ALTER_TABLE(String table) {
        return ALTER().TABLE(table);
    }

    /**
     * <p>AVG.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AVG(SQL sql) {
        return FUNCTION("AVG", sql);
    }

    /**
     * <p>AVG.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AVG(String column) {
        return FUNCTION("AVG", column);
    }

    /**
     * <p>COUNT.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL COUNT(SQL sql) {
        return FUNCTION("COUNT", sql);
    }

    /**
     * <p>COUNT.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL COUNT(String column) {
        return FUNCTION("COUNT", column);
    }

    /**
     * <p>CREATE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CREATE() {
        return keyword("CREATE");
    }

    /**
     * <p>CREATE_TABLE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CREATE_TABLE(String table) {
        return CREATE().TABLE(table);
    }

    /**
     * <p>DELETE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DELETE() {
        return keyword("DELETE");
    }

    /**
     * <p>DELETE_FROM.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DELETE_FROM(String table) {
        return DELETE().FROM(table);
    }

    /**
     * <p>DROP.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DROP() {
        return keyword("DROP");
    }

    /**
     * <p>DROP_TABLE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DROP_TABLE() {
        return DROP().keyword("TABLE");
    }

    /**
     * <p>DROP_TABLE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DROP_TABLE(String table) {
        return DROP().TABLE(table);
    }

    /**
     * <p>INSERT.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INSERT() {
        return keyword("INSERT");
    }

    /**
     * <p>instance.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL instance() {
        return new SQL();
    }

    /**
     * <p>LOWER.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LOWER(SQL sql) {
        return FUNCTION("LOWER", sql);
    }

    /**
     * <p>LOWER.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LOWER(String column) {
        return FUNCTION("LOWER", column);
    }

    /**
     * <p>MAX.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL MAX(SQL sql) {
        return FUNCTION("MAX", sql);
    }

    /**
     * <p>MAX.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL MAX(String column) {
        return FUNCTION("MAX", column);
    }

    /**
     * <p>MIN.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL MIN(SQL sql) {
        return FUNCTION("MIN", sql);
    }

    /**
     * <p>MIN.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL MIN(String column) {
        return FUNCTION("MIN", column);
    }

    /**
     * <p>SELECT.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SELECT() {
        return keyword("SELECT");
    }

    /**
     * <p>SELECT.</p>
     *
     * @param arg a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SELECT(SQL arg) {
        return SELECT().field(arg);
    }

    /**
     * <p>SELECT.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SELECT(String... columns) {
        return SELECT().FIELD(columns);
    }

    /**
     * <p>SUM.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SUM(SQL sql) {
        return FUNCTION("SUM", sql);
    }

    /**
     * <p>SUM.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SUM(String column) {
        return FUNCTION("SUM", column);
    }

    /**
     * <p>TRUNCATE_TABLE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL TRUNCATE_TABLE(String table) {
        return keyword("TRUNCATE").TABLE(table);
    }

    /**
     * <p>UPDATE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UPDATE(String table) {
        return keyword("UPDATE").FIELD(table);
    }

    /**
     * <p>UPPER.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UPPER(SQL sql) {
        return FUNCTION("UPPER", sql);
    }

    /**
     * <p>UPPER.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UPPER(String column) {
        return FUNCTION("UPPER", column);
    }

    /**
     * <p>ADD.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ADD() {
        return keyword("ADD");
    }

    /**
     * <p>AND.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AND() {
        return keyword("AND");
    }

    /**
     * <p>AND.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AND(SQL sql) {
        return keyword("AND").FN(sql);
    }

    /**
     * <p>AND.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AND(String column) {
        return AND().FIELD(column);
    }

    /**
     * <p>AND_ON.</p>
     *
     * @param column1 a {@link java.lang.String} object.
     * @param column2 a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AND_ON(String column1, String column2) {
        return AND().FIELD(column1).EQUAL_TO_FIELD(column2);
    }

    /**
     * <p>append.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL append(SQL sql) {
        keywords.add(sql);
        lastCall = KEYWORD;
        return this;
    }

    /**
     * <p>AS.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AS() {
        return keyword("AS");
    }

    /**
     * <p>AS.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AS(SQL sql) {
        return AS().FIELD(sql);
    }

    /**
     * <p>AS.</p>
     *
     * @param columnAs a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL AS(String columnAs) {
        AS().FIELD(columnAs);
        lastCall = FIELD;
        return this;
    }

    /**
     * <p>ASC.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ASC() {
        keyword("ASC");
        lastCall = FIELD;
        return this;
    }

    /**
     * <p>build.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.Breakdown} object.
     */
    public Breakdown build() {
        Breakdown bk = new Breakdown();
        build(bk, this);
        return bk;
    }

    /**
     * <p>build.</p>
     *
     * @param bk a {@link com.ivanceras.fluent.sql.Breakdown} object.
     * @param passed a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.Breakdown} object.
     */
    public Breakdown build(Breakdown bk, SQL passed) {
        List<Object> passedKeywords = passed.keywords;
        List<Object> passedValues = passed.values;
        for (Object keyword : passedKeywords) {
            if (keyword != null) {
                Class<?> keyClass = keyword.getClass();
                if (keyClass.equals(String.class)) {
                    bk.appendSp((String) keyword);
                } else if (keyClass.equals(SQL.class)) {
                    build(bk, (SQL) keyword);
                }
            }
        }
        for (Object value : passedValues) {
            bk.addParameter(value);
        }
        return bk;
    }

    /**
     * <p>CASCADE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CASCADE() {
        return keyword("CASCADE");
    }

    /**
     * <p>RESTRICT.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RESTRICT() {
        return keyword("RESTRICT");
    }

    /**
     * <p>CASE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CASE() {
        return keyword("CASE");
    }

    /**
     * <p>CAST.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CAST() {
        return keyword("CAST");
    }

    /**
     * <p>chars.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL chars(String keyword) {
        keywords.add(keyword);
        return this;
    }

    /**
     * <p>CHECK.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CHECK() {
        return keyword("CHECK").ln();
    }

    /**
     * <p>closeParen.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL closeParen() {
        lastCall = "_CLOSE_PAREN_";
        return chars(")");
    }

    /**
     * <p>COALESCE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL COALESCE() {
        return keyword("COALESCE");
    }

    /**
     * <p>COLUMN.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL COLUMN() {
        return keyword("COLUMN");
    }

    /**
     * <p>comma.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL comma() {
        return chars(",");
    }

    /**
     * <p>CONNECT_BY.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CONNECT_BY() {
        return keyword("CONNECT BY");
    }

    /**
     * <p>CONSTRAINT.</p>
     *
     * @param constraintName a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CONSTRAINT(String constraintName) {
        return keyword("CONSTRAINT").keyword(constraintName).ln();
    }

    /**
     * <p>CROSS_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL CROSS_JOIN(String table) {
        return keyword("CROSS JOIN").FIELD(table).ln();
    }

    /**
     * <p>DECODE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DECODE() {
        return keyword("DECODE");
    }

    /**
     * <p>DESC.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DESC() {
        keyword("DESC");
        lastCall = FIELD;
        return this;
    }

    /**
     * <p>DISTINCT.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DISTINCT(String... columns) {
        return keyword("DISTINCT").FIELD(columns);
    }

    /**
     * <p>DISTINCT_ON.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DISTINCT_ON(String... columns) {
        keyword("DISTINCT ON");
        openParen();
        FIELD(columns);
        closeParen();
        lastCall = "DISTINCT_ON";
        return this;
    }

    /**
     * <p>EQUAL.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EQUAL() {
        return keyword("=");
    }

    /**
     * <p>EQUAL.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EQUAL(Object value) {
        return EQUAL().VALUE(value);
    }

    /**
     * <p>EQUAL_TO.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EQUAL_TO(Object value) {
        return EQUAL().VALUE(value);
    }

    /**
     * <p>EQUAL_TO_FIELD.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EQUAL_TO_FIELD(String column) {
        return EQUAL().FIELD(column);
    }

    /**
     * <p>EXCEPT.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EXCEPT(SQL sql) {
        return keyword("EXCEPT").FIELD(sql);
    }

    /**
     * <p>EXCEPT_ALL.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EXCEPT_ALL(SQL sql) {
        return keyword("EXCEPT ALL").FIELD(sql);
    }

    /**
     * <p>EXIST.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL EXIST(SQL sql) {
        keyword("EXIST");
        openParen();
        FIELD(sql);
        closeParen();
        return this;
    }

    private SQL field(SQL sql) {
        keywords.add(sql);
        return this;
    }

    /**
     * Wraps with open and close parenthesis the SQL
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FIELD(SQL sql) {
        smartCommaFnField();
        openParen();
        field(sql);
        closeParen();
        lastCall = FIELD;
        return this;
    }

    /**
     * <p>FIELD.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FIELD(String keyword) {
        smartCommaFnField();
        keyword(keyword);
        lastCall = FIELD;
        return this;
    }

    /**
     * <p>FIELD.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FIELD(String... columns) {
        for (String col : columns) {
            FIELD(col);
        }
        return this;
    }

    /**
     * Does not wraps the SQL statements with parenthesis, to avoid unnecessary characters on the query
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FN(SQL sql) {
        smartCommaFnField();
        field(sql);
        lastCall = FUNCTION;
        return this;
    }

    /**
     * <p>FOR.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FOR() {
        return keyword("FOR");
    }

    /**
     * <p>FOREIGN_KEY.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FOREIGN_KEY(String... columns) {
        return keyword("FOREIGN KEY")
                .openParen()
                .FIELD(columns)
                .closeParen()
                .ln();
    }

    /**
     * <p>FROM.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FROM() {
        return keyword("\n FROM");
    }

    /**
     * <p>FROM.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FROM(SQL sql) {
        return FROM().FIELD(sql);
    }

    /**
     * <p>FROM.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FROM(String table) {
        FROM().keyword(table);
        lastCall = TABLE;
        return this;
    }

    /**
     * <p>FROM.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @param otherTables a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FROM(String table, String... otherTables) {
        tableList.add(table);
        Collections.addAll(tableList, otherTables);
        return FROM(tableList.toArray(new String[tableList.size()]));
    }

    /**
     * <p>FROM.</p>
     *
     * @param tables an array of {@link java.lang.String} objects.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FROM(String[] tables) {
        FROM();
        for (String tbl : tables) {
            table(tbl);
        }
        return this;
    }

    /**
     * <p>FULL_OUTER_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FULL_OUTER_JOIN(String table) {
        return keyword("FULL OUTER JOIN").FIELD(table).ln();
    }

    /**
     * <p>function.</p>
     *
     * @param function a {@link java.lang.String} object.
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL function(String function, SQL sql) {
        keyword(function);
        lastCall = FUNCTION;
        openParen();
        field(sql);
        closeParen();
        lastCall = FUNCTION;
        return this;
    }

    /**
     * <p>function.</p>
     *
     * @param function a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL function(String function, String column) {
        keyword(function);
        lastCall = FUNCTION;
        openParen();
        FIELD(column);
        closeParen();
        lastCall = FUNCTION;
        return this;
    }

    /**
     * <p>FUNCTION.</p>
     *
     * @param function a {@link java.lang.String} object.
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FUNCTION(String function, SQL sql) {
        return function(function, sql);
    }

    /**
     * <p>FUNCTION.</p>
     *
     * @param function a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL FUNCTION(String function, String column) {
        return function(function, column);
    }

    /**
     * <p>GREATER_THAN.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL GREATER_THAN(Object value) {
        return keyword(">").VALUE(value);
    }

    /**
     * <p>GREATER_THAN.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL GREATER_THAN(SQL sql) {
        return keyword(">").FIELD(sql);
    }

    /**
     * <p>GREATER_THAN_OR_EQUAL.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL GREATER_THAN_OR_EQUAL(Object value) {
        return keyword(">=").VALUE(value);
    }

    /**
     * <p>GROUP_BY.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL GROUP_BY(SQL sql) {
        return keyword("GROUP BY").FN(sql);
    }

    /**
     * <p>GROUP_BY.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL GROUP_BY(String... column) {
        return keyword("GROUP BY").FIELD(column);
    }

    /**
     * As much as possible, don't use this
     *
     * @param column1 a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL HAVING(String column1) {
        return keyword("HAVING").FIELD(column1);
    }

    /**
     * <p>IF.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IF() {
        return keyword("IF");
    }

    /**
     * <p>IF_EXISTS.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IF_EXISTS() {
        return keyword("IF EXISTS").ln();
    }

    /**
     * <p>IF_NOT_EXIST.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IF_NOT_EXIST() {
        return keyword("IF NOT EXIST");
    }

    /**
     * <p>IN.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IN(Object... value) {
        keyword("IN");
        openParen();
        boolean doComma = false;
        for (Object v : value) {
            if (doComma) {
                comma();
            } else {
                doComma = true;
            }
            VALUE(v);
        }
        closeParen();
        return this;
    }

    /**
     * <p>IN.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IN(SQL sql) {
        return keyword("\n IN").FIELD(sql);
    }

    /**
     * <p>INDEX.</p>
     *
     * @param indexName a {@link java.lang.String} object.
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INDEX(String indexName, String columns) {
        return keyword("INDEX").keyword(indexName).FIELD(columns);
    }

    /**
     * <p>INHERITS.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INHERITS(String table) {
        return keyword("INHERITS").openParen().FIELD(table).closeParen();
    }

    /**
     * <p>INNER_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INNER_JOIN(String table) {
        return keyword("INNER JOIN").FIELD(table).ln();
    }

    /**
     * <p>INTERSECT.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INTERSECT(SQL sql) {
        return keyword("INTERSECT").field(sql);
    }

    /**
     * <p>INTERSECT_ALL.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INTERSECT_ALL(SQL sql) {
        return keyword("INTERSECT ALL").FIELD(sql);
    }

    /**
     * <p>INTO.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INTO(String table) {
        keyword("INTO").FIELD(table);
        lastCall = TABLE;
        return this;
    }

    /**
     * <p>IS_NOT_NULL.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IS_NOT_NULL() {
        return keyword("IS NOT NULL");
    }

    /**
     * <p>IS_NULL.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL IS_NULL() {
        return keyword("IS NULL");
    }

    /**
     * <p>keyword.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL keyword(String keyword) {
        keywords.add(keyword);
        lastCall = KEYWORD;
        return this;
    }

    /**
     * <p>LEFT_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LEFT_JOIN(String table) {
        return keyword("\n LEFT JOIN").FIELD(table).ln();
    }

    /**
     * <p>LEFT_OUTER_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LEFT_OUTER_JOIN(String table) {
        return keyword("\n LEFT OUTER JOIN").FIELD(table).ln();
    }

    /**
     * <p>LESS_THAN.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LESS_THAN(Object value) {
        return keyword("<").VALUE(value);
    }

    /**
     * <p>LESS_THAN.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LESS_THAN(SQL sql) {
        return keyword("<").FIELD(sql);
    }

    /**
     * <p>LESS_THAN_OR_EQUAL.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LESS_THAN_OR_EQUAL(Object value) {
        return keyword("<").EQUAL().VALUE(value);
    }

    /**
     * <p>LIMIT.</p>
     *
     * @param limit a int.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL LIMIT(int limit) {
        return keyword("LIMIT").keyword(limit + "");
    }

    /**
     * <p>ln.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ln() {
//		return keyword("\n");
        return this;
    }

    /**
     * <p>MATCH_FULL.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL MATCH_FULL() {
        return keyword("MATCH FULL").ln();
    }

    /**
     * <p>MATCH_SIMPLE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL MATCH_SIMPLE() {
        return keyword("MATCH SIMPLE").ln();
    }

    /**
     * <p>NOT_EQUAL_TO.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL NOT_EQUAL_TO(Object value) {
        return keyword("!=").VALUE(value);
    }

    /**
     * <p>NOT_EQUAL_TO_FIELD.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL NOT_EQUAL_TO_FIELD(String column) {
        return keyword("!=").FIELD(column);
    }

    /**
     * <p>NOT_EXIST.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL NOT_EXIST(SQL sql) {
        keyword("NOT EXIST");
        FIELD(sql);
        return this;
    }

    /**
     * <p>NOT_IN.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL NOT_IN(Object... value) {
        keyword("NOT IN");
        openParen();
        boolean doComma = false;
        for (Object v : value) {
            if (doComma) {
                comma();
            } else {
                doComma = true;
            }
            VALUE(v);
        }
        closeParen();
        return this;
    }

    /**
     * <p>NOT_IN.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL NOT_IN(SQL sql) {
        keyword("NOT IN");
        FIELD(sql);
        return this;
    }

    /**
     * <p>NOT_NULL.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL NOT_NULL() {
        return keyword("NOT NULL");
    }

    /**
     * <p>OFFSET.</p>
     *
     * @param offset a int.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL OFFSET(int offset) {
        return keyword("OFFSET").keyword(offset + "");
    }

    /**
     * <p>ON.</p>
     *
     * @param column1 a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ON(String column1) {
        return keyword("ON").FIELD(column1);
    }

    /**
     * <p>ON.</p>
     *
     * @param column1 a {@link java.lang.String} object.
     * @param column2 a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ON(String column1, String column2) {
        return keyword("ON").FIELD(column1).EQUAL().FIELD(column2).ln();
    }

    /**
     * <p>ON_DELETE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ON_DELETE() {
        return keyword("ON DELETE").ln();
    }

    /**
     * <p>ON_UPDATE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ON_UPDATE() {
        return keyword("ON UPDATE").ln();
    }

    /**
     * <p>ONLY.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ONLY() {
        return keyword("ONLY");
    }

    /**
     * <p>openParen.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL openParen() {
        lastCall = "_OPEN_PAREN_";
        return chars("(");
    }

    /**
     * <p>OR.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL OR(String column) {
        return keyword("OR").FIELD(column);

    }

    /**
     * <p>ORDER_BY.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ORDER_BY() {
        return keyword("ORDER BY");
    }

    /**
     * <p>ORDER_BY.</p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL ORDER_BY(String... field) {
        ORDER_BY();
        for (String f : field) {
            FIELD(f);
        }
        return this;
    }

    /**
     * <p>PARTITION_BY.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL PARTITION_BY(String... columns) {
        return keyword("PARTITION BY").FIELD(columns);
    }

    /**
     * <p>PRIMARY_KEY.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL PRIMARY_KEY() {
        return keyword("PRIMARY KEY");
    }

    /**
     * <p>PRIMARY_KEY.</p>
     *
     * @param columns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL PRIMARY_KEY(String... columns) {
        return PRIMARY_KEY().openParen().FIELD(columns).closeParen().ln();
    }

    /**
     * <p>REFERENCES.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL REFERENCES(String table, String column) {
        return keyword("REFERENCES").ln()
                .FIELD(table)
                .openParen().FIELD(column).ln().closeParen();
    }

    /**
     * <p>RENAME.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RENAME() {
        return keyword("RENAME");
    }

    /**
     * <p>RENAME_TO.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RENAME_TO(String table) {
        return RENAME().TO(table);
    }

    /**
     * <p>RETURNING.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RETURNING(String column) {
        return keyword("RETURNING").FIELD(column);
    }

    /**
     * <p>RIGHT_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RIGHT_JOIN(String table) {
        return keyword("RIGHT JOIN").FIELD(table).ln();
    }

    /**
     * <p>RIGHT_OUTER_JOIN.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL RIGHT_OUTER_JOIN(String table) {
        return keyword("RIGHT OUTER JOIN").FIELD(table).ln();
    }

    /**
     * <p>SCHEMA.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SCHEMA() {
        return keyword("SCHEMA").ln();
    }

    /**
     * <p>SCHEMA.</p>
     *
     * @param schema a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SCHEMA(String schema) {
        return SCHEMA().FIELD(schema);
    }

    /**
     * <p>SET.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SET() {
        return keyword("SET");
    }

    /**
     * <p>SET.</p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SET(String field) {
        return SET().FIELD(field);
    }

    /**
     * <p>SET.</p>
     *
     * @param field a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL SET(String field, Object value) {
        return SET().FIELD(field).EQUAL(value);
    }

    private SQL smartCommaFnField() {
        if (smartMode && lastCall != null && (lastCall.equals(FIELD) || lastCall.equals(FUNCTION))) {
            comma();
        }
        return this;
    }

    /**
     * <p>tab.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL tab() {
        tabs++;
        for (int i = 0; i < tabs; i++) {
            keyword("\t");
        }
        return this;
    }

    private SQL table(String tbl) {
        if (smartMode && lastCall != null && lastCall.equals(TABLE)) {
            comma();
        }
        this.keywords.add(tbl);
        lastCall = TABLE;
        return this;
    }

    /**
     * <p>TABLE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL TABLE(String table) {
        return keyword("TABLE").FIELD(table);
    }

    /**
     * <p>TABLE.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @param otherTables a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL TABLE(String table, String... otherTables) {
        keyword("TABLE");
        FIELD(table);
        lastCall = TABLE;
        for (String o : otherTables) {
            FIELD(o);
        }
        return this;
    }

    /**
     * <p>TEMPORARY.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL TEMPORARY() {
        return keyword("TEMPORARY");
    }

    /**
     * <p>THEN.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL THEN() {
        return keyword("THEN");
    }

    /**
     * <p>TO.</p>
     *
     * @param table a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL TO(String table) {
        return keyword("TO").TABLE(table);
    }

    /**
     * <p>UNION.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UNION(SQL sql) {
        return keyword("UNION").field(sql);
    }

    /**
     * <p>UNION_ALL.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UNION_ALL(SQL sql) {
        return keyword("UNION ALL").FIELD(sql);
    }

    /**
     * <p>UNIQUE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UNIQUE() {
        return keyword("UNIQUE");
    }

    /**
     * <p>UNIQUE.</p>
     *
     * @param uniqueColumns a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL UNIQUE(String... uniqueColumns) {
        return keyword("UNIQUE").openParen().FIELD(uniqueColumns).closeParen();
    }

    /**
     * <p>USING.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL USING(String... column) {
        return keyword("USING").FIELD(column);
    }

    /**
     * <p>VALUE.</p>
     *
     * @param value a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL VALUE(Object value) {
        FIELD("?");
        values.add(value);
        lastCall = VALUE;
        return this;
    }

    /**
     * <p>VALUE.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL VALUE(SQL sql) {
        return FIELD(sql);
    }

    /**
     * <p>VALUES.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL VALUES() {
        return keyword("VALUES");
    }

    /**
     * <p>VALUES.</p>
     *
     * @param objValue a {@link java.lang.Object} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL VALUES(Object... objValue) {
        VALUES().openParen();
        Iterator<Object> it = Arrays.asList(objValue).iterator();
        while (it.hasNext()) {
            Object val = it.next();
            VALUE(val);
            if (it.hasNext()) {
                comma();
            }
        }
        return closeParen();
    }

    /**
     * <p>WHEN.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WHEN() {
        return keyword("WHEN");
    }

    /**
     * <p>WHERE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WHERE() {
        return keyword("\n WHERE");
    }

    /**
     * <p>WHERE.</p>
     *
     * @param sql a {@link com.ivanceras.fluent.sql.SQL} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WHERE(SQL sql) {
        return WHERE().FIELD(sql);
    }

    /**
     * <p>WHERE.</p>
     *
     * @param column a {@link java.lang.String} object.
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL WHERE(String column) {
        return WHERE().FIELD(column);
    }

    /**
     * <p>DEFERRABLE.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL DEFERRABLE() {
        return keyword("\n DEFERRABLE");
    }

    /**
     * <p>INITIALLY_DEFERRED.</p>
     *
     * @return a {@link com.ivanceras.fluent.sql.SQL} object.
     */
    public SQL INITIALLY_DEFERRED() {
        return keyword("INITIALLY DEFERRED");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Object keyword : keywords) {
            sb.append(keyword).append(' ');
        }
        return sb.toString();
    }

    /**
     * Provides a convenient way to use Static methods such as SELECTs and Aggreate Functions i.e. min,max,lower,upper
     *
     * @author lee
     */
    public static class Statics {

        public static SQL instance() {
            return new SQL();
        }

        public static SQL WITH() {
            return instance().WITH();
        }

        public static SQL OVER() {
            return instance().OVER();
        }

        public static SQL WITH(String queryName) {
            return instance().WITH(queryName);
        }

        public static SQL RECURSIVE() {
            return instance().RECURSIVE();
        }

        public static SQL WITH(String queryName, SQL sql) {
            return instance().WITH(queryName, sql);
        }

        public static SQL WITH_RECURSIVE(String queryName) {
            return instance().WITH_RECURSIVE(queryName);
        }

        public static SQL WITH_RECURSIVE(String queryName, SQL sql) {
            return instance().WITH_RECURSIVE(queryName, sql);
        }

        public static SQL ALTER() {
            return instance().ALTER();
        }

        public static SQL ALTER_TABLE(String table) {
            return instance().ALTER_TABLE(table);
        }

        public static SQL AVG(SQL sql) {
            return instance().AVG(sql);
        }

        public static SQL AVG(String column) {
            return instance().AVG(column);
        }

        public static SQL COUNT(SQL sql) {
            return instance().COUNT(sql);
        }

        public static SQL COUNT(String column) {
            return instance().COUNT(column);
        }

        public static SQL CREATE() {
            return instance().CREATE();
        }

        public static SQL CREATE_TABLE(String table) {
            return instance().CREATE_TABLE(table);
        }

        public static SQL DELETE() {
            return instance().DELETE();
        }

        public static SQL DELETE_FROM(String table) {
            return instance().DELETE_FROM(table);
        }

        public static SQL DROP() {
            return instance().DROP();
        }

        public static SQL DROP_TABLE() {
            return instance().DROP_TABLE();
        }

        public static SQL DROP_TABLE(String table) {
            return instance().DROP_TABLE(table);
        }

        public static SQL INSERT() {
            return instance().INSERT();
        }


        public static SQL LOWER(SQL sql) {
            return instance().LOWER(sql);
        }

        public static SQL LOWER(String column) {
            return instance().LOWER(column);
        }

        public static SQL MAX(SQL sql) {
            return instance().MAX(sql);
        }

        public static SQL MAX(String column) {
            return instance().MAX(column);
        }

        public static SQL MIN(SQL sql) {
            return instance().MIN(sql);
        }

        public static SQL MIN(String column) {
            return instance().MIN(column);
        }

        public static SQL SELECT() {
            return instance().SELECT();
        }

        public static SQL SELECT(SQL arg) {
            return instance().SELECT(arg);
        }

        public static SQL SELECT(String... columns) {
            return instance().SELECT(columns);
        }

        public static SQL SUM(SQL sql) {
            return instance().SUM(sql);
        }


        public static SQL SUM(String column) {
            return instance().SUM(column);
        }

        public static SQL TRUNCATE_TABLE(String table) {
            return instance().TRUNCATE_TABLE(table);
        }

        public static SQL UPDATE(String table) {
            return instance().UPDATE(table);
        }

        public static SQL UPPER(SQL sql) {
            return instance().UPPER(sql);
        }

        public static SQL UPPER(String column) {
            return instance().UPPER(column);
        }
    }


}
