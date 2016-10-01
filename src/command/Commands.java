package command;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Commands extends Thread{
	private boolean cancelled;
	
	public Commands() {
		this.start();
	}
	
	@Override
	public void run() {
		try {
		Scanner in = new Scanner(System.in);
		while(!cancelled) {
			String line = in.nextLine();
			if(!line.equals("")) {
				if(!line.contains(";")) {
				List<String> split = new ArrayList<String>();
				Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
				while (m.find()) {
					String sect = m.group(1);
				    split.add(sect.replaceAll("\"", ""));
				}
				String[] args = new String[split.size() - 1];
				for(int i = 1; i < split.size(); i++) {
					args[i - 1] = split.get(i).toLowerCase();
				}
				onCommand(split.get(0).toLowerCase(), args);
				}else{
					String[] splitCmd = line.split(";");
					for(String line1 : splitCmd) {
						if(!line1.equals("")) {
							List<String> split = new ArrayList<String>();
							Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line1);
							while (m.find()) {
								String sect = m.group(1);
							    split.add(sect.replaceAll("\"", ""));
							}
							String[] args = new String[split.size() - 1];
							for(int i = 1; i < split.size(); i++) {
								args[i - 1] = split.get(i).toLowerCase();
							}
							onCommand(split.get(0).toLowerCase(), args);
						}
					}
				}
			}
		}
		in.close();
		}catch(NoSuchElementException e) {}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public abstract void onCommand(String cmd,String[] args);

}
