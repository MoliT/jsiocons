package com.netyaroze.jsiocons.format;

public class EcoffSectionHeader {
	public static final int SECTION_HEADER_SIZE = 40;
	
	public static final int SECTION_NAME_LENGTH = 8;
	
	private String name;	/* section name */

	private int physicalAddr;	/* physical address, aliased s_nlib */

	private int	virtualAddr;	/* virtual address */

	private int size;		/* section size */

	private int dataPointer;	/* file ptr to raw data for section */

	private int relocationPointer;	/* file ptr to relocation */

	private int gpHistorigramPointer;	/* file ptr to gp histogram */

	private short relocationEntriesNumber;	/* number of relocation entries */

	private short gpHistorigramEntriesNumber;	/* number of gp histogram entries */

	private int flags;	/* flags */

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPhysicalAddr() {
		return physicalAddr;
	}

	public void setPhysicalAddr(int physicalAddr) {
		this.physicalAddr = physicalAddr;
	}

	public int getVirtualAddr() {
		return virtualAddr;
	}

	public void setVirtualAddr(int virtualAddr) {
		this.virtualAddr = virtualAddr;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getDataPointer() {
		return dataPointer;
	}

	public void setDataPointer(int dataPointer) {
		this.dataPointer = dataPointer;
	}

	public int getRelocationPointer() {
		return relocationPointer;
	}

	public void setRelocationPointer(int relocationPointer) {
		this.relocationPointer = relocationPointer;
	}

	public int getGpHistorigramPointer() {
		return gpHistorigramPointer;
	}

	public void setGpHistorigramPointer(int gpHistorigramPointer) {
		this.gpHistorigramPointer = gpHistorigramPointer;
	}

	public short getRelocationEntriesNumber() {
		return relocationEntriesNumber;
	}

	public void setRelocationEntriesNumber(short relocationEntriesNumber) {
		this.relocationEntriesNumber = relocationEntriesNumber;
	}

	public short getGpHistorigramEntriesNumber() {
		return gpHistorigramEntriesNumber;
	}

	public void setGpHistorigramEntriesNumber(short gpHistorigramEntriesNumber) {
		this.gpHistorigramEntriesNumber = gpHistorigramEntriesNumber;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public static int getSectionNameLength() {
		return SECTION_NAME_LENGTH;
	}

	
}
