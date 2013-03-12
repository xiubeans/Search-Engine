/**
 * @author Jasper Chen
 *
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/*
 * Node in the query tree
 */
public class Node {
	/**
	 * 0: Term
	 * 1: AND
	 * 2: OR
	 * 3+n: NEAR/n
	 */
	int type;
	String term;
	boolean isBody;
	ArrayList<Node> children;
	Node parent;
	
	/**
	 * Constructor for operator node
	 * @param type
	 * @param parent
	 */
	public Node(int type, Node parent){
		this.type = type;
		this.children = new ArrayList<Node>();
		this.parent = parent;
	}
	
	/**
	 * constructor for term node
	 * @param type
	 * @param term
	 * @param isBody
	 * @param parent
	 */
	public Node(int type, String term, boolean isBody, Node parent){
		this.type = type;
		this.term = term;
		this.isBody = isBody;
		this.children = null;
		this.parent = parent;
	}
	
	/**
	 * Read score list from file
	 * @return
	 * @throws Exception
	 */
	public ArrayList<Posting> ReadPostingFromFile() throws Exception{
		if(Stopwords.getInstance().containKey(this.term))
			return null;
		
		ArrayList<Posting> postings = new ArrayList<Posting>();
		String path = "";
		
		// Get the right path
		// Based on field: body OR title
		if(this.isBody)
			if(this.term.endsWith(".body"))
				path = ENV.BODYDIR + File.separator + this.term.substring(0, this.term.indexOf(".body")) + ".inv";
			else
				path = ENV.BODYDIR + File.separator + this.term + ".inv";
		else
			path = ENV.TITLEDIR + File.separator + this.term + ".inv";
		
		// Try opening the inverted list file
		// Return null if the term does not exist
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(path));	
		}catch(Exception e){
			if(br != null)
				br.close();
			return null;
		}
		String line = "";
		br.readLine();
		// Read file to memory
		while((line = br.readLine()) != null){
			int docID = Integer.parseInt(line.substring(0, line.indexOf(' ')));
			line = line.substring(line.indexOf(' ') + 1, line.length());
			int freq = Integer.parseInt(line.substring(0, line.indexOf(' ')));
			Posting pst = new Posting(docID, freq);
			postings.add(pst);
		}
		br.close();
		
		return postings;
	}
	
	/**
	 * Read positional posting list from file
	 * @return
	 * @throws Exception
	 */
	public ArrayList<PstPosting> ReadPstPostingFromFile() throws Exception{
		if(Stopwords.getInstance().containKey(this.term))
			return null;
		
		ArrayList<PstPosting> pstPostings = new ArrayList<PstPosting>();
		String path = "";
		
		// Get the right path
		// Based on field: body OR title
		if(this.isBody)
			path = ENV.BODYDIR + File.separator + this.term + ".inv";
		else
			path = ENV.TITLEDIR + File.separator + this.term + ".title.inv";
		
		// Try opening the inverted list file
		// Return null if the term does not exist
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(path));	
		}catch(Exception e){
			if(br != null)
				br.close();
			return null;
		}
		String line = "";
		br.readLine();
		// Read file to memory
		while((line = br.readLine()) != null){
			String[] args = line.split(" ");
			int docID = Integer.parseInt(args[0]);
			int freq = Integer.parseInt(args[1]);
			PstPosting pstP = new PstPosting(docID, freq);
			for(int i = 3; i < args.length; i++){
				pstP.positions.add(Integer.parseInt(args[i]));
			}
			pstPostings.add(pstP);
		}
		br.close();
		
		return pstPostings;
	}
}
