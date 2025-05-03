package com.netyaroze.jsiocons;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.CommunicationException;

import jssc.SerialPort;
import jssc.SerialPortException;
import com.netyaroze.jsiocons.format.EcoffFile;
import com.netyaroze.jsiocons.format.EcoffSection;

/**
 * SIOCONS replacement in Java for Net Yaroze
 * @author Antonio Molinero
 */
public class Siocons 
{
	private static final Set<String> A_EXEC_SECTIONS = Set.of(".text", ".rdata", ".data", ".sdata");

	private static final byte BEGIN_HANDSHAKE = 0x01;
	private static final byte BEGIN_DATA = 0x02;

	private static final int PACKET_SIZE = 2048;
	private static final int PS_PACKET_SIZE = 8;
	
	private final SerialPort serialPort;
	private final int portSpeed;
	private final String batchFile;
	private boolean run;
	
	private Lock lock = new ReentrantLock();
	private Thread readingThread;
	
	private Siocons(String serialPort, int portSpeed, String batchFile) {
		this.serialPort = new SerialPort(serialPort);
		this.portSpeed = portSpeed;
		this.batchFile = batchFile;
		this.run = true;
	}
	
	private Thread createReadingThread() {
		return new Thread(() -> {
			final List<String> commands = new ArrayList<>();
			while (run) {
				String read = null;
				try {
					lock.lock();
					read = readUntil("\r");
				} catch (SerialPortException e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
				if (read != null && !read.isBlank()) {
					for(String line:read.split("\n")) {
						System.out.println(":> " + line);
						try {
							if(line.startsWith("get ")) {
								processRemoteCommand(line.substring("get ".length()), this::loadBinary);
							} else if (line.startsWith("search ")) {
								processRemoteCommand(line.substring("search ".length()), this::searchFile);
							} else if (line.startsWith("cmd ")) {
								commands.add(line.substring(4));
							} else if (line.equals("exec\r")) {
								commands.add("go");
								System.out.println("Executing commands from game.");
								sleep(5000);
								waitLivePS();
								runBatch(commands);
								commands.clear();
							} else if (batchFile!= null && !batchFile.isBlank() && checkExpectedEnds(line, ">>", ">>\r\n")) {
								run = false;
								System.out.println("Done!");
								synchronized (this) {
									this.notifyAll();
								}
							}
						} catch (SerialPortException e) {
							e.printStackTrace();
						}
					}
				} else {
					sleep(1000);
				}
			}
		}); 
	}

	public void runSiocons() {
		try {
			this.serialPort.openPort();
			this.serialPort.setParams(portSpeed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			this.serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
			readingThread = createReadingThread(); 
			readingThread.start();
			if (batchFile != null && !batchFile.isBlank()) {
				waitLivePS();
				processCommand("batch " + batchFile);
				synchronized (this) {
					this.wait();					
				}
			}
			while (run) {
				this.serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
				final BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
				final String command = console.readLine();
				processCommand(command);					
			}
		} catch (SerialPortException | IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (this.serialPort != null && this.serialPort.isOpened()) {
				try {
					this.serialPort.closePort();
				} catch (SerialPortException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void processCommand(String command) throws SerialPortException {
		try {
			lock.lock();
			if ("exit".equals(command)) {
				this.run = false;
			}else if (command != null && command.startsWith("batch ")) {
				runBatch(command);
			}else if (command != null && command.startsWith("local ")) {
				runLocalCommand(command + "\r");
			} else {
				this.serialPort.writeString(command + "\r");
				if(!command.startsWith("go")) {
					System.out.println(readUntil(true, ">>", ">>\r\n"));
				}
			}
		}finally {
			lock.unlock();				
		}
	}
	
	private void waitLivePS() throws SerialPortException {
		try {
			lock.lock();
			this.serialPort.writeString("\r");
			System.out.println(readUntil(">>", ">>\r\n"));
		} finally {
			lock.unlock();
		}
	}
	
	private void processRemoteCommand(String command, Consumer<String> loader) throws SerialPortException {
		try {
			lock.lock();
			loader.accept(command);
		}finally {
			lock.unlock();				
		}
	}
	
    private void runBatch(String command) {
		final String commandParts[] = command.split("\\s");
		if (commandParts.length > 1) {
			try (FileReader fileReader = new FileReader(commandParts[1])){
				runBatch(fileReader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
    
    private void runBatch(List<String> commands) {
    	runBatch(new StringReader(String.join("\n", commands)));
    }
    
    private void runBatch(Reader reader) {
		try (BufferedReader br = new BufferedReader(reader)) {
			br.lines().forEach(c -> {
				try {
					processCommand(c);
				} catch (SerialPortException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}    	
    }

	private void runLocalCommand(String command) throws SerialPortException {
		final String commandParts[] = command.split("\\s");
		
		if (commandParts.length > 2 && "load".equals(commandParts[1])) {
			loadExecutable(getPath(commandParts[2]));
		} else if (commandParts.length > 3 && "dload".equals(commandParts[1])) {
			loadBinary(getPath(commandParts[2]), commandParts[3]);
		}
	}

	private void loadBinary(String path) {
		try {
			lock.lock();
			loadFile(getPath(path), this::sendBinaryData);
		} finally {
			lock.unlock();
		}
	}
	
	private void loadFile(String path, Sender sender) {
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path))) {
			final byte[] data = bis.readAllBytes();
			System.out.println("Loading " + path + " - " + data.length);
			sender.send(data);
		} catch (IOException | NumberFormatException | SerialPortException e) {
			System.out.println("File '" + path + "'");
			e.printStackTrace();
		}		
	}
	
	private void searchFile(String path) {
		try {
			lock.lock();
			final String realPath = getPath(path);
			final long size = new File(realPath).length();
			this.serialPort.writeString(Long.toString(size) + "\r");
		} catch (SerialPortException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Converts path from DOS or ISO9660 format to Unix like managed by Java.
	 * All paths must be relative to working dir for emulating PSX CD files.
	 * @param path the path to fix.
	 */
	private String getPath(String path) {
		String workPath = path.replaceAll("\\\\", "/").replace(";1", "").trim();
		return workPath.startsWith("/") ? workPath.substring(1) : workPath;
	}
	
	private void loadBinary(String path, String addrStr) {
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path))) {
			final byte[] data = bis.readAllBytes();
			final int addr = getAddr(addrStr);
			System.out.println("Loading " + path + " to " + Long.toHexString(addr) + "-" + Long.toHexString(addr + data.length - 1));
			sendBinaryData(addr, data);
		} catch (IOException | NumberFormatException | SerialPortException e) {
			e.printStackTrace();
		}
	}
	
	private int getAddr(String addr) {
		if (addr.toLowerCase().startsWith("0x")) {
			return Long.decode(addr).intValue();
		}
		return Long.decode("0x" + addr).intValue();
	}

	private void loadExecutable(String path) throws SerialPortException {
		final ExeLoader loader = new ExeLoader();
		try {
			final EcoffFile file = loader.read(path);
			for (EcoffSection section:file.getSections()) {
				if (A_EXEC_SECTIONS.contains(section.getHeader().getName())) {
					System.out.println("Send section: " + section.getHeader().getName()+ " to " + Long.toHexString(section.getHeader().getPhysicalAddr()));
					sendBinaryData(section.getHeader().getPhysicalAddr(), section.getData());
				} else {
					System.out.println("Ignoring section: " + section.getHeader().getName());
				}
			}
			
			this.serialPort.writeString("sr epc "+ Integer.toHexString(file.getOptionalHeader().getEntry()) +"\r");
			System.out.println(readUntil(true, ">>", ">>\r\n"));
			this.serialPort.writeString("sr gp "+ Integer.toHexString(file.getOptionalHeader().getGpValue()) +"\r");
			System.out.println(readUntil(true, ">>", ">>\r\n"));
			this.serialPort.writeString("sr sp 801ffff0\r");
			System.out.println(readUntil(true, ">>", ">>\r\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendBinaryData(int physicalAddr, byte[] data) throws SerialPortException {
		int retries = 3;
		do {
			try {
				this.serialPort.writeString("bwr\r");
				waitUntilDataIsSent(200);
				readUntil("binary\r");
				this.serialPort.writeByte(BEGIN_HANDSHAKE);
				waitUntilDataIsSent(200);
				this.serialPort.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(physicalAddr).array());
				waitUntilDataIsSent(200);
				this.serialPort.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(data.length).array());
				waitUntilDataIsSent(200);
				for (int pos = 0; pos < data.length; pos += PACKET_SIZE) {
					sendDataPacket(ByteBuffer.wrap(data, pos, Math.min(PACKET_SIZE, data.length - pos)), PACKET_SIZE);
				}
				retries = 0;
			} catch (CommunicationException e) {
				retries--;
			}
			this.serialPort.writeByte((byte) 0x0d);
			System.out.println("Transfer complete: " + data.length + " bytes");
			readUntil(">>", ">>\r\n");
		} while(retries > 0);
	}
	
	private void sendBinaryData(byte[] data) throws SerialPortException {
		try {
			long start = System.currentTimeMillis();
			this.serialPort.writeByte(BEGIN_HANDSHAKE);
			waitUntilDataIsSent(200);
			this.serialPort.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(data.length).array());
			waitUntilDataIsSent(200);
			Queue<String> commands = null;
			String command = "";
			while (!command.startsWith("D")) {
				if (commands == null || commands.isEmpty()) {
					commands = getCommands();
				}
				
				command = commands.isEmpty()? "" : commands.poll();
				if (command.startsWith("A")) {
					throw new CommunicationException("Communication aborted");
				}
				if (command.startsWith("G")) {
					final int pos = readInt(command);
					sendDataPacket(ByteBuffer.wrap(data, pos, Math.min(PS_PACKET_SIZE, data.length - pos)));
				}
			}
			System.out.println("Transfer complete: " + data.length + " bytes - " + (System.currentTimeMillis() - start) +"ms");
			if (commands == null || commands.isEmpty() || commands.stream().noneMatch(s -> s.startsWith(">>"))) {
				readUntil(">>", ">>\r\n");
			}
		} catch (CommunicationException e) {
			System.out.println("Transfer aborted: " + data.length + " bytes");
			e.printStackTrace();
		}
	}
	
	private Queue<String> getCommands() throws SerialPortException {
		String input = readUntil(";");
		
		return new ArrayDeque<>(List.of(input.split(";")));
	}
	
	private void sendDataPacket(ByteBuffer byteBuffer) throws SerialPortException, CommunicationException {
		this.serialPort.writeByte(BEGIN_DATA);
		waitUntilDataIsSent(200);
		this.serialPort.writeByte((byte)byteBuffer.remaining());
		waitUntilDataIsSent(200);
		int sum = 0;
		while(byteBuffer.remaining() > 0) {
			final byte data = byteBuffer.get();
			this.serialPort.writeByte(data);
			sum += data;
		}
		
		waitUntilDataIsSent(200);
		this.serialPort.writeByte((byte) sum);
		waitUntilDataIsSent(10);
	}
	
	private void sendDataPacket(ByteBuffer byteBuffer, int packetSize) throws SerialPortException, CommunicationException {
		boolean ok = false;
		int retries = 3;
		byteBuffer.mark();
		do {
			this.serialPort.writeByte(BEGIN_DATA);
			waitUntilDataIsSent(200);
			int sum = 0;
			for (int pos = 0; pos < packetSize; pos++) {
				if (byteBuffer.remaining() > 0) {
					final byte data = byteBuffer.get();
					this.serialPort.writeByte(data);
					sum += data;
				} else {
					this.serialPort.writeByte((byte) 0);
				}
			}
			
			waitUntilDataIsSent(200);
			this.serialPort.writeByte((byte) sum);
			waitUntilDataIsSent(10);
			
			final String status = readUntil("Y", "N"); 
			ok = status.endsWith("Y");
			if (!ok) {
				if(checkExpectedEnds(status, new String(new byte[] {0x7D}), ">>", ">>\r\n")) {
					throw new CommunicationException("Error while transferring data."); 
				}
				retries--;
			}
			byteBuffer.reset();
		} while (!ok && retries > 0);
		
	}
	
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// Ignore
		}
	}
	
	private int readInt(String input) throws SerialPortException {
		return Integer.parseInt(input.substring(input.lastIndexOf('G') + 1));
	}
	
	private String readUntil(String...expectedEnds) throws SerialPortException {
		return readUntil(false, expectedEnds);
	}
	
	private String readUntil(boolean unlimted, String...expectedEnds) throws SerialPortException {
		String result = "";
		String read = null;
		int waits = 0;
		boolean found = false;
		while ((unlimted || waits < 3) && !found) {
			read = this.serialPort.readString();
			if (read != null) {
				result += read;
				waits = 0;
				found = checkExpectedEnds(result, expectedEnds);				
			}
			if (!found) {
				sleep(500);
			}
			waits++;
		}		
		return result;
	}
	
	private boolean checkExpectedEnds(String value, String...expectedEnds) {
		final String valueTrimmed = value.trim();
		if (expectedEnds == null) {
			return true;
		}
		
		for(String expectedEnd:expectedEnds) {
			if (value.endsWith(expectedEnd) || value.equals(expectedEnd)
					|| valueTrimmed.endsWith(expectedEnd) || valueTrimmed.equals(expectedEnd)) {
				return true;
			}
		}
		return false;
	}

	private void waitUntilDataIsSent(long millis) throws SerialPortException {
		sleep(millis);
		while(this.serialPort.getOutputBufferBytesCount() > 0) {
			sleep(millis);
		}		
	}
	
	public static void main( String[] args ) {
        System.out.println("Java SIOCONS v.1.0");
        final Map<String, String> arguments = parseArgs(args);
        final String port = arguments.get("port");
        if (port == null) {
        	System.out.println("No port specified. Exiting!!");
        	System.exit(-1);
        }
        final int portSpeed = getPortSpeed(arguments);
        final String batch = arguments.get("batch");
        System.out.println("Port: " + port);
      	new Siocons(port, portSpeed, batch).runSiocons();
    }
	
	private static int getPortSpeed(Map<String, String> arguments) {
		final String portSpeedArgument = arguments.getOrDefault("port-speed", "9600");
		
		try {
			return Integer.parseInt(portSpeedArgument);
		} catch (Exception e) {
			return SerialPort.BAUDRATE_9600;
		}
	}

	private static Map<String, String> parseArgs(String[] args) {
		if (args == null || args.length == 0) {
			return Collections.emptyMap();
		}
		
		return Stream.of(args)
			.filter(Predicate.not(String::isBlank))
			.filter(arg -> arg.startsWith("--") && arg.contains("="))
			.map(arg -> arg.substring(2).split("="))
			.collect(Collectors.toMap(arg -> arg[0], arg -> arg[1]));
	}
	
	@FunctionalInterface
	private static interface Sender {
		/**
		 * Sends data.
		 * @param data the data to send.
		 * @throws SerialPortException
		 */
		void send(byte[] data) throws SerialPortException;
	}
}
