package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;

public class ServersRespondingSocketRunner extends RespondingSocketRunner {

	Set<Host> hostsSet;

	public ServersRespondingSocketRunner(LoggingHost loggingHost, Socket socket, Set<Host> hostsSet) {
		super(loggingHost, socket);
		this.hostsSet = hostsSet;
	}

	@Override
	protected void handleHost(DataOutputStream os, DataInputStream is) throws IOException {
		int choice = -1;
		choice = is.readInt();

		switch (choice) {
		case 0:
			thisHost.logAndShow(
					new LogEvent(questioningHost, "Host odmeldowuje sie"));
			hostsSet.remove(questioningHost);
			break;

		case 1:
			hostsSet.add(questioningHost);
			thisHost.logAndShow(
					new LogEvent(questioningHost, "Host melduje sie"));
			break;

		case 2:
			thisHost.logAndShow(
					new LogEvent(questioningHost, "Host prosi o zbior hostow"));

			os.writeInt(hostsSet.size());
			for (Iterator<Host> iter = hostsSet.iterator(); iter.hasNext();) {
				os.writeInt(iter.next().getHostNumber());
			}
			break;

		default:
			System.out.println("Nieznana komenda");
		}
	}

}
