package save;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Config {
	private File file;
	private List<String> lines;
	private ConfigParent list = new ConfigParent("");
	private PrintWriter out;
	
	public Config(File file) {
		this.file = file;
	}
	
	public void load() {
		System.out.println("attempting to load data...");
		if(file.exists()) {
			lines = new ArrayList<String>();
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String next;
				while((next = in.readLine()) != null) lines.add(next);
				load(0,-1);
				in.close();
				System.out.println("load successful. read from \'" + file.getAbsolutePath() + "\'");
			} catch (Exception e) {
				System.out.println("loading failed:");
				e.printStackTrace();
			}
		}else{
			System.out.println("configuration file doesn't exist. no data to load. (that's okay)");
			System.out.println("load successful.");
		}
	}
	
	private Object[] load(int tabs,int line) {
		Object[] toRet = new Object[2];
		int iRet = lines.size();
		ConfigParent parent = list;
		if(line != -1) parent = new ConfigParent(lines.get(line).substring(0,lines.get(line).length()-1).replaceAll("\t",""));
		String at;
		int count;
		int i = line+1;
		while(i < lines.size()) {
			at = lines.get(i);
			count = at.length() - at.replaceAll("\t", "").length();
			if(count >= tabs) {
				if(i != lines.size()-1) {
					if(at.endsWith(">")) {
						Object[] ret = load(tabs+1,i);
						i = (int) ret[0];
						parent.addChild((ConfigParent) ret[1]);
					}else if(at.contains("&")) {
						String[] split = at.split("&");
						ConfigChild child = new ConfigChild(split[0].replaceAll("\t", ""),split[1]);
						parent.addChild(child);
						i++;
					}else{
						System.out.println("failed to load entry at line " + i + ". incorrect format.");
						i++;
					}
				}else{
					if(at.contains("&")) {
						String[] split = at.split("&");
						ConfigChild child = new ConfigChild(split[0].replaceAll("\t", ""),split[1]);
						parent.addChild(child);
					}else{
						System.out.println("failed to load entry at line " + i + ". incorrect format.");
					}
					iRet = i + 1;
					break;
				}
			}else{
				iRet = i;
				break;
			}
		}
		toRet[0] = iRet;
		toRet[1] = parent;
		return toRet;
	}
	
	public void save() {
		if(!list.getChildren().isEmpty()) {
		System.out.println("attempting to save data...");
		try {
			out = new PrintWriter(new FileOutputStream(file),true);
		} catch (FileNotFoundException e) {
			System.out.println("saving failed:");
			e.printStackTrace();
		}
		save(list,0);
		out.close();
		System.out.println("save successful. written to \'" + file.getAbsolutePath() + "\'");
		}else{
			file.delete();
			System.out.println("no data to save. deleted save file (it's okay)");
			System.out.println("save successful.");
		}
	}
	
	private void save(ConfigParent parent,int i) {
		for(ConfigObject child : parent.getChildren()) {
			if(child instanceof ConfigParent) {
				for(int x = 0; x < i; x++) out.print('\t');
				out.println(child.getKey() + ">");
				save((ConfigParent) child,i + 1);
			}else{
				ConfigChild cast = (ConfigChild) child;
				for(int x = 0; x < i; x++) out.print('\t');
				out.println(cast.getKey() + '&' + cast.getValue());
			}
		}
	}
	
	public String getValue(String path) {
		String value;
		if(path.contains(".")) {
			String[] split = path.split("\\.");
			ConfigParent last = list;
			for(int i = 0;i < split.length - 1; i++) {
				String at = split[i];
				if(last.getChild(at) instanceof ConfigChild) {
					System.out.println(last.getChild(at).getKey());
					System.out.println("PATH: " + path);
				}
				last = (ConfigParent) last.getChild(at);
			}
			value = ((ConfigChild)last.getChild(split[split.length - 1])).getValue();
		}else{
			ConfigChild child = (ConfigChild) list.getChild(path);
			value = child.getValue();
			
		}
		return value;
	}
	
	public void setValue(String path,Object value) {
		if(path.contains(".")) {
			String[] split = path.split("\\.");
			ConfigParent last = list;
			for(int i = 0;i < split.length - 1; i++) {
				String at = split[i];
				if(!last.hasChild(at)) {
					ConfigParent parent = new ConfigParent(at);
					last.addChild(parent);
					last = parent;
				}else{
					ConfigObject child = last.getChild(at);
					if(child instanceof ConfigParent) {
						last = (ConfigParent) last.getChild(at);
					}else{
						System.out.println("failed to save key \'" + path + "\' with value \'" + value + "\'. existing entry of parent \'" + at + "\' is a child.");
						break;
					}
				}
			}
			String lastKey = split[split.length - 1];
			if(!last.hasChild(lastKey)) {
				ConfigChild child = new ConfigChild(lastKey,value.toString());
				last.addChild(child);
			}else{
				ConfigObject child = last.getChild(lastKey);
				if(child instanceof ConfigChild) {
					((ConfigChild) last.getChild(lastKey)).setValue(value.toString());
				}else{
					System.out.println("failed to save key \'" + path + "\' with value \'" + value + "\'. existing entry is a parent, not a child.");
				}
			}
		}else{
			if(!list.hasChild(path)) {
				ConfigChild at = new ConfigChild(path,value.toString());
				list.addChild(at);
			}else{
				ConfigObject at = list.getChild(path);
				if(at instanceof ConfigChild) {
					((ConfigChild) list.getChild(path)).setValue(value.toString());
				}else{
					System.out.println("failed to save key \'" + path + "\' with value \'" + value + "\'. existing entry is a parent, not a child.");
				}
			}
		}
	}
	
	public List<String> getChildren(String path) {
		List<String> children = new ArrayList<String>();
		if(path.contains(".")) {
			String[] split = path.split("\\.");
			ConfigParent last = list;
			for(int i = 0;i < split.length - 1; i++) {
				String at = split[i];
				last = (ConfigParent) last.getChild(at);
			}
			for(ConfigObject child : ((ConfigParent)last.getChild(split[split.length - 1])).getChildren()) {
				children.add(child.getKey());
			}
		}else if (!path.equals("")) {
			ConfigParent parent = (ConfigParent) list.getChild(path);
			for(ConfigObject child : parent.getChildren()) {
				children.add(child.getKey());
			}
			
		}else{
			for(ConfigObject child : list.getChildren()) {
				children.add(child.getKey());
			}
		}
		return children;
	}
	
	public boolean hasKey(String path) {
		if(path.contains(".")) {
			String[] split = path.split("\\.");
			ConfigParent last = list;
			for(int i = 0;i < split.length - 1; i++) {
				String at = split[i];
				if(!last.hasChild(at)) return false;
				last = (ConfigParent) last.getChild(at);
			}
			String end = split[split.length - 1];
			return last.hasChild(end);
		}else{
			return list.hasChild(path);
			
		}
	}
	
	public void clear() {
		list.clear();
	}
}

class ConfigObject {
	private String key;
	
	public ConfigObject(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
}

class ConfigParent extends ConfigObject{
	private List<ConfigObject> children = new ArrayList<ConfigObject>();
	public ConfigParent(String key) {
		super(key);
	}
	
	public List<ConfigObject> getChildren() {
		return children;
	}
	
	public void setChildren(List<ConfigObject> children) {
		this.children = children;
	}
	
	public void addChild(ConfigObject child) {
		children.add(child);
	}
	
	public void removeChild(ConfigObject child) {
		children.remove(child);
	}
	
	public void removeChild(int i) {
		children.remove(i);
	}
	
	public void removeChild(String key) {
		ConfigObject toRemove = null;
		for(ConfigObject child : children) {
			if(child.getKey().equalsIgnoreCase(key)) toRemove = child;
		}
		children.remove(toRemove);
	}
	
	public ConfigObject getChild(int i) {
		return children.get(i);
	}
	
	public ConfigObject getChild(String key) {
		for(ConfigObject child : children) {
			if(child.getKey().equalsIgnoreCase(key)) return child;
		}
		return null;
	}
	
	public boolean hasChild(String key) {
		for(ConfigObject child : children) {
			if(child.getKey().equalsIgnoreCase(key)) return true;
		}
		return false;
	}
	
	public void clear() {
		children.clear();
	}
}

class ConfigChild extends ConfigObject{
	private String value;
	
	public ConfigChild(String key,String value) {
		super(key);
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
