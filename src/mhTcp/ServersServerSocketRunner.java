package mhTcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class ServersServerSocketRunner extends ServerSocketRunner {
	
	Set<Host> hostsSet; 
	
	public ServersServerSocketRunner(LoggingHost loggingHost) {
		super(loggingHost);
		hostsSet = Collections.synchronizedSet(new TreeSet<Host>());
	}
	
	@Override
	public void run() {

		try (ServerSocket ss = new ServerSocket(thisHost.getServerPort())) {

			while (!Thread.interrupted()) {

				new Thread(new ServersRespondingSocketRunner(thisHost, ss.accept(), hostsSet)).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
