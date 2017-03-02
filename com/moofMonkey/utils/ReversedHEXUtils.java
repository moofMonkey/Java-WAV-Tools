package com.moofMonkey.utils;

import javax.xml.bind.DatatypeConverter;

/**
 * Utils that allows using of big-endian
*/
public class ReversedHEXUtils {
	public static String toHex(String str) {
		return toHex(str.getBytes());
	}
	
	public static String toHex(byte[] b) {
		return DatatypeConverter.printHexBinary(reverse(b));
	}
	
	public static byte[] fromHex(String str) {
		return DatatypeConverter.parseHexBinary(new String(reverseHEX(str.getBytes())));
	}
	
	public static byte[] reverseHEX(byte[] b) {
		byte temp;
		
		for(int i = 1; i < b.length; i = i + 2) {
			temp = b[i];

			b[i] = b[i - 1];
			b[i - 1] = temp;
		}
		
		return b;
	}
	
	public static byte[] reverse(byte[] b) {
		return new StringBuilder (
			new String (
				b
			)
		).reverse().toString().getBytes();
	}
}
