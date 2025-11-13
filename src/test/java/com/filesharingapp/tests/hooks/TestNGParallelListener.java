package com.filesharingapp.tests.hooks;

import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import java.util.List;

/**
 * TestNGParallelListener
 * ----------------------
 * This tells TestNG to run tests in parallel.
 *
 * Here we choose:
 * - Parallel mode: TESTS
 * - Thread count: 4
 */
public class TestNGParallelListener implements IAlterSuiteListener {

    @Override
    public void alter(List<XmlSuite> suites) {
        for (XmlSuite suite : suites) {
            suite.setParallel(XmlSuite.ParallelMode.TESTS);
            suite.setThreadCount(4);
        }
    }
}
