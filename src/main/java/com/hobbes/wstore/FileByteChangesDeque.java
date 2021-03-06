package com.hobbes.wstore;
import java.util.*;
import java.io.*;

import org.apache.hadoop.fs.*;

public class FileByteChangesDeque  {

	private ArrayList<ByteArrayDataRange> deque;
	private Path dataFile;
	private long numChanges;

	public FileByteChangesDeque(Path dataFile) {
		this.dataFile = dataFile;
		this.deque = new ArrayList<ByteArrayDataRange>();
		this.numChanges = 0;
	}

	public Path getPath() {
		return dataFile;
	}

	public void print() {
		for(int i=0; i < deque.size(); i++) {
			String data = new String(deque.get(i).backing);
			System.out.println("POSITION " + Integer.toString(i) + " - START: " + Long.toString(deque.get(i).getLogicalStartPosition()) + ", END: " + Long.toString(deque.get(i).getLogicalEndPosition()) + ", BYTES: " + data);
		}
	}

	public long getNumChanges() {
		return numChanges;
	}

	public void merge(ByteArrayDataRange b1, ByteArrayDataRange b2) {
		long b1Start = b1.getLogicalStartPosition();
		long b1End = b1.getLogicalEndPosition();
		long b2Start = b2.getLogicalStartPosition();
		long b2End = b2.getLogicalEndPosition();
		int srcPos, destPos, length;
		srcPos = destPos = 0;

		/* First case: b1 is [===========================]
					   b2 is        [===========] */
		if(b1Start <= b2Start  &&
		   b1End >= b2End) {
		   	byte[] buf = new byte[(int)(b1.getLogicalEndPosition() - b1.getLogicalStartPosition())];

			length = (int)(b2Start-b1Start);

			System.arraycopy(b1.backing, srcPos, buf, destPos, length);

			destPos = (int)(b2Start-b1Start);
			length = b2.backing.length;

			System.arraycopy(b2.backing, srcPos, buf, destPos, length);
			
			srcPos = destPos = (int)(b2End-b1Start);
			length = (int)(b1End-b2End);

			System.arraycopy(b1.backing, srcPos, buf, destPos, length);
			b1.setBacking(buf);

		/* Second case: b1 is [=========================]
						b2 is              [=================] */
		} else if(b1Start <= b2Start && 
				  b1End <= b2End) {
			b1.setLogicalEndPosition(b2End);
			byte[] buf = new byte[(int)(b1.getLogicalEndPosition() - b1.getLogicalStartPosition())];

			length = (int)(b2Start-b1Start);

			System.arraycopy(b1.backing, srcPos, buf, destPos, length);

			destPos = (int)(b2Start-b1Start);
			length = b2.backing.length;

			System.arraycopy(b2.backing, srcPos, buf, destPos, length);
			b1.setBacking(buf);

		/* Third case: b1 is           [==================]
					   b2 is [================] */
		} else if(b1Start >= b2Start && 
				  b1End >= b2End) {
			b1.setLogicalStartPosition(b2Start);
			byte[] buf = new byte[(int)(b1.getLogicalEndPosition() - b1.getLogicalStartPosition())];

			length = b2.backing.length;

			System.arraycopy(b2.backing, srcPos, buf, destPos, length);

			destPos = b2.backing.length;
			srcPos = (int)(b2End-b1Start);
			length = (int)(b1End-b2End);

			System.arraycopy(b1.backing, srcPos, buf, destPos, length);
			b1.setBacking(buf);
		/* Fourth case: b1 is        [=============]
						b2 is   [=========================] */
		} else if(b1Start >= b2Start && 
				  b1End <= b2End) {
			b1.setLogicalStartPosition(b2Start);
			b1.setLogicalEndPosition(b2End);
			b1.setBacking(b2.backing);
		}
	}


	/* Return the index in the deque that has the largest logicalStartPostiion < start
	 * Return -1 if deque is empty
	 */
	public int search(long start) {
		int size = deque.size();
		
		if(size == 0) {
			return -1;
		}

		int low, mid, high, ret;
		low = mid = ret = 0;
        high = size-1;

		while (low <= high) {
            mid = low + (high - low) / 2;
            if      (start < deque.get(mid).getLogicalStartPosition()) high = mid - 1;
            else if (start > deque.get(mid).getLogicalStartPosition()) {
				ret = mid;
				low = mid+1;
			}
			else return mid;
        }

		return ret;
	}

	public void add(ByteArrayDataRange b) {

		int mid = search(b.getLogicalStartPosition());

		// this overestimates the # bytes changed 
		numChanges += b.backing.length;

		if(mid  == -1) {
			deque.add(b);
			return;
		}

		ByteArrayDataRange curr = deque.get(mid);
		if (curr.getLogicalStartPosition() > b.getLogicalEndPosition()) {
			deque.add(mid, b);
		} else if(curr.getLogicalEndPosition() < b.getLogicalStartPosition()) {
			if(mid+1 >= deque.size() || deque.get(mid+1).getLogicalStartPosition() > b.getLogicalEndPosition()) {
				deque.add(mid+1,b);
			} else {
				curr = deque.get(mid+1);
				merge(curr, b);
				mid++;
				ByteArrayDataRange next;
				next = mid+1 < deque.size() && curr.getLogicalEndPosition() >= deque.get(mid+1).getLogicalStartPosition() ? deque.remove(mid+1) : null;
				while(next != null) {
					merge(next, curr);
					next = mid+1 < deque.size() && curr.getLogicalEndPosition() >= deque.get(mid+1).getLogicalStartPosition() ? deque.remove(mid+1) : null;
				}
			}
		} else {
			merge(curr, b);
			ByteArrayDataRange prev, next;

			prev = mid-1 > 0 && curr.getLogicalStartPosition() <= deque.get(mid-1).getLogicalEndPosition() ? deque.remove(mid-1) : null;
			while(prev != null) {
				merge(prev, curr);
				mid--;
				prev = mid-1 > 0 && curr.getLogicalStartPosition() <= deque.get(mid-1).getLogicalEndPosition() ? deque.remove(mid-1) : null;
			}

			next = mid+1 < deque.size() && curr.getLogicalEndPosition() >= deque.get(mid+1).getLogicalStartPosition() ? deque.remove(mid+1) : null;
			while(next != null) {
				merge(next, curr);
				next = mid+1 < deque.size() && curr.getLogicalEndPosition() >= deque.get(mid+1).getLogicalStartPosition() ? deque.remove(mid+1) : null;
			}
		}
	}

	public ArrayList<ByteArrayDataRange> getDeque() {
		return deque;
	}

	public void clearDeque() {
		deque = new ArrayList<ByteArrayDataRange>();
	}

	public boolean isEmpty() {
		return deque.size() == 0;
	}



	public static void main(String[] args) {
		FileByteChangesDeque d = new FileByteChangesDeque(null);

		String s1 = "hello";
		byte[] backing1 = s1.getBytes();
		int size1 = s1.length();

		String s2 = "foo";
		byte[] backing2 = s2.getBytes();
		int size2 = s2.length();

		String s3 = "anteater";
		byte[] backing3 = s3.getBytes();
		int size3 = s3.length();

		ByteArrayDataRange b1 = new ByteArrayDataRange(0, size1, backing1);
		ByteArrayDataRange b2 = new ByteArrayDataRange(size1+1, size1+1+size2, backing2);
		ByteArrayDataRange b3 = new ByteArrayDataRange(100, 100+size3, backing3);
		
		d.add(b1);
		d.add(b2);
		d.add(b3);

		d.print();
	}
}
