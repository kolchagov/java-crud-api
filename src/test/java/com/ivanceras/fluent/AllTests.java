package com.ivanceras.fluent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({TestComplexQuery.class, TestQuery2HiveSQL.class,
        TestSimpleComplexFunctions.class, TestSQL1.class,
        TestSQLBuilderDelete.class, TestSQLBuilderEquality.class,
        TestSQLBuilderFunctions.class, TestSQLBuilderIn.class,
        TestSQLBuilderInsert.class, TestSQLBuilderMoreComplexFunctions.class,
        TestSQLBuilderNamedColumns.class, TestSQLBuilderRecursive.class,
        TestSQLBuilderSelect.class, TestSQLBuilderUpdate.class,
        TestSQLBuilderWithRecursive.class, TestSQLFieldsComma.class,
        TestSQLOrderBy.class, TestStaticCreate.class, TestStaticSelects.class,
        TestStringBuilderTechniques.class})
public class AllTests {

}
