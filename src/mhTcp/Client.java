package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
public class Client {

	static LoggingHost thisHost;

	public static void main(String[] args) {

		int hostNumber = -1;
		if (args.length > 0) {
			hostNumber = Integer.parseInt(args[0]);
		}
		System.out.println("Klient F1-F5, F7, mutlihost, TCP: " + hostNumber);

		thisHost = new LoggingHost(hostNumber);

		Thread ssThread = new Thread(new ServerSocketRunner(thisHost));
		ssThread.start();

		signHost(true);
		
		ClientMenu menu = new ClientMenu(thisHost);
		menu.serveCustomer();
		
		signHost(false);
		SSInterruptor.interruptSSThread(thisHost, ssThread);
		
		thisHost.saveLogs();
	}

	private static void signHost(boolean in) {

		new HostToServerQuestioningSocket(thisHost, new Questionable() {
						
			@Override
			public void askQuestion(LoggingHost thisHost, Host server, DataOutputStream os, DataInputStream is) throws IOException {
				if (in) {
				os.writeInt(1);
				thisHost.logAndShow(
						new LogEvent(server, "Zameldowalem sie na serwerze"));
			} else {
				os.writeInt(0);
				thisHost.logAndShow(
						new LogEvent(server, "Wymeldowalem sie z serwera"));
			}
			os.flush();
			}
		}).ask();
	
			

	}

}
