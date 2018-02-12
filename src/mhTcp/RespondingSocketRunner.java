package mhTcp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;

public class RespondingSocketRunner extends HostSocket implements Runnable {

	private final Socket socket;
	protected Host questioningHost;

	public RespondingSocketRunner(LoggingHost loggingHost, Socket socket) {
		super(loggingHost);
		this.socket = socket;
	}

	@Override
	public void run() {
		try (DataOutputStream os = new DataOutputStream(socket.getOutputStream());
				DataInputStream is = new DataInputStream(socket.getInputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

			String line = br.readLine().split(" ")[0];

			switch (line) {
			case "Host":
				os.writeInt(1); // separate BR
				os.flush();
				questioningHost = new Host(is.readInt());

				handleHost(os, is);
				break;
			// "GET / HTTP/1.1":
			case "GET":
				thisHost.logAndShow(new LogEvent());
				httpReq();
				break;

			case "ssKiller":
				break;

			default:
				System.out.println("Server: Niezidentyfikowane polaczenie przychodzace");
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Polaczenie przerwane, host: " + questioningHost);
		} finally {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void handleHost(DataOutputStream os, DataInputStream is) throws IOException {
		
		int choice;
		do {
			choice = is.readInt();

			switch (choice) {
			case 0:
				break;

			case 1:
				thisHost.logAndShow(new LogEvent(questioningHost, "Przesylam liste plikow"));
				sendListOfFiles(os);
				break;

			case 2:
				System.out.println("RespondToPull: " + questioningHost);
				respondToPull(is.readUTF(), is, os);
				break;

			case 3:
				System.out.println("RespondToPush: " + questioningHost);
				respondToPush(is.readUTF(), is, os, null);
				break;

			case 4:
				retransmitPartOfMultiPull(is, os);
				break;

			default:
				thisHost.logAndShow(new LogEvent(questioningHost, "Nieznany komunikat: " + choice));
			}
			os.flush();
		} while (choice != 0);
	}

	private void retransmitPartOfMultiPull(DataInputStream is, DataOutputStream os) throws IOException {
		String filename = is.readUTF();
		int ordinalNumber = is.readInt();
		int numberOfHosts = is.readInt();
		ProgressLogEvent event = getEventFromProgressLog(questioningHost, true, filename, ordinalNumber, numberOfHosts,
				true);
		if (event != null) {
			os.writeBoolean(true);
			respondToPush("tmp_" + ordinalNumber + "_" + filename, is, os, event);

		} else {
			os.writeBoolean(false);
		}
	}

	private void httpReq() {

		try (PrintWriter out = new PrintWriter(socket.getOutputStream())) {

			out.print("HTTP/1.1 200 OK\r\n");
			out.print("Content-Type: text/html\r\n\r\n");
			out.print("<html>\r\n");
			out.print("<head>\r\n");
			out.print("</head>\r\n");
			out.print("<body>\r\n");
			thisHost.sendLogs(out);
			out.print("</body>\r\n");
			out.print("</html>\r\n");
			out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void respondToPull(String filename, DataInputStream is, DataOutputStream os) throws IOException {

		int skippedChunks = is.readInt();
		int ordinalNumber = is.readInt();
		int numberOfHosts = is.readInt();
		boolean multiPull = is.readBoolean();
		String fullFileName;
		if (!multiPull) {
			fullFileName = filename;
		} else {
			fullFileName = filename + " " + (1 + ordinalNumber) + " / " + numberOfHosts;
		}

		try {

			MyFile tmpMyFile = findMyFileInMyFilesList(filename);

			os.writeBoolean(true);

			int sendedChunks = -1;

			ProgressLogEvent event = new ProgressLogEvent(questioningHost, -1, filename, ordinalNumber, numberOfHosts,
					multiPull);

			try {
				if (!multiPull) {
					sendedChunks = myFileToDataOutputStream(tmpMyFile, os, skippedChunks);
				} else {
					sendedChunks = myFileToDataOutputStream(tmpMyFile, os, skippedChunks, ordinalNumber, numberOfHosts);
				}
				os.writeInt(sendedChunks);
				eraseEventAndSaveProgressLog(event);
				thisHost.logAndShow(new LogEvent(questioningHost, "RespondToPull: Przeslalem plik: " + fullFileName));
			} catch (InterruptionException e) {
				thisHost.logAndShow(
						new LogEvent(questioningHost, "RespondToPull: Problem z przeslaniem pliku: " + fullFileName));
				saveToProgressLogs(event);
				throw new IOException("Przerwano polaczenie, rozlaczam hosta");
			}

		} catch (NoSuchElementException e) {
			os.writeBoolean(false);
			thisHost.logAndShow(new LogEvent(questioningHost, "RespondToPull: Brak pliku: " + fullFileName));
		}
	}

	private void sendListOfFiles(DataOutputStream os) throws IOException {
		List<MyFile> myFilesList = mapFiles();
		os.writeInt(myFilesList.size());
		for (int i = 0; i < myFilesList.size(); ++i) {
			MyFile tmpMyFile = myFilesList.get(i);
			os.writeUTF(tmpMyFile.getFileName());
			os.writeInt(tmpMyFile.getMD5().length);
			os.write(tmpMyFile.getMD5());
		}
	}

	private void respondToPush(String filename, DataInputStream is, DataOutputStream os, ProgressLogEvent event)
			throws IOException {

		if (event == null) {
			event = getEventFromProgressLog(questioningHost, true, filename, 0, 1, false);
		}
		boolean retransmision = is.readBoolean();
		if (retransmision && event != null) {
			os.writeInt(event.getChunks());
		} else {
			event = new ProgressLogEvent(questioningHost, 0, filename, 0, 1, false);
			os.writeInt(0);
		}

		int downloadedChunks;
		try {
			downloadedChunks = dataInputStreamToFile(filename, is, questioningHost, event.getChunks());
		} catch (InterruptionException e) {
			downloadedChunks = e.getChunks();
		}
		
		int sendedChunks = is.readInt();
		int shouldDownload = sendedChunks / event.getNumberOfHosts();
		if (sendedChunks % event.getNumberOfHosts() > event.getOrdinalNumber())
			++shouldDownload;
		
		if (downloadedChunks == shouldDownload){
			
			eraseEventAndSaveProgressLog(event);
			thisHost.logAndShow(new LogEvent(questioningHost, "RespondToPush: Udalo sie otrzymac plik: " + filename));
		} else {
			event.setChunks(downloadedChunks);
			saveToProgressLogs(event);
			thisHost.logAndShow(new LogEvent(questioningHost,
					"RespondToPush: Problem z otrzymaniem lub utworzeniem pliku: " + filename));
		}

	}

}
