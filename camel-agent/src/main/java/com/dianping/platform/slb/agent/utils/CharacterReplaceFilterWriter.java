package com.dianping.platform.slb.agent.utils;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class CharacterReplaceFilterWriter extends FilterWriter {

	private char toFind;
	private String toReplace;

	public CharacterReplaceFilterWriter(Writer out, char toFind, String toReplace) {
		super(out);
		this.toFind = toFind;
		this.toReplace = toReplace;
	}

	@Override
	public void write(int c) throws IOException {
		if ((char) c == toFind) {
			super.write(toReplace);
		} else {
			super.write(c);
		}
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for (int i = 0; i < len; i++) {
			write(cbuf[off + i]);
		}
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		write(str.substring(off, off + len).toCharArray());
	}

	@Override
	public void flush() throws IOException {
		super.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

}
