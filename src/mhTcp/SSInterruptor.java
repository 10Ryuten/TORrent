package mhTcp;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SSInterruptor {

	static void interruptSSThread(LoggingHost host, Thread ssThread) {
		ssThread.interrupt();

		try (Socket socket = new Socket(host.getServerName(), host.getServerPort());
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						new DataOutputStream(socket.getOutputStream())))){
			bw.write("ssKiller");
			bw.newLine();
			bw.flush();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
