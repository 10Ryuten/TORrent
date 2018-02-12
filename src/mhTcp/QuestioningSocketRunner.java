package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class QuestioningSocketRunner extends QuestioningSocket implements Runnable {

	private String fileName;
	private int chunks;
	private int ordinalNumber;
	private int numberOfHosts;
	private List<Integer> feedbackFromThreads;

	public QuestioningSocketRunner(LoggingHost loggingHost, Host destHost, String fileName, int chunks, int ordinalNumber,
			int numberOfHosts, List<Integer> feedbackFromThreads) {
		super(loggingHost, destHost);
		this.fileName = fileName;
		this.ordinalNumber = ordinalNumber;
		this.numberOfHosts = numberOfHosts;
		this.feedbackFromThreads = feedbackFromThreads;
		this.chunks = chunks;
	}

	@Override
	public void run() {
		ask();
	}

	@Override
	protected void query(DataOutputStream os, DataInputStream is) throws IOException {

		try {
			pull(fileName, is, os, chunks, ordinalNumber, numberOfHosts, true);
			feedbackFromThreads.set(ordinalNumber, 1);
		} catch (Exception e) {
			feedbackFromThreads.set(ordinalNumber, -1);
		}

		os.writeInt(0);
		os.flush();
	}

	protected int dataInputStreamToFile(String fileName, DataInputStream is, Host host, int skippedChunks)
			throws InterruptionException {
		return super.dataInputStreamToFile("tmp_" + ordinalNumber + "_" + fileName, is, host, skippedChunks);
	}

}
