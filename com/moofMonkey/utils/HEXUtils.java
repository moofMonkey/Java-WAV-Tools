package com.moofMonkey.utils;

import javax.xml.bind.DatatypeConverter;

public class HEXUtils {
	public static String toHex(String str) {
		return toHex(str.getBytes());
	}
	
	public static String toHex(byte[] b) {
		return DatatypeConverter.printHexBinary(b);
	}
	
	public static String fromHex(String str) {
		return new String(DatatypeConverter.parseHexBinary(str));
	}
}