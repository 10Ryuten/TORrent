package mhTcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class HostSocket {

	protected final LoggingHost thisHost;
	protected final int BUFFER_SIZE;
	private static final String nameOfProgressLogs = "progressLogs.txt";

	public HostSocket(LoggingHost loggingHost) {
		super();
		this.thisHost = loggingHost;
		BUFFER_SIZE = 1024;

	}

	protected List<MyFile> mapFiles() {
		List<MyFile> myFilesList = new LinkedList<>();

		String DPATH = thisHost.getDPATH();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(DPATH))) {
			for (Path path : stream) {
				if (path.toFile().isFile()) {
					MyFile tmp = new MyFile(path);
					myFilesList.add(tmp);
				}
			}
		} catch (DirectoryIteratorException | IOException e) {
			System.out.println("Problem z katalogiem: " + DPATH);
		}
		return Collections.synchronizedList(myFilesList);
	}

	protected int myFileToDataOutputStream(MyFile myFile, DataOutputStream os, int skippedChunks)
			throws InterruptionException {
		
		return myFileToDataOutputStream(myFile, os, skippedChunks, 0, 1);
	}

	protected int myFileToDataOutputStream(MyFile tmpMyFile, DataOutputStream os, int skippedChunks, int ordinalNumber,
			int numberOfHosts) throws InterruptionException {
		Path path = tmpMyFile.getPath();

		boolean interrupted = false;
		skippedChunks *= numberOfHosts;
		int chunks = skippedChunks;
		try (FileInputStream fis = new FileInputStream(path.toFile())) {

			byte[] buffer = new byte[BUFFER_SIZE];

			while (skippedChunks > 0) {
				fis.skip(BUFFER_SIZE);
				--skippedChunks;
			}

			int read = 1;
			while (!(interrupted = Thread.interrupted()) && read > 0) {
				if (chunks % numberOfHosts == ordinalNumber) {
					read = fis.read(buffer);
					if (read > 0) {
						os.writeInt(read);
						os.write(buffer, 0, read);
						os.flush();
					}

				} else {
					read = (int) fis.skip(BUFFER_SIZE);
				}
				++chunks;
			}
			os.writeInt(0);
			os.flush();
		} catch (IOException e) {
			interrupted = true;
		}
		if (!interrupted) {
			return --chunks;
		} else {
			throw new InterruptionException(chunks);
		}
	}
	
	protected MyFile findMyFileInMyFilesList(String fileName) {
		
		return (MyFile) mapFiles().stream().filter(a -> a.getFileName().equals(fileName)).findFirst().get();
		
	}

	protected int dataInputStreamToFile(String fileName, DataInputStream is, Host destHost, int skippedChunks)
			throws InterruptionException {

		String path = thisHost.getDPATH() + "/" + fileName;
		int chunks = skippedChunks;

		boolean interrupted = false;
		try (FileOutputStream fos = new FileOutputStream(path, skippedChunks > 0)) {

			byte[] buffer = new byte[BUFFER_SIZE];

			int read = 1;

			while (read > 0 && !(interrupted = Thread.interrupted())) {
				read = is.readInt();
				is.readFully(buffer, 0, read);
				fos.write(buffer, 0, read);
				fos.flush();
				++chunks;
			}
		} catch (IOException e) {
			interrupted = true;
		}

		if (!interrupted) {
			return --chunks;
		} else {
			throw new InterruptionException(chunks);
		}
	}

	protected Thread interruptionThread(Thread t) {
		Thread interruptionThread = new Thread(() -> {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Nacisnij enter, aby przerwac operacje i rozlaczyc z hostem/hostami");
			try {
				while (!br.ready()) {

					Thread.sleep(200);
				}
				br.readLine();
				t.interrupt();
			} catch (InterruptedException e) {
			} catch (IOException e) {
				System.out.println("Interruption Thread: IOException");
				e.printStackTrace();
			}

		});
		interruptionThread.start();
		return interruptionThread;
	}

	protected synchronized void saveToProgressLogs(ProgressLogEvent event) {
		synchronized (thisHost) {
			List<ProgressLogEvent> progressLogs = eraseEventAndSaveProgressLog(event);
			progressLogs.add(event);
			saveProgressLogs(progressLogs);
		}
	}

	private void saveProgressLogs(List<ProgressLogEvent> progressLogs) {
		synchronized (thisHost) {
			final String progressLogsStringPath = thisHost.getDPATH() + "/" + nameOfProgressLogs;
			try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(progressLogsStringPath)))) {

				for (ProgressLogEvent s : progressLogs) {
					pw.println(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Problem z zapisem logow postepu do pliku");
			}
		}
	}

	protected List<ProgressLogEvent> getProgressLog() {

		List<ProgressLogEvent> progressLogs = new ArrayList<ProgressLogEvent>();
		try (FileReader fr = new FileReader(new File(thisHost.getDPATH() + "/" + nameOfProgressLogs));
				BufferedReader br = new BufferedReader(fr)) {

			String line;
			while ((line = br.readLine()) != null) {
				progressLogs.add(new ProgressLogEvent(line));
			}

		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			System.out.println("Problem z odczytaniem pliku logow postepu");
		}
		return progressLogs;

	}

	protected ProgressLogEvent getEventFromProgressLog(Host host, boolean isPull, String fileName, int ordinalNumber,
			int numberOfHosts, boolean multiPull) {
		List<ProgressLogEvent> progressLogs = getProgressLog();

		for (ProgressLogEvent s : progressLogs) {
			boolean pull = s.getChunks() >= 0;

			if (s.getHost().getHostNumber() == host.getHostNumber() && pull == isPull
					&& s.getFileName().equals(fileName) && s.getOrdinalNumber() == ordinalNumber
					&& s.getNumberOfHosts() == numberOfHosts && s.isMultiPull() == multiPull)
				return s;
		}
		return null;
	}

	protected List<ProgressLogEvent> eraseEventAndSaveProgressLog(ProgressLogEvent event) {
		synchronized (thisHost) {
			List<ProgressLogEvent> progressLogs = getProgressLog();
			ProgressLogEvent elementToErase = null;
			for (Iterator<ProgressLogEvent> iter = progressLogs.iterator(); iter.hasNext();) {
				ProgressLogEvent s = iter.next();

				if (s.getHost().getHostNumber() == event.getHost().getHostNumber() && s.isPull() == event.isPull()
						&& s.getFileName().equals(event.getFileName())
						&& s.getOrdinalNumber() == event.getOrdinalNumber()
						&& s.getNumberOfHosts() == event.getNumberOfHosts() && s.isMultiPull() == event.isMultiPull()) {
					elementToErase = s;
				}

			}
			progressLogs.remove(elementToErase);
			saveProgressLogs(progressLogs);
			return progressLogs;
		}
	}

	protected List<ProgressLogEvent> getRelatedEventsFromProgressLog(ProgressLogEvent event) {
		synchronized (thisHost) {
			List<ProgressLogEvent> progressLogs = getProgressLog();
			List<ProgressLogEvent> relatedEvents = Collections.synchronizedList(new LinkedList<ProgressLogEvent>());
			for (ProgressLogEvent s : progressLogs) {
				if (s.getFileName().equals(event.getFileName()) 
						&& s.getOrdinalNumber() != -1	// synteza
						&& s.getNumberOfHosts() == event.getNumberOfHosts()
						&& s.isMultiPull() == event.isMultiPull()) {
					relatedEvents.add(s);
				}
			}
			return relatedEvents;
		}
	}

}
