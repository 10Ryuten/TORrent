package mhTcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Host implements Comparable<Host> {

	private int hostNumber;
	private String hostName;
	private int hostPort;
	private String DPATH;
	//private final static String hostConfigFilePath = "src/hostsConfiguration.txt";
	private final static String hostConfigFilePath = "hostsConfiguration.txt";
	//private final static String serverConfigFilePath = "src/serverConfiguration.txt";
	private final static String serverConfigFilePath = "serverConfiguration.txt";
	
	public Host(int hostNumber) {
		this.hostNumber = hostNumber;

		if (hostNumber == 0)
			readServerConfig();
		else
			if (hostNumber > 0 )
				readHostConfig(hostNumber);
			else {
				System.out.println("Blad przy inicjalizacji Hosta: " + hostNumber);
			}
	}

	private void readServerConfig() {
		try (BufferedReader br = new BufferedReader(new FileReader(new File(serverConfigFilePath)))) {
			
			this.hostName = br.readLine();
			this.hostPort = Integer.parseInt(br.readLine());
			DPATH = null;

		} catch (IOException e) {
			System.out.println("Problem z plikiem konfigurcji servera");
		}
	}

	private void readHostConfig(int hostNumber) {
		try (BufferedReader br = new BufferedReader(new FileReader(new File(hostConfigFilePath)))) {
			for (int i = 3; i < hostNumber * 4; ++i) {
				br.readLine();
			}
			
			this.hostName = br.readLine();
			this.hostPort = Integer.parseInt(br.readLine());
			// D:\\TORrent_$
			//DPATH = br.readLine();
			DPATH = "E:/TORrent/" + hostNumber;

		} catch (IOException e) {
			System.out.println("Problem z plikiem konfigurcji hostow");
			e.printStackTrace();
		}
	}

	public String getServerName() {
		return hostName;
	}

	public int getServerPort() {
		return hostPort;
	}

	public int getHostNumber() {
		return hostNumber;
	}

	public String getDPATH() {
		return DPATH;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return hostNumber + " " + hostName + " " + hostPort + " " + DPATH;
	}

	@Override
	public int compareTo(Host host) {
		return this.getHostNumber() - host.getHostNumber();
	}
	
	
	

}
