package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HostToServerQuestioningSocket extends QuestioningSocket{
	
	Questionable asker;
	
	public HostToServerQuestioningSocket(LoggingHost loggingHost, Questionable asker) {
		super(loggingHost, new Host (0));
		this.asker = asker;
	}
	
	@Override
	protected void query(DataOutputStream os, DataInputStream is) throws IOException {
		asker.askQuestion(thisHost, destHost, os, is);
		
	}
}
