package com.netyaroze.jsiocons;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import com.netyaroze.jsiocons.format.EcoffFile;
import com.netyaroze.jsiocons.format.EcoffFileHeader;
import com.netyaroze.jsiocons.format.EcoffOptionalHeader;
import com.netyaroze.jsiocons.format.EcoffSection;
import com.netyaroze.jsiocons.format.EcoffSectionHeader;

public class ExeLoader {
	
	public EcoffFile read(String path) throws FileNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(path)) {
			final EcoffFile file = new EcoffFile();
			final EcoffFileHeader fileHeader = readEcoffFileHeader(fis.getChannel().map(MapMode.READ_ONLY, 0, EcoffFileHeader.ECOFF_FILE_HEADER_SIZE));
			file.setHeader(fileHeader);
			
			if (!fileHeader.isExecutable()) {
				throw new IOException("File is not an executable");
			}
			
			file.setOptionalHeader(readEcoffOptionalHeader(fis.getChannel().map(MapMode.READ_ONLY,
					EcoffFileHeader.ECOFF_FILE_HEADER_SIZE, EcoffOptionalHeader.OPTIONAL_HEADER_SIZE), fileHeader.getOptionalHeaderSize()));
			file.setSections(readEcoffSectionHeaders(fis, fileHeader.getNumberOfSections()));
			return file;
		}
	}

	private static List<EcoffSection> readEcoffSectionHeaders(FileInputStream fis, short numberOfSections) throws IOException {
		final MappedByteBuffer map = fis.getChannel().map(MapMode.READ_ONLY,
				EcoffFileHeader.ECOFF_FILE_HEADER_SIZE + EcoffOptionalHeader.OPTIONAL_HEADER_SIZE, EcoffSectionHeader.SECTION_HEADER_SIZE * numberOfSections);
		final List<EcoffSection> result = new ArrayList<>(numberOfSections);
		map.order(ByteOrder.LITTLE_ENDIAN);
		
		for (int i = 0; i < numberOfSections; i++) {
			final EcoffSection section = new EcoffSection();
			final EcoffSectionHeader sectionHeader = new EcoffSectionHeader();
			final byte[] name = new byte[EcoffSectionHeader.SECTION_NAME_LENGTH];
			map.get(name);
			sectionHeader.setName(new String(name).trim());
			sectionHeader.setPhysicalAddr(map.getInt());
			sectionHeader.setVirtualAddr(map.getInt());
			sectionHeader.setSize(map.getInt());
			sectionHeader.setDataPointer(map.getInt());
			sectionHeader.setRelocationPointer(map.getInt());
			sectionHeader.setGpHistorigramPointer(map.getInt());
			sectionHeader.setRelocationEntriesNumber(map.getShort());
			sectionHeader.setGpHistorigramEntriesNumber(map.getShort());
			sectionHeader.setFlags(map.getInt());
			
			section.setHeader(sectionHeader);
			final byte[] data = new byte[sectionHeader.getSize()];
			
			if (sectionHeader.getSize() > 0 && sectionHeader.getSize() < fis.available()) {
				fis.getChannel().map(MapMode.READ_ONLY, sectionHeader.getDataPointer(), sectionHeader.getSize()).get(data);
			}
			section.setData(data);
			
			result.add(section);
		}
		
		return result;
	}

	private static EcoffOptionalHeader readEcoffOptionalHeader(MappedByteBuffer mappedByteBuffer, short optionalHeaderSize) throws IOException {
		if (optionalHeaderSize != EcoffOptionalHeader.OPTIONAL_HEADER_SIZE) {
			throw new IOException("Optional header inconsistent");
		}
		mappedByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		final EcoffOptionalHeader optionalHeader = new EcoffOptionalHeader();
		
		optionalHeader.setMagic(mappedByteBuffer.getShort());
		optionalHeader.setVstamp(mappedByteBuffer.getShort());
		optionalHeader.setTextSize(mappedByteBuffer.getInt());
		optionalHeader.setDataSize(mappedByteBuffer.getInt());
		optionalHeader.setBssSize(mappedByteBuffer.getInt());
		optionalHeader.setEntry(mappedByteBuffer.getInt());
		optionalHeader.setTextStart(mappedByteBuffer.getInt());
		optionalHeader.setDataStart(mappedByteBuffer.getInt());
		optionalHeader.setBssStart(mappedByteBuffer.getInt());
		optionalHeader.setGprMask(mappedByteBuffer.getInt());
		
		final int[] cprMask = new int[EcoffOptionalHeader.CPR_MASK_LENGTH];
		for (int i = 0; i < EcoffOptionalHeader.CPR_MASK_LENGTH; i++) {
			cprMask[i] = mappedByteBuffer.getInt(); 
		}
		optionalHeader.setCprMask(cprMask);
		optionalHeader.setGpValue(mappedByteBuffer.getInt());
		
		return optionalHeader;
	}

	private static EcoffFileHeader readEcoffFileHeader(MappedByteBuffer mappedByteBuffer) throws IOException {
		final EcoffFileHeader fileHeader = new EcoffFileHeader();
		mappedByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		fileHeader.setMagic(mappedByteBuffer.getShort());
		fileHeader.setNumberOfSections(mappedByteBuffer.getShort());
		fileHeader.setDatetime(mappedByteBuffer.getInt());
		fileHeader.setSymbolicHeaderPointer(mappedByteBuffer.getInt());
		fileHeader.setSymbolicHeaderSize(mappedByteBuffer.getInt());
		fileHeader.setOptionalHeaderSize(mappedByteBuffer.getShort());
		fileHeader.setFlags(mappedByteBuffer.getShort());
		
		return fileHeader;
	}

}
