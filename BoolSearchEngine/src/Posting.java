/**
 * @author Jasper Chen
 */
public class Posting implements Comparable<Posting>{
	// Internal docID
	int docID;
	// For ranked AND/OR operators, freq represent score
	float freq;		
	
	/**
	 * Constructor
	 * @param docID
	 * @param freq
	 */
	public Posting(int docID, float freq){
		this.docID = docID;
		this.freq = freq;
	}
	
	/**
	 * ToString method
	 */
	public String toString(){
		return docID + " " + freq + "\n";	
	}

	/**
	 * Enable sorting for Collections
	 * Finally with obey "frequency as 1st sorting factor, docID as the 2nd factor"
	 */
	public int compareTo(Posting other){
		if(this.freq > other.freq)
			return 1;
		else if(this.freq == other.freq && this.docID < other.docID)
			return 1;
		else 
			return -1;
	}
}
