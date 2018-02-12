package mhTcp;

public class InterruptionException extends Exception{

	private static final long serialVersionUID = -4185596284251599315L;
	
	private int chunks;
	
	public InterruptionException(int chunks) {
		this.chunks = chunks;
	}
	public int getChunks() {
		return chunks;
	}

}
