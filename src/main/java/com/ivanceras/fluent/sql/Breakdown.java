package com.ivanceras.fluent.sql;

import java.util.LinkedList;

/**
 * <p>Breakdown class.</p>
 *
 * @author ivankol
 * @version $Id: $Id
 */
public class Breakdown {

    boolean doComma = false;
    private StringBuilder sql;
    private LinkedList<Object> parameters;

    /**
     * <p>Constructor for Breakdown.</p>
     *
     * @param sql a {@link java.lang.String} object.
     * @param parameters a {@link java.util.LinkedList} object.
     */
    public Breakdown(String sql, LinkedList<Object> parameters) {
        this.sql = new StringBuilder(sql);
        this.parameters = parameters;
    }

    /**
     * <p>Constructor for Breakdown.</p>
     */
    public Breakdown() {
        this.sql = new StringBuilder();
        this.parameters = new LinkedList<>();
    }

    /**
     * <p>Getter for the field <code>sql</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSql() {
        return sql.toString();
    }

    /**
     * <p>append.</p>
     *
     * @param sb a {@link java.lang.StringBuilder} object.
     */
    public void append(StringBuilder sb) {
        this.sql.append(sb);
    }

    /**
     * <p>append.</p>
     *
     * @param sb a {@link java.lang.String} object.
     */
    public void append(String sb) {
        this.sql.append(sb);
    }

    /**
     * <p>appendSp.</p>
     *
     * @param sb a {@link java.lang.String} object.
     */
    public void appendSp(String sb) {
        append(sb + " ");
    }


    /**
     * <p>addParameter.</p>
     *
     * @param parameter a {@link java.lang.Object} object.
     */
    public void addParameter(Object parameter) {
        this.parameters.add(parameter);
    }

    /**
     * <p>line.</p>
     */
    public void line() {
        line(0);
    }

    /**
     * <p>line.</p>
     *
     * @param tabs a int.
     */
    public void line(int tabs) {
        append("\n");
        tabs(tabs);
    }

    /**
     * <p>tabs.</p>
     *
     * @param tabs a int.
     */
    public void tabs(int tabs) {
        for (int i = 0; i < tabs; i++) {
            append("\t");
        }
    }

    /**
     * <p>Getter for the field <code>parameters</code>.</p>
     *
     * @return an array of {@link java.lang.Object} objects.
     */
    public Object[] getParameters() {
        return parameters.toArray(new Object[parameters.size()]);
    }


}
