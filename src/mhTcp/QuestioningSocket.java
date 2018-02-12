package mhTcp;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class QuestioningSocket extends HostSocket {

	protected final Host destHost;

	public QuestioningSocket(LoggingHost thisHost, Host destHost) {
		super(thisHost);
		this.destHost = destHost;
	}

	public void ask() {
		try (Socket socket = new Socket(destHost.getServerName(), destHost.getServerPort());
				DataOutputStream os = new DataOutputStream(socket.getOutputStream());
				DataInputStream is = new DataInputStream(socket.getInputStream());
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {

			bw.write("Host");
			bw.newLine();
			bw.flush();
			is.readInt(); // separate BW

			os.writeInt(thisHost.getHostNumber());
			os.flush();

			query(os, is);

		} catch (IOException e) {
			System.out.println("Polaczenie zerwane z hostem: " + destHost);
		}

	}

	protected void query(DataOutputStream os, DataInputStream is) throws IOException {
		String command;
		do {
			showMenu();
			Scanner scanner = new Scanner(System.in);
			command = scanner.nextLine();
			makeTheChoice(os, is, command, scanner);

		} while (!command.equals("0"));
	}

	private void makeTheChoice(DataOutputStream os, DataInputStream is, String command, Scanner scanner)
			throws IOException {
		switch (command) {
		case "0":
			System.out.println("Rozlaczam hosta");
			os.writeInt(0);
			break;

		case "1":
			thisHost.logAndShow(new LogEvent(destHost, "Pobrana lista plikow:"));
			showMapOfFiles(getMapOfFiles(is, os));
			break;

		case "2":
			System.out.println("PULL: Prosze wprowadzic nazwe pliku:");
			pull(scanner.nextLine(), is, os, 0, 0, 1, false);

			break;

		case "3":
			System.out.println("PUSH: Prosze wprowadzic nazwe pliku:");
			push(scanner.nextLine(), is, os, false);
			break;

		default:
			System.out.println("Nieznana operacja");
		}

		os.flush();
	}

	private void showMenu() {
		System.out.println("Prosze wprowadzic numer operacji...");
		System.out.println("'0' Rozlacz hosta");
		System.out.println("'1' Wyswietl liste plikow");
		System.out.println("'2' PULL");
		System.out.println("'3' PUSH");
	}

	protected void push(String fileName, DataInputStream is, DataOutputStream os, boolean retransmit)
			throws IOException {

		try {

			MyFile tmpMyFile = findMyFileInMyFilesList(fileName); // NoSuchElementException
			
			
			ProgressLogEvent event = initializeRespondToPush(os, tmpMyFile);
			
			os.writeBoolean(retransmit);
			int skippedChunks = is.readInt();
			interruptablePush(fileName, os, tmpMyFile, skippedChunks, event);

		} catch (NoSuchElementException e) {
			thisHost.logAndShow(new LogEvent(destHost, "PUSH: Nie znaleziono pliku: " + fileName));
		}
	}

	protected ProgressLogEvent initializeRespondToPush(DataOutputStream os, MyFile tmpMyFile) throws IOException {
		os.writeInt(3);
		os.writeUTF(tmpMyFile.getFileName());
		return new ProgressLogEvent(destHost, -1, tmpMyFile.getFileName(), 0, 1, false);
	}

	private void interruptablePush(String fileName, DataOutputStream os, MyFile tmpMyFile, int skippedChunks, ProgressLogEvent event)
			throws IOException {
		Thread interruptionThread = interruptionThread(Thread.currentThread());

		int sendedChunks = -1;
		try {
			sendedChunks = myFileToDataOutputStream(tmpMyFile, os, skippedChunks);
			thisHost.logAndShow(new LogEvent(destHost, "PUSH: Udalo sie wyslac plik: " + fileName));
			eraseEventAndSaveProgressLog(event);

		} catch (InterruptionException e) {
			saveToProgressLogs(event);
			thisHost.logAndShow(new LogEvent(destHost, "PUSH: Problem z wyslaniem pliku: " + fileName));
			throw new IOException("Przerwano polaczenie, rozlaczam hosta");

		} finally {
			os.writeInt(sendedChunks);
			interruptionThread.interrupt();
		}

	}

	protected void pull(String fileName, DataInputStream is, DataOutputStream os, int skippedChunks, int ordinalNumber,
			int numberOfHosts, boolean multiPull) throws IOException {

		os.writeInt(2);
		os.writeUTF(fileName);
		os.writeInt(skippedChunks);
		os.writeInt(ordinalNumber);
		os.writeInt(numberOfHosts);
		os.writeBoolean(multiPull);
		boolean fileExists = is.readBoolean();
		if (fileExists) {
			if (multiPull) {
				interruptionMultiPull(fileName, is, os, skippedChunks, ordinalNumber, numberOfHosts);
			} else {
				iterruptionPull(fileName, is, os, skippedChunks, ordinalNumber, numberOfHosts);
			}
		} else {
			thisHost.logAndShow(new LogEvent(destHost, "PULL: Nie odnaleziono pliku: " + fileName));
		}

	}

	protected void iterruptionPull(String fileName, DataInputStream is, DataOutputStream os, int skippedChunks,
			int ordinalNumber, int numberOfHosts) throws IOException {
		Thread interruptionThread = interruptionThread(Thread.currentThread());

		ProgressLogEvent event = new ProgressLogEvent(destHost, 0, fileName, ordinalNumber, numberOfHosts, false);

		int pulledChunks = -1;
		try {
			pulledChunks = dataInputStreamToFile(fileName, is, destHost, skippedChunks);
			is.readInt(); // sended chunks
			eraseEventAndSaveProgressLog(event);
			thisHost.logAndShow(new LogEvent(destHost,
					"PULL: Udalo sie sciagnac plik: " + fileName));
		} catch (InterruptionException e) {
			pulledChunks = e.getChunks();
			event.setChunks(pulledChunks);
			saveToProgressLogs(event);
			thisHost.logAndShow(new LogEvent(destHost, "PULL: Problem z pobraniem lub utworzeniem pliku: " + fileName));
			throw new IOException("Przerwano polaczenie, rozlaczam hosta");
		} finally {
			interruptionThread.interrupt();
		}
	}
	
	private void interruptionMultiPull(String fileName, DataInputStream is, DataOutputStream os, int skippedChunks,
			int ordinalNumber, int numberOfHosts) throws IOException { 
		ProgressLogEvent event = new ProgressLogEvent(destHost, 0, fileName, ordinalNumber, numberOfHosts, true);
		
		int pulledChunks = -1;
		try {
			pulledChunks = dataInputStreamToFile(fileName, is, destHost, skippedChunks);
			is.readInt(); // sended chunks
			eraseEventAndSaveProgressLog(event);
			thisHost.logAndShow(new LogEvent(destHost,
					"MultiPull: Udalo sie sciagnac plik: " + fileName + " " + (ordinalNumber + 1) + " / " + numberOfHosts));
		} catch (InterruptionException e) {
			pulledChunks = e.getChunks();
			event.setChunks(pulledChunks);
			saveToProgressLogs(event);
			thisHost.logAndShow(new LogEvent(destHost, "MultiPull: Problem z pobraniem lub utworzeniem pliku: " + fileName
					+ " " + (ordinalNumber + 1) + " / " + numberOfHosts));
			throw new IOException("Przerwano polaczenie, rozlaczam hosta");
		}
	}
	
	private Map<String, byte[]> getMapOfFiles(DataInputStream is, DataOutputStream os) throws IOException {

		Map<String, byte[]> mapOfFiles = new HashMap<>();

		os.writeInt(1);
		int size = is.readInt();
		for (int i = 0; i < size; ++i) {
			String fileName = is.readUTF();
			byte[] md5 = new byte[is.readInt()];
			is.read(md5);

			mapOfFiles.put(fileName, md5);
		}
		return mapOfFiles;
	}

	private void showMapOfFiles(Map<String, byte[]> mapOfFiles) {

		mapOfFiles.forEach((fileName, md5) -> {
			System.out.print(fileName + " ");
			for (byte b : md5) {
				System.out.print((String.format("%02X", b)));
			}
			System.out.println();
		});
	}

}
