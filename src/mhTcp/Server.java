package mhTcp;

import java.util.Scanner;

public class Server {

	public static void main(String[] args) {
		
		System.out.println("Serwer");
		LoggingHost server = new LoggingHost(0);
		
		Thread serverThread = new Thread(new ServersServerSocketRunner(server));
    	serverThread.start();
		
    	
		System.out.println("Nacisnij enter, aby zakonczyc dzialanie serwera");
		
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		scanner.next();
		SSInterruptor.interruptSSThread(server, serverThread);
		
	}
	
}
