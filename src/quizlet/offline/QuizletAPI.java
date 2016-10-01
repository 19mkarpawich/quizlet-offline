package quizlet.offline;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class QuizletAPI {
	
	public static CardSet getSet(String name, String urlStr) {
		CardSet ret = new CardSet(name);
		String setID = "";
		for(int i = urlStr.indexOf("quizlet.com") + 12; i < urlStr.length(); i++) {
			char at = urlStr.charAt(i);
			if(at != '/') {
				setID+=at;
			}else break;
		}
		String request = "https://api.quizlet.com/2.0/sets/" + setID + "/terms?client_id=Adt4Xnyuaz&whitespace=1";
    	InputStream inputStream = null ;
        URL url;
        HttpURLConnection connection;
		try {
			url = new URL(request);
			connection = (HttpURLConnection) url.openConnection();
			if (connection.getResponseCode() >= 400 ) {
				inputStream = connection.getErrorStream();
		    }else{
		    	inputStream = connection.getInputStream();
		    }
			Reader reader = new InputStreamReader(inputStream,"UTF-8");
			JsonArray arr = new Gson().fromJson(reader,JsonArray.class);
			Iterator<JsonElement> list = arr.iterator();
			while(list.hasNext()) {
				JsonObject obj = (JsonObject) list.next();
				String term = obj.get("term").getAsString();
				String def = obj.get("definition").getAsString();
				ret.addTerm(term, def);
			}
			inputStream.close();
			connection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

}
