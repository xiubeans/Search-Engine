/**
 * @author Jasper Chen
 */
import java.util.*;

/*
 * Positional postings list 
 */
public class PstPosting implements Comparable<Posting>{
	int docID;
	int freq;
	// Position list
	ArrayList<Integer> positions = new ArrayList<Integer>();
	
	/**
	 * Constructor
	 * @param docID
	 * @param freq
	 */
	public PstPosting(int docID, int freq){
		this.docID = docID;
		this.freq = freq;
		this.positions = new ArrayList<Integer>();
	}
	
	/**
	 * ToString method
	 */
	public String toString(){
		return docID + " " + freq + "\n";
	}
	
	/**
	 * Enable sorting
	 */
	public int compareTo(Posting other){
		if(this.freq > other.freq)
			return 1;
		else if(this.freq == other.freq && this.docID > other.docID)
			return 1;
		else if(this.freq == other.freq && this.docID == other.docID)
			return 0;
		else 
			return -1;
	}
}
