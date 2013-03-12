/**
 * Singleton class for stopwords
 * To keep the list of stop words 
 */
import java.io.*;
import java.util.*;

/*
 * All stop words in the system
 * Implemented as singleton
 */
public class Stopwords {
	Hashtable<String, String> stopwords;
	private static Stopwords instance = null;
	
	/**
	 * Initiate the stop words dictionary 
	 * @throws Exception
	 */
	protected Stopwords() throws Exception{
		/**
		 * Get the list of stop words
		 */
		BufferedReader sbr = null;
		try{
			sbr = new BufferedReader(new FileReader("stoplist.txt"));
		}catch(Exception e){	
			return;
		}
		stopwords = new Hashtable<String, String>();
		String word = "";
		while((word = sbr.readLine()) != null)
			if(!stopwords.containsKey(word))
				stopwords.put(word, "");
	}
	
	/**
	 * Get instance for this singleton
	 * @return
	 * @throws Exception
	 */
	public static Stopwords getInstance() throws Exception{
		if(instance == null)
			instance = new Stopwords();
		return instance;
	}
	
	public boolean containKey(String word){
		return this.stopwords.containsKey(word);
	}
}
