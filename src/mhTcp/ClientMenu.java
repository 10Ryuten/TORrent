package mhTcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

public class ClientMenu extends HostSocket {

	Scanner console;

	public ClientMenu(LoggingHost loggingHost) {
		super(loggingHost);
		console = new Scanner(System.in);
	}

	public void serveCustomer() {

		String choice;
		do {
			showWelcomeMenu();
			choice = console.next();
			switch (choice) {
			case "0":
				System.out.println("Zamykam hosta");
				break;

			case "1":
				System.out.println("Z ktorym sie polaczyc?");
				System.out.println("'0'    wroc");
				SortedSet<Integer> hosts = askServerAboutHosts();
				showSetOfHostNumbers(hosts);
				int chosenHost = askCustomerAboutHostNumber(hosts);
				if (chosenHost != 0)
					new QuestioningSocket(thisHost, new Host(chosenHost)).ask();
				break;

			case "2":
				System.out.println("Podaj nazwe pliku:");
				String fileName = console.next();
				System.out.println("Z ktorych hostow mam pobierac? ( '0', aby zakonczyc wybor )");
				SortedSet<Host> setOfHosts = askCustomerAboutSetOfHosts();
				if (!setOfHosts.isEmpty()) {
					if (isHostsAvailable(setOfHosts)) {
						pullFileFromMultipleHosts(fileName, setOfHosts);
					} else {
						System.out.println("Brak hosta/hostow w sieci: " + Arrays.toString(setOfHosts.toArray()));
					}
				}

				break;

			case "3":
				System.out.println("Lista plikow:");
				showListOfFiles(mapFiles());
				break;

			case "4":
				System.out.println("Lista plikow z przerwana transmisja ('0' aby wrocic):");
				List<ProgressLogEvent> progressLogs = getProgressLog();
				showAndAdjustProgressLog(progressLogs);
				ProgressLogEvent retransmision = askCustomerAboutRetransmision(progressLogs);
				if (retransmision != null) {
					retransmit(retransmision);
				}

				break;

			default:
				System.out.println("Nieznane polecenie: " + choice);

			}

		} while (!choice.equals("0"));

	}

	private ProgressLogEvent askCustomerAboutRetransmision(List<ProgressLogEvent> progressLogs) {
		int i = -1;
		boolean isLogOk = false;
		while (!isLogOk) {
			try {
				i = Integer.parseInt(console.next());
				isLogOk = (i >= 0 && i <= progressLogs.size());
				if (!isLogOk) {
					System.out.println("Brak takiej retransmisji, sprobuj ponownie ('0' aby wrocic):");
				}
			} catch (NumberFormatException e) {
				System.out.println("Nieprawidlowy wybor, sprobuj ponownie");
			}
		}

		if (i == 0) {
			return null;
		} else {
			return progressLogs.get(i - 1);
		}
	}

	private void retransmit(ProgressLogEvent retransmision) {

		if (retransmision.isPull() && retransmision.isMultiPull()) {
			multiRetransmission(retransmision);
		} else {
			new RetransmisionSocket(thisHost, retransmision).ask();
		}
	}

	private void multiRetransmission(ProgressLogEvent retransmision) {
		Thread interruptionThread = interruptionThread(Thread.currentThread());
		List<ProgressLogEvent> relatedProgressEvents = getRelatedEventsFromProgressLog(retransmision);

		int size = retransmision.getNumberOfHosts();
		List<Integer> feedbackFromThreads = Collections.synchronizedList(new ArrayList<Integer>(size));
		for (int i = 0; i < size; ++i) {
			feedbackFromThreads.add(1);
		}

		int remainingSize = relatedProgressEvents.size();
		Thread[] threads = new Thread[remainingSize];
		int i = 0;
		for (ProgressLogEvent event : relatedProgressEvents) {
			int ordinalNumber = event.getOrdinalNumber();
			feedbackFromThreads.set(ordinalNumber, 0);
			threads[i] = new Thread(new QuestioningSocketRunner(thisHost, event.getHost(), event.getFileName(),
					event.getChunks(), ordinalNumber, size, feedbackFromThreads));
			threads[i].start();
			++i;
		}

		waitForThreads(threads);

		interruptionThread.interrupt();

		tryToSyntesise(retransmision.getFileName(), size, feedbackFromThreads);

	}

	private void showAndAdjustProgressLog(List<ProgressLogEvent> progressLogs) {

		if (progressLogs.size() == 0) {
			System.out.println("Brak przerwanych transmisji");
		}

		int i = 1;
		List<ProgressLogEvent> newProgressLogs = new ArrayList<ProgressLogEvent>();
		for (ProgressLogEvent event : progressLogs) {

			if (event.isMultiPull() == false || (event.isMultiPull() && event.getOrdinalNumber() == -1)
					|| (event.isMultiPull() && event.isPull() == false)) {
				newProgressLogs.add(event);
				String eventInfo;
				if (event.isPull()) {
					if (event.isMultiPull()) {
						eventInfo = "MultiPull, nazwa pliku: " + event.getFileName();
					} else {
						eventInfo = "Pull, nazwa pliku: " + event.getFileName() + ", host: " + event.getHost();
					}
				} else {
					if (event.isMultiPull()) {
						eventInfo = "Push, z MultiPulla, nazwa pliku: " + event.getFileName() + ", host: " + event.getHost();
					} else {
						eventInfo = "Push, nazwa pliku: " + event.getFileName() + ", host: " + event.getHost();
					}
				}
				System.out.println("'" + i + "'\t" + eventInfo);
				++i;
			}
		}
		progressLogs = newProgressLogs;
	}

	private boolean isHostsAvailable(SortedSet<Host> setOfHosts) {
		SortedSet<Host> hosts = hostsFromHostNumbers(askServerAboutHosts());

		if (hosts.containsAll(setOfHosts)) {
			return true;
		} else {
			setOfHosts.removeAll(hosts);
			return false;
		}
	}

	private SortedSet<Host> askCustomerAboutSetOfHosts() {
		SortedSet<Host> setOfHosts = new TreeSet<Host>();
		int inputHostNumber = -1;

		do {
			try {
				inputHostNumber = Integer.parseInt(console.next());
				if (inputHostNumber != 0)
					setOfHosts.add(new Host(inputHostNumber));
			} catch (NumberFormatException e) {
				System.out.println("Nieprawidlowy wybor, prosze wprowadzic liczbe...");
			}

		} while (inputHostNumber != 0);
		return setOfHosts;
	}

	private int askCustomerAboutHostNumber(SortedSet<Integer> hosts) {

		int i = -1;
		boolean isHostOk = false;
		while (!isHostOk) {
			try {
				i = Integer.parseInt(console.next());
				isHostOk = (i == 0 || hosts.contains(i));
				if (!isHostOk) {
					System.out.println("Brak takiego hosta na liscie, sprobuj ponownie ('0' aby wrocic):");
				}
			} catch (NumberFormatException e) {
				System.out.println("Nieprawidlowy wybor, prosze wprowadzic liczbe...");
			}
		}
		return i;
	}

	private void showSetOfHostNumbers(SortedSet<Integer> hosts) {
		for (int hostNumber : hosts) {
			Host host = new Host(hostNumber);
			System.out.println("'" + host.getHostNumber() + "'\t" + host.getServerName() + " " + host.getServerPort());
		}
	}

	private void showListOfFiles(List<MyFile> listOfFiles) {

		listOfFiles.forEach((myFile) -> {
			System.out.print(myFile.getFileName() + " ");
			for (byte b : myFile.getMD5()) {
				System.out.print((String.format("%02X", b)));
			}
			System.out.println();
		});
	}

	private void showWelcomeMenu() {
		System.out.println("Prosze wprowadzic numer operacji...");
		System.out.println("'0' Koniec");
		System.out.println("'1' Lista hostow");
		System.out.println("'2' Pobieranie pliku z wielu hostow");
		System.out.println("'3' Lista plikow tego hosta");
		System.out.println("'4' Wznowienie transmisji pliku");
	}

	private void pullFileFromMultipleHosts(String fileName, SortedSet<Host> setOfHosts) {

		Thread interruptionThread = interruptionThread(Thread.currentThread());
		int size = setOfHosts.size();
		List<Integer> feedbackFromThreads = Collections.synchronizedList(new ArrayList<Integer>(size));
		Thread[] threads = startQuestioningThreads(fileName, setOfHosts, size, feedbackFromThreads);
		waitForThreads(threads);
		interruptionThread.interrupt();

		tryToSyntesise(fileName, size, feedbackFromThreads);

	}

	private void tryToSyntesise(String fileName, int size, List<Integer> feedbackFromThreads) {
		ProgressLogEvent event = new ProgressLogEvent(thisHost, fileName, size);

		if (isGoodFeedback(feedbackFromThreads)) {

			synthesiseFile(fileName, size);
			deleteTmpFiles(fileName, size);
			eraseEventAndSaveProgressLog(event);
		} else {
			saveToProgressLogs(event);
			thisHost.logAndShow(new LogEvent(thisHost, "MultiPull: Nie udalo sie pobrac pliku: " + fileName));
		}
	}

	private boolean isGoodFeedback(List<Integer> feedbackFromThreads) {
		for (Integer i : feedbackFromThreads) {
			if (i != 1)
				return false;
		}
		return true;
	}

	private Thread[] startQuestioningThreads(String fileName, SortedSet<Host> setOfHosts, int size,
			List<Integer> feedbackFromThreads) {
		Thread[] threads = new Thread[size];
		Iterator<Host> iter = setOfHosts.iterator();
		for (int i = 0; i < size; ++i) {
			feedbackFromThreads.add(0);
			threads[i] = new Thread(
					new QuestioningSocketRunner(thisHost, iter.next(), fileName, 0, i, size, feedbackFromThreads));
			threads[i].start();
		}
		return threads;
	}

	private void waitForThreads(Thread[] threads) {
		try {
			for (int i = 0; i < threads.length; ++i) {
				threads[i].join();
			}
		} catch (InterruptedException e) {
			interruptThreads(threads);
		}

	}

	private void interruptThreads(Thread[] threads) {
		for (Thread t : threads) {
			t.interrupt();
		}

	}

	private SortedSet<Host> hostsFromHostNumbers(SortedSet<Integer> hostNumbers) {
		SortedSet<Host> hosts = new TreeSet<Host>();
		for (Integer hostNumber : hostNumbers) {
			hosts.add(new Host(hostNumber));
		}
		return hosts;
	}

	private void deleteTmpFiles(String fileName, int size) {

		for (int i = 0; i < size; ++i) {
			String path = thisHost.getDPATH() + "/tmp_" + i + "_" + fileName;
			File file = new File(path);
			if (!file.delete()) {
				System.out.println("Operacja kasowania pliku tmp nie powiodla sie " + (i + 1) + " / " + size);
			}
		}
	}

	private void synthesiseFile(String fileName, int size) {
		System.out.println("Syntezowanie pliku: " + fileName + ", prosze czekac...");
		FileInputStream[] fisArr = new FileInputStream[size];
		try {
			for (int i = 0; i < size; ++i) {
				fisArr[i] = new FileInputStream(thisHost.getDPATH() + "/tmp_" + i + "_" + fileName);
			}

			createFile(fileName);

			concatFISArrIntoFile(fileName, fisArr);

			thisHost.logAndShow(new LogEvent(thisHost, "MultiPull: Udalo sie polaczyc plik: " + fileName));
		} catch (IOException e) {
			System.out.println("MultiPull: Problem z synteza pliku: " + fileName);
		} finally {
			for (int i = 0; i < size; ++i) {
				if (fisArr[i] != null)
					try {
						fisArr[i].close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
	}

	private void concatFISArrIntoFile(String fileName, FileInputStream[] fisArr)
			throws IOException, FileNotFoundException {
		int read = 1;
		while (read > 0) {
			for (int i = 0; i < fisArr.length; ++i) {

				try (FileOutputStream fos = new FileOutputStream(thisHost.getDPATH() + "/" + fileName, true)) {

					byte[] buffer = new byte[BUFFER_SIZE];

					if ((read = fisArr[i].read(buffer)) > 0) {
						fos.write(buffer, 0, read);
					} else
						break;
				}

			}
		}
	}

	private void createFile(String fileName) {
		try (FileOutputStream fos = new FileOutputStream(thisHost.getDPATH() + "/" + fileName)) {
		} catch (IOException e) {
			System.out.println("Problem z utworzeniem pliku: " + fileName);
		}
	}

	private SortedSet<Integer> askServerAboutHosts() {

		SortedSet<Integer> hostsSet = Collections.synchronizedSortedSet(new TreeSet<Integer>());
		new HostToServerQuestioningSocket(thisHost, new Questionable() {

			@Override
			public void askQuestion(LoggingHost thisHost, Host server, DataOutputStream os, DataInputStream is)
					throws IOException {

				os.writeInt(2);
				os.flush();

				int size = is.readInt();
				for (int i = 0; i < size; ++i) {
					int hostNumber = is.readInt();
					if (hostNumber != thisHost.getHostNumber()) {
						hostsSet.add(hostNumber);
					}
				}

			}
		}).ask();
		return hostsSet;
	}

}
