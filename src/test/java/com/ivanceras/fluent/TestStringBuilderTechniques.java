package com.ivanceras.fluent;

import com.ivanceras.fluent.sql.Breakdown;
import org.junit.*;

import static org.junit.Assert.assertTrue;

public class TestStringBuilderTechniques {

    private int iterations = 500;

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
        long t1 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            sb.append(buildString(i));
        }
        long t2 = System.currentTimeMillis();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            buildPassString(sb2, i);
        }
        long t3 = System.currentTimeMillis();
//		System.out.println(sb);
        long first = t2 - t1;
        long second = t3 - t2;
        System.out.println("First: took " + first + " ms ");
        System.out.println("Second: took " + second + " ms ");
        //assertTrue(second < first);
    }

    @Test
    public void testBreakdown() {
        long t1 = System.currentTimeMillis();
        Breakdown bk1 = new Breakdown();
        for (int i = 0; i < iterations; i++) {
            Breakdown iterBk = buildBreakdown(i);
            bk1.append(iterBk.getSql());
            for (Object p : iterBk.getParameters()) {
                bk1.addParameter(p);
            }
        }
        long t2 = System.currentTimeMillis();
        Breakdown bk2 = new Breakdown();
        for (int i = 0; i < iterations; i++) {
            buildPassBreakdown(bk2, i);
        }
        long t3 = System.currentTimeMillis();
        long first = t2 - t1;
        long second = t3 - t2;
        System.out.println("Breakdown First: took " + first + " ms ");
        System.out.println("Breakdown Second: took " + second + " ms ");
        assertTrue(second < first);
    }

    private StringBuilder buildString(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("StringBuilder ").append(n).append(" - ").append(i).append("\n");
        }
        return sb;
    }

    private void buildPassString(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append("StringBuilder ").append(n).append(" - ").append(i).append("\n");
        }
    }

    private void buildPassBreakdown(Breakdown bk, int n) {
        for (int i = 0; i < n; i++) {
            bk.append("StringBuilder " + n + " - " + i + "\n");
            bk.addParameter(new Integer(i));
            bk.addParameter(new Integer(n));
        }
    }

    private Breakdown buildBreakdown(int n) {
        Breakdown bk = new Breakdown();
        for (int i = 0; i < n; i++) {
            bk.append("StringBuilder " + n + " - " + i + "\n");
            bk.addParameter(new Integer(i));
            bk.addParameter(new Integer(n));
        }
        return bk;
    }
}
