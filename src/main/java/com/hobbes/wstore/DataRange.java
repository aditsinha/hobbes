package com.hobbes.wstore;

/**
 * Represents a range of data in a file.  The start is inclusive and
 * the end is exclusive
 */
abstract class DataRange {

    private long logicalStartPosition, logicalEndPosition;

    public DataRange(long logicalStartPosition, long logicalEndPosition) {
	this.logicalStartPosition = logicalStartPosition;
	this.logicalEndPosition = logicalEndPosition;
	
    }

    public long getLogicalStartPosition() {
	return logicalStartPosition;
    }

    public long getLogicalEndPosition() {
	return logicalEndPosition;
    }

    /**
     * Copy this data range into buf, starting at buf[pos]
     */
    public abstract int getData(byte[] buf, int pos);
}