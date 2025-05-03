package com.netyaroze.jsiocons.format;

public class EcoffOptionalHeader {
	
	public static final int OPTIONAL_HEADER_SIZE = 56; 
	
	public static final int CPR_MASK_LENGTH = 4;
	
	private short magic;
	
	private short vstamp;
	
	private int textSize;
	
	private int dataSize;
	
	private int bssSize;
	
	private int entry;
	
	private int textStart;
	
	private int dataStart;
	
	private int bssStart;
	
	private int gprMask;
	
	private int cprMask[];
	
	private int gpValue;

	public short getMagic() {
		return magic;
	}

	public void setMagic(short magic) {
		this.magic = magic;
	}

	public short getVstamp() {
		return vstamp;
	}

	public void setVstamp(short vstamp) {
		this.vstamp = vstamp;
	}

	public int getTextSize() {
		return textSize;
	}

	public void setTextSize(int textSize) {
		this.textSize = textSize;
	}

	public int getDataSize() {
		return dataSize;
	}

	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}

	public int getBssSize() {
		return bssSize;
	}

	public void setBssSize(int bssSize) {
		this.bssSize = bssSize;
	}

	public int getEntry() {
		return entry;
	}

	public void setEntry(int entry) {
		this.entry = entry;
	}

	public int getTextStart() {
		return textStart;
	}

	public void setTextStart(int textStart) {
		this.textStart = textStart;
	}

	public int getDataStart() {
		return dataStart;
	}

	public void setDataStart(int dataStart) {
		this.dataStart = dataStart;
	}

	public int getBssStart() {
		return bssStart;
	}

	public void setBssStart(int bssStart) {
		this.bssStart = bssStart;
	}

	public int getGprMask() {
		return gprMask;
	}

	public void setGprMask(int gprMask) {
		this.gprMask = gprMask;
	}

	public int[] getCprMask() {
		return cprMask;
	}

	public void setCprMask(int[] cprMask) {
		this.cprMask = cprMask;
	}

	public int getGpValue() {
		return gpValue;
	}

	public void setGpValue(int gpValue) {
		this.gpValue = gpValue;
	}

	public static int getOptionalHeaderSize() {
		return OPTIONAL_HEADER_SIZE;
	}

	public static int getCprMaskLength() {
		return CPR_MASK_LENGTH;
	}

}
