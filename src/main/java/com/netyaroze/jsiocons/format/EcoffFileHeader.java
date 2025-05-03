package com.netyaroze.jsiocons.format;

public class EcoffFileHeader {
	
	public static final short MIPSEBMAGIC = 0x0160;
	
	public static final int ECOFF_FILE_HEADER_SIZE = 20;
	
	public static final short FLAG_EXEC = 2;
	
	private short magic;	/* magic number */

	private short numberOfSections;	/* number of sections */

	private int datetime;	/* time & date stamp */

	private int symbolicHeaderPointer;	/* file pointer to symbolic header */

	private int symbolicHeaderSize;	/* sizeof(symbolic hdr) */

	private short optionalHeaderSize;	/* sizeof(optional hdr) */

	private short flags;	/* flags */

	public short getMagic() {
		return magic;
	}

	public void setMagic(short magic) {
		this.magic = magic;
	}

	public short getNumberOfSections() {
		return numberOfSections;
	}

	public void setNumberOfSections(short numberOfSections) {
		this.numberOfSections = numberOfSections;
	}

	public int getDatetime() {
		return datetime;
	}

	public void setDatetime(int datetime) {
		this.datetime = datetime;
	}

	public int getSymbolicHeaderPointer() {
		return symbolicHeaderPointer;
	}

	public void setSymbolicHeaderPointer(int symbolicHeaderPointer) {
		this.symbolicHeaderPointer = symbolicHeaderPointer;
	}

	public int getSymbolicHeaderSize() {
		return symbolicHeaderSize;
	}

	public void setSymbolicHeaderSize(int symbolicHeaderSize) {
		this.symbolicHeaderSize = symbolicHeaderSize;
	}

	public short getOptionalHeaderSize() {
		return optionalHeaderSize;
	}

	public void setOptionalHeaderSize(short optionalHeaderSize) {
		this.optionalHeaderSize = optionalHeaderSize;
	}

	public short getFlags() {
		return flags;
	}

	public void setFlags(short flags) {
		this.flags = flags;
	}

	public boolean isExecutable() {
		return (this.flags & FLAG_EXEC) == 2;
	}
	
}
