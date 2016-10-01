package quizlet.offline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CardSet {
	private String name;
	private Map<String,String> set = new HashMap<String,String>();
	
	public CardSet(String name) {
		this.setName(name);
	}
	
	public void addTerm(String term, String definition) {
		set.put(term, definition);
	}
	
	public void removeTerm(String term)  {
		set.remove(term);
	}
	
	public String getDefinition(String term) {
		return set.get(term);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getTermCount() {
		return set.size();
	}
	
	public Set<String> getTerms() {
		return set.keySet();
	}
	
	public Map<String,String> getTermPairs() {
		return new HashMap<String,String>(set);
	}

}
