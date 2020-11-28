package com.ivanceras.fluent;

import static org.junit.Assert.assertEquals;

public class CTest {


    public static void cassertEquals(String expected, String actual) {
        String cleansedExpected = cleanUpSpaces(expected).toLowerCase();
        String cleansedActual = cleanUpSpaces(actual).toLowerCase();
        System.out.println("cleansed expected:\n" + cleansedExpected);
        System.out.println("cleansed actual: \n" + cleansedActual);
        assertEquals(cleansedExpected, cleansedActual);
    }

    public static String cleanUpSpaces(String str) {
        return str.trim().replaceAll("\\s+", " ");
    }

}
