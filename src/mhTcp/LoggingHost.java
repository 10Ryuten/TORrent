package mhTcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Decorator, host with logs
public class LoggingHost extends Host {
	private List<LogEvent> logs;

	private final static String nameOfLogFile = "logs.txt";
	
	public LoggingHost(int hostNumber) {
		super(hostNumber);

		logs = Collections.synchronizedList(new ArrayList<LogEvent>());
		if (hostNumber > 0)
			readLogFile(this);

	}

	private void readLogFile(Host host) {
		try (FileReader fr = new FileReader(new File(host.getDPATH() + "/" + nameOfLogFile));
				BufferedReader br = new BufferedReader(fr)) {

			if (br.readLine().equals(host.toString())) {
				System.out.println("Plik logow zgodny, wracam do sesji");

				String line;
				while ((line = br.readLine()) != null) {
					logs.add(new LogEvent(line));
				}

			} else {
				System.out.println("Plik logow z innego hosta, rozpoczynam nowa sesje");
			}

		} catch (FileNotFoundException e) {
			System.out.println("Brak pliku logow, rozpoczynam nowa sesje");

		} catch (IOException e) {
			System.out.println("Problem z odczytaniem pliku logow, rozpoczynam nowa sesje");
		}

	}

	public void saveLogs() {
		if (getHostNumber() == 0)
			return;

		try (PrintWriter pw = new PrintWriter(
				new BufferedWriter(new FileWriter(getDPATH() + "/" + nameOfLogFile)))) {

			pw.println(toString());

			for (LogEvent logEvent : logs) {
				pw.println(logEvent);
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Problem z zapisem logow do pliku");
		}

	}

	public void sendLogs(PrintWriter pw) {

		pw.print(toString() + "<br/>");

		for (LogEvent logEvent : logs) {
			pw.print(logEvent + "<br/>");
		}

	}

	public void logAndShow(LogEvent logEvent) {

		logs.add(logEvent);
		System.out.println(logEvent);
	}


}
