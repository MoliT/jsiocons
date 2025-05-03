package com.netyaroze.jsiocons.format;

import java.util.List;

public class EcoffFile {

	private EcoffFileHeader header;
	
	private EcoffOptionalHeader optionalHeader;
	
	private List<EcoffSection> sections;

	public EcoffFileHeader getHeader() {
		return header;
	}

	public void setHeader(EcoffFileHeader header) {
		this.header = header;
	}

	public EcoffOptionalHeader getOptionalHeader() {
		return optionalHeader;
	}

	public void setOptionalHeader(EcoffOptionalHeader optionalHeader) {
		this.optionalHeader = optionalHeader;
	}

	public List<EcoffSection> getSections() {
		return sections;
	}

	public void setSections(List<EcoffSection> sections) {
		this.sections = sections;
	}
	
	
	
}
