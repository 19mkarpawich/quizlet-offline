package quizlet.offline;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import command.Commands;
import save.Config;

public class Main {
	private Config cfg;
	private List<CardSet> sets = new ArrayList<CardSet>();
	private Map<String,String> wrong = new HashMap<String,String>();
	int currentQuiz = -1;
	public static void main(String[] args) {
		new Main();
	}
	
	public Main() {
		cfg = new Config(new File("cfg"));
		cfg.load();
		this.load();
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				System.out.println("exiting.. bye!");
				save();
				cfg.save();
			}
			
		});
		System.out.println("+-+-+-+-+-+-+ WELCOME TO QUIZLET-OFFLINE +-+-+-+-+-+-+");
		System.out.println("+ 4 commands:                                        +");
		System.out.println("+    download [name] [link]: downloads a card set    +");
		System.out.println("+    open [index]: opens 'quiz mode' for a saved set +");
		System.out.println("+    list OR list [index]: lists saved sets or terms +");
		System.out.println("+    exit: exits the program.                        +");
		System.out.println("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
		new Commands() {

			@Override
			public void onCommand(String cmd, String[] args ) {
				switch(cmd) {
				case "download":
					if(args.length == 2) {
						boolean nameExists = false;
						for(CardSet set : sets) {
							if(set.getName().equalsIgnoreCase(args[0])) nameExists = true;
						}
						if(!nameExists) {
							CardSet set = QuizletAPI.getSet(args[0], args[1]);
							sets.add(set);
							System.out.println("downloaded set \"" + set.getName() + "\"");
							
						}else System.out.println("that name already exists. choose another");
					}else{
						System.out.println("correct usage: download [name] [link]");
					}
					break;
				case "open":
					if(args.length == 1) {
						currentQuiz = Integer.parseInt(args[0]);
						quizMode();
					}else{
						System.out.println("correct usage: open [index]");
					}
					break;
				case "list":
					if(args.length == 0) {
						System.out.println("-=-=-=-=-=- " + sets.size() + " sets -=-=-=-=-=-");
						int i = 0;
						for(CardSet set : sets) {
							System.out.println();
							System.out.println(i + ": " + set.getName() + ": " + set.getTermCount() + " terms");
							i++;
						}
					}else if (args.length == 1) {
						CardSet set = sets.get(Integer.parseInt(args[0]));
						System.out.println("-=-=-=-=-=- " + set.getTermCount() + " terms -=-=-=-=-=-");
						int i = 0;
						for(String term: set.getTerms()) {
							System.out.println();
							System.out.println(i + ": " + term + ": " + set.getDefinition(term));
							i++;
						}
					}else System.out.println("correct usage: list OR list [index] ");
					break;
				case "remove":
					if(args.length == 1) {
						sets.remove(Integer.parseInt(args[0]));
					}else{
						System.out.println("correct usage: remove [index]");
					}
					break;
				case "exit":
					System.exit(1);
					break;
				default:
					System.out.println("correct usage: [download/open/list/exit]");
					break;
				}
			}
			
		};
	}
	
	private void quizMode() {
		System.out.println("YOU ARE NOW IN QUIZ MODE. TO EXIT TYPE %EXIT%");
		Scanner scan = new Scanner(System.in);
		System.out.println("term first? type 'y' for term first, 'n' for definition");
		if(scan.nextLine().toLowerCase().contains("y")) {
			wrong = sets.get(currentQuiz).getTermPairs();
			System.out.println("going terms first");
		}else{
			System.out.println("going definitions first");
			wrong = sets.get(currentQuiz).getTermPairs().entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		}
		String lastTerm = null;
		while(wrong.size() > 0) {
			List<String> keys = new ArrayList<String>(wrong.keySet());
			Collections.shuffle(keys);
			for(String term : keys) {
				System.out.print(term + ": ");
				String line = scan.nextLine();
				if(line.equalsIgnoreCase("%exit%")) {
					currentQuiz = -1;
					System.out.println("EXITED QUIZ MODE");
					scan.close();
					return;
				}else if (line.equalsIgnoreCase("%override%")) {
					if(lastTerm != null) {
						wrong.remove(lastTerm);
						System.out.println("CORRECT! (overridden: " + lastTerm + ") (continue)");
						String line1 = scan.nextLine();
						if(line1.equalsIgnoreCase("%exit%")) {
							currentQuiz = -1;
							System.out.println("EXITED QUIZ MODE");
							scan.close();
							return;
						}
						if(wrong.get(term).equals(line1)) {
							wrong.remove(term);
							System.out.println("CORRECT!");
						}else{
							lastTerm = term;
							System.out.println("INCORRECT! to override type %OVERRIDE% ");
						}
					}else{
						System.out.println("no incorrect answer to override (continue)");
						if(wrong.get(term).equals(line)) {
							wrong.remove(term);
							System.out.println("CORRECT!");
						}else{
							lastTerm = term;
							System.out.print("INCORRECT! to override type %OVERRIDE% ");
						}
					}
				}else{
					if(wrong.get(term).equals(line)) {
						wrong.remove(term);
						System.out.println("CORRECT!");
					}else{
						lastTerm = term;
						System.out.print("INCORRECT! to override type %OVERRIDE% ");
					}
				}
			}
			
		}
		System.out.println("you got \"" + sets.get(currentQuiz).getName() + "\" all correct! repeat? y/n");
		String rspns = scan.nextLine();
		if(rspns.toLowerCase().contains("y")) {
			scan.close();
			quizMode();
		}else{
			currentQuiz = -1;
			System.out.println("EXITED QUIZ MODE");
			scan.close();
			return;
		}
			
	}
	
	protected void save() {
		for(CardSet set : sets) {
			for(String term : set.getTerms()) {
				cfg.setValue(set.getName() + "." + term, set.getDefinition(term));
			}
		}
	}

	private void load() {
		for(String name : cfg.getChildren("")) {
			CardSet set = new CardSet(name);
			for(String term : cfg.getChildren(name)) {
				String def = cfg.getValue(name + "." + term);
				set.addTerm(term, def);
			}
			sets.add(set);
		}
		cfg.clear();
	}

}
