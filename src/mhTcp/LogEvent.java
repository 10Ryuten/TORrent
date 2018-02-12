package mhTcp;

import java.time.LocalDateTime;

public class LogEvent {
	private static final String splitter = " # ";
	
	private int subjectHostNumber;
	private LocalDateTime date;
	private String msg;
	
	// subjectHostNumber, date, host.toString(), msg 
	
	public LogEvent() {
		this.subjectHostNumber = -1;
		this.date = LocalDateTime.now();
		this.msg = "GET - HTTP";
	}
	
	public LogEvent(Host subjectHost, String msg) {
		this.subjectHostNumber = subjectHost.getHostNumber();
		this.date = LocalDateTime.now();
		this.msg = msg;
	}
	
	public LogEvent(String line) {
		String[] words = line.split(splitter);
		this.subjectHostNumber = Integer.parseInt(words[0]);
		this.date = LocalDateTime.parse(words[1]);
		this.msg = words[3];
	}

	
	
	@Override
	public String toString() {
		if (subjectHostNumber == -1)
		{
			return "-1" + splitter + date.toString() + splitter + "Przegladarka internetowa" + splitter + msg;
		} else
		return "" + subjectHostNumber + splitter + date.toString() + splitter + new Host(subjectHostNumber) + splitter + msg;
	}
	
	
}
