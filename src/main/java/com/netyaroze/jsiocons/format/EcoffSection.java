package com.netyaroze.jsiocons.format;

public class EcoffSection {
	
	private EcoffSectionHeader header;
	
	private byte[] data;

	public EcoffSectionHeader getHeader() {
		return header;
	}

	public void setHeader(EcoffSectionHeader header) {
		this.header = header;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	
}
