package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RetransmisionSocket extends QuestioningSocket {
	private int chunks;
	private String filename;
	private int ordinalNumber;
	private int numberOfHosts;
	private boolean multiPull;

	public RetransmisionSocket(LoggingHost thisHost, ProgressLogEvent retransmision) {
		super(thisHost, retransmision.getHost());
		this.chunks = retransmision.getChunks();
		this.filename = retransmision.getFileName();
		this.ordinalNumber = retransmision.getOrdinalNumber();
		this.numberOfHosts = retransmision.getNumberOfHosts();
		this.multiPull = retransmision.isMultiPull();
	}

	@Override
	protected void query(DataOutputStream os, DataInputStream is) throws IOException {
		if (chunks > 0) {
			pull(filename, is, os, chunks, ordinalNumber, numberOfHosts, multiPull);

		} else {
			if (multiPull) {
				retransmitPartOfMultiPull(os, is);
			} else {
				push(filename, is, os, true);
			}
		}
		os.writeInt(0);
		os.flush();
	}

	private void retransmitPartOfMultiPull(DataOutputStream os, DataInputStream is) throws IOException {
		os.writeInt(4);
		os.writeUTF(filename);
		os.writeInt(ordinalNumber);
		os.writeInt(numberOfHosts);
		if (is.readBoolean()) {
			push(filename, is, os, true);
		} 

	}

	@Override
	protected ProgressLogEvent initializeRespondToPush(DataOutputStream os, MyFile tmpMyFile) throws IOException {
		if (!multiPull) {
			return super.initializeRespondToPush(os, tmpMyFile);
		} else {
			return new ProgressLogEvent(destHost, -1, filename, ordinalNumber, numberOfHosts, true); 
		}
	}
	
	@Override
	protected int myFileToDataOutputStream(MyFile myFile, DataOutputStream os, int skippedChunks)
			throws InterruptionException{
		if (multiPull){
			return super.myFileToDataOutputStream(myFile, os, skippedChunks, ordinalNumber, numberOfHosts);
		} else {
			return super.myFileToDataOutputStream(myFile, os, skippedChunks);
		}
	
	
	}

}
