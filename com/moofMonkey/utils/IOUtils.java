package com.moofMonkey.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils implements Closeable {
	InputStream in;
	OutputStream out;

	public IOUtils(InputStream _in, OutputStream _out) {
		in = _in;
		out = _out;
	}

	public IOUtils(InputStream _in) {
		in = _in;
	}

	public IOUtils(OutputStream _out) {
		out = _out;
	}
	
	public String readString() throws Throwable {
		return new String (
			getBytes (
				4
			)
		);
	}
	
	public int readInt() throws Throwable {
		return NativeTranslate.bytes2int (
			getBytes (
				4
			)
		);
	}
	
	public short readShort() throws Throwable {
		return NativeTranslate.bytes2short (
			getBytes (
				2
			)
		);
	}
	
	public byte[] getBytes(int num) throws Throwable {
		byte[] bytes = new byte[num];
		in.read(bytes);
		
		return bytes;
	}
	
	//-----------------------------------------------------------------------------

	public void writeString(String str) throws Throwable {
		out.write (
			str.getBytes()
		);
	}
	
	public void writeInt(int i) throws Throwable {
		out.write (
			NativeTranslate.int2bytes (
				i
			)
		);
	}
	
	public void writeShort(short s) throws Throwable {
		out.write (
			NativeTranslate.short2bytes (
				s
			)
		);
	}

	@Override
	public void close() throws IOException {
		if(in != null)
			in.close();
		if(out != null)
			out.close();
	}

	//-----------------------------------------------------------------------------
	
	public InputStream getIn() {
		return in;
	}
	
	public OutputStream getOut() {
		return out;
	}
}
