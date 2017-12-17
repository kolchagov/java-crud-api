package com.ivanceras.fluent.sql;

import java.util.LinkedList;

public class Breakdown {

    boolean doComma = false;
    private StringBuilder sql;
    private LinkedList<Object> parameters;

    public Breakdown(String sql, LinkedList<Object> parameters) {
        this.sql = new StringBuilder(sql);
        this.parameters = parameters;
    }

    public Breakdown() {
        this.sql = new StringBuilder();
        this.parameters = new LinkedList<Object>();
    }

    public String getSql() {
        return sql.toString();
    }

    public void append(StringBuilder sb) {
        this.sql.append(sb);
    }

    public void append(String sb) {
        this.sql.append(sb);
    }

    public void appendSp(String sb) {
        append(sb + " ");
    }


    public void addParameter(Object parameter) {
        this.parameters.add(parameter);
    }

    public void line() {
        line(0);
    }

    public void line(int tabs) {
        append("\n");
        tabs(tabs);
    }

    public void tabs(int tabs) {
        for (int i = 0; i < tabs; i++) {
            append("\t");
        }
    }

    public Object[] getParameters() {
        return parameters.toArray(new Object[parameters.size()]);
    }


}
