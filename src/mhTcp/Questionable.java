package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Questionable {
	public void askQuestion(LoggingHost thisHost, Host server, DataOutputStream os, DataInputStream is) throws IOException;
}

