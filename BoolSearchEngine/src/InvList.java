/*
 * @auther Jasper Chen
 */

/*
 * Meta-data of an inverted list
 */
public class InvList implements Comparable<InvList>{
	String term;
	String stemmdTerm;
	int postingCount;
	
	public InvList(String term, String stemmedTerm, int postingCount){
		this.term = term;
		this.stemmdTerm = stemmedTerm;
		this.postingCount = postingCount;
	}
	
	public int compareTo(InvList other){
		if(this.postingCount < other.postingCount)
			return -1;
		else if(this.postingCount == other.postingCount)
			return 0;
		else
			return 1;
	}
}
