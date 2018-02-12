package mhTcp;

public class ProgressLogEvent {

	private Host host;
	private int chunks;
	private String filename;
	private int ordinalNumber;
	private int numberOfHosts;
	private boolean multiPull;
	
	private final static String splitter = " # ";
	
	public ProgressLogEvent(String line) {
		String [] words = line.split(splitter);

		host = new Host (Integer.parseInt(words[0]));
		chunks = Integer.parseInt(words[1]);
		filename = words[2];
		
		ordinalNumber = Integer.parseInt(words[3]);
		numberOfHosts = Integer.parseInt(words[4]);
		multiPull = Boolean.parseBoolean(words[5]);
		

	}
	
	public ProgressLogEvent (Host host, String filename, int numberOfHosts) {
		this.host = host;
		this.chunks = 0;
		this.filename = filename;
		this.ordinalNumber = -1;
		this.numberOfHosts = numberOfHosts;
		this.multiPull = true;
	}
	

	public ProgressLogEvent(Host host, int chunks, String filename, int ordinalNumber, int numberOfHosts,
			boolean multiPull) {
		super();
		this.host = host;
		this.chunks = chunks;
		this.filename = filename;
		this.ordinalNumber = ordinalNumber;
		this.numberOfHosts = numberOfHosts;
		this.multiPull = multiPull;
	}


	public Host getHost() {
		return host;
	}

	public int getChunks() {
		return chunks;
	}

	public String getFileName() {
		return filename;
	}

	public int getOrdinalNumber() {
		return ordinalNumber;
	}

	public int getNumberOfHosts() {
		return numberOfHosts;
	}
	
	public boolean isMultiPull() {
		return multiPull;
	}
	
	public boolean isPull() {
		return chunks >= 0 ? true : false;
	}
	
	public void setChunks(int chunks) {
		this.chunks = chunks;
	}
	
	@Override
	public String toString() {
		return "" + host.getHostNumber() + splitter + chunks + splitter + filename + splitter
				+ ordinalNumber + splitter + numberOfHosts + splitter + multiPull;
	}

	
	
}
