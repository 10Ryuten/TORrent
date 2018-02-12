package mhTcp;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerSocketRunner implements Runnable {

	protected LoggingHost thisHost;

	public ServerSocketRunner(LoggingHost loggingHost) {
		this.thisHost = loggingHost;
	}

	@Override
	public void run() {
		try (ServerSocket ss = new ServerSocket(thisHost.getServerPort())) {

			while (!Thread.interrupted()) {
				new Thread(new RespondingSocketRunner(thisHost, ss.accept())).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
