package com.hobbes.wstore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.hadoop.fs.*;
import java.io.*;
import java.util.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    private static final int ONE_KB = 1024;
    private static final int ONE_MB = ONE_KB * ONE_KB;

    private static byte[] getBlock(byte val, int size) {
	byte[] ret = new byte[size];
	Arrays.fill(ret, val);
	return ret;
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() throws Exception
    {



    }

}
