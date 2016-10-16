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
	private Scanner scan = new Scanner(System.in);
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
				scan.close();
				save();
				cfg.save();
			}
			
		});
		
		line();
		print("+-+-+-+-+-+-+ WELCOME TO QUIZLET-OFFLINE +-+-+-+-+-+-+");
		print("+            (by Max Karpawich 10/1/2016)            +");
		print("+  https://github.com/19mkarpawich/quizlet-offline/  +");
		print("+                                                    +");     
		print("+ 5 commands:                                        +");
		print("+    download [name] [link]: downloads a card set    +");
		print("+    quiz [index]: opens 'quiz mode' for a saved set +");
		print("+    list OR list [index]: lists saved sets or terms +");
		print("+    exit: exits the program.                        +");
		print("+    remove [index]: deletes a saved set.            +");
		print("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
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
							line();
							print("================================");
							print("         SET DOWNLOADED");
							print("   Name: " + set.getName());
							print("   Link: " + args[1]);
							print("\n\t================================");
							
						}else error("that name already exists. choose another");
					}else error("correct usage: download [name] [link]");
					break;
				case "quiz":
					if(args.length == 1) {
						currentQuiz = Integer.parseInt(args[0]);
						quizMode();
					}else{
						error("correct usage: open [index]");
					}
					break;
				case "list":
					if(args.length == 0) {
						line();
						print("-=-=-=-=-=- " + sets.size() + " sets -=-=-=-=-=-");
						int i = 0;
						for(CardSet set : sets) {
							print("");
							print(i + ": " + set.getName() + ": " + set.getTermCount() + " terms");
							i++;
						}
					}else if (args.length == 1) {
						CardSet set = sets.get(Integer.parseInt(args[0]));
						line();
						print("-=-=-=-=-=- " + set.getTermCount() + " terms -=-=-=-=-=-");
						int i = 0;
						for(String term: set.getTerms()) {
							print("");
							print(i + ": " + term + ": " + set.getDefinition(term));
							i++;
						}
					}else error("correct usage: list OR list [index] ");
					break;
				case "remove":
					if(args.length == 1) {
						CardSet toRem = sets.get(Integer.parseInt(args[0]));
						sets.remove(toRem);
						line();
						print("================================");
						print("         SET REMOVED");
						print("   Name: " + toRem.getName());
						print("\n\t================================");
					}else{
						error("correct usage: remove [index]");
					}
					break;
				case "exit":
					System.exit(1);
					break;
				default:
					error("correct usage: [download/quiz/list/exit/remove]");
					break;
				}
			}
			
		};
	}
	
	private void quizMode() {
		line();
		print("================================");
		print("           QUIZ MODE\n");
		print("   Name: " + sets.get(currentQuiz).getName() + "\n");
		print("To exit, type %EXIT%. Print term first?");
		print("Type 'y' for term first, 'n' for definition first.");
		print("\n\t================================");
		if(scan.nextLine().toLowerCase().contains("y")) {
			wrong = sets.get(currentQuiz).getTermPairs();
			line();
			print("<---- TERM FIRST ---->");
		}else{
			line();
			print("<---- DEFINITION FIRST ---->");
			wrong = sets.get(currentQuiz).getTermPairs().entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		}
		String lastTerm = null;
		while(wrong.size() > 0) {
			line();
			print("=====================================================");
			print(wrong.size() + " terms left.");
			print("=====================================================");
			List<String> keys = new ArrayList<String>(wrong.keySet());
			Collections.shuffle(keys);
			String skip = null;
			for(String term : keys) System.out.print(term + ",");
			System.out.println();
			System.out.println(lastTerm);
			for(String term : keys) {
				if(skip == null || !skip.equals(term)) {
					line();
					printNoLineBreak(term + ": ");
					String line = scan.nextLine();
					if(line.equalsIgnoreCase("%exit%")) {
						currentQuiz = -1;
						line();
						print("================================");
						print("        EXITED QUIZ MODE");
						print("================================");
						return;
					}else if (line.equalsIgnoreCase("%override%")) {
						if(lastTerm != null) {
							wrong.remove(lastTerm);
							line();
							print("================================");
							print("           CORRECT!\n\t");
							print("   For: " + lastTerm);
							print("\n\t================================");
							skip = lastTerm;
							lastTerm = null;
							if(wrong.size() > 0 && !skip.equals(term)) {
								line();
								printNoLineBreak(term + ": ");
								String line1 = scan.nextLine();
								if(line1.equalsIgnoreCase("%exit%")) {
									currentQuiz = -1;
									line();
									print("================================");
									print("        EXITED QUIZ MODE");
									print("================================");
									return;
								}
								if(wrong.get(term).equals(line1)) {
									wrong.remove(term);
									line();
									print("================================");
									print("           CORRECT!\n\t");
									print("   For: " + term);
									print("\n\t================================");
								}else{
									lastTerm = term;
									line();
									print("================================");
									print("           INCORRECT!\n\t");
									print("   For: " + term);
									print("   Correct: " + wrong.get(term) + "\n");
									print("To override type %OVERRIDE%");
									print("\n\t================================");
								}
							}
						}else{
							error("no incorrect answer to override");
							line();
							printNoLineBreak(term + ": ");
							String line1 = scan.nextLine();
							if(wrong.get(term).equals(line1)) {
								wrong.remove(term);
								line();
								print("================================");
								print("           CORRECT!\n\t");
								print("   For: " + term);
								print("\n\t================================");
							}else{
								lastTerm = term;
								line();
								print("================================");
								print("           INCORRECT!\n\t");
								print("   For: " + term);
								print("   Correct: " + wrong.get(term) + "\n");
								print("To override type %OVERRIDE%");
								print("\n\t================================");
							}
						}
					}else{
						if(wrong.get(term).equals(line)) {
							wrong.remove(term);
							line();
							print("================================");
							print("           CORRECT!\n\t");
							print("   For: " + term);
							print("\n\t================================");
						}else{
							lastTerm = term;
							line();
							print("================================");
							print("           INCORRECT!\n\t");
							print("   For: " + term);
							print("   Correct: " + wrong.get(term) + "\n");
							print("To override type %OVERRIDE%");
							print("\n\t================================");
						}
					}
				}else skip = null;
			}
			
		}
		line();
		print("<+><+><+><+><+><+><+><+><+><+><+><+>");
		print("      You got all correct!\n\t");
		print("   Name: " + sets.get(currentQuiz).getName());
		print("\n\tRepeat? Type 'y' for yes, 'n' for no.");
		print("\n\t<+><+><+><+><+><+><+><+><+><+><+><+>");
		String rspns = scan.nextLine();
		if(rspns.toLowerCase().contains("y")) {
			quizMode();
		}else{
			currentQuiz = -1;
			line();
			print("================================");
			print("        EXITED QUIZ MODE");
			print("================================");
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
	
	private void print(String msg) {
		System.out.println("\t" + msg);
	}
	
	private void printNoLineBreak(String msg) {
		System.out.print("\t" + msg);
	}
	
	private void line() { System.out.println("\n"); }
	
	private void error(String error) {
		line();
		print("================================");
		print("            ERROR\n");
		print(error);
		print("\n\t================================");
	}

}
