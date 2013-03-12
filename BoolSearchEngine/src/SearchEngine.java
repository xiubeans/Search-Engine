/**
 * @author Jasper Chen
 *
 */
import java.util.*;
import java.io.*;

/*
 * The search engine 
 */
public class SearchEngine {	
	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Loop to read query one by one from file
		//		Parse the query to build a query tree
		//		Retrieve information
		
		// Arguments validation and extraction
		if(args.length < 2){
			System.out.print("Usage: SearchEngine query_file_path 1/2\nin which 1 means ranked retrieval and 2 mean unranked retrieval\n");
			return;
		}
		String queryPath = args[0];
		BufferedReader qbr = null;
		try{
			qbr = new BufferedReader(new FileReader(queryPath));
		}catch(Exception e){
			System.out.print("Invalid query file\n");
		}
		boolean mode;
		if(args[1].equals("1"))
			mode = ENV.RANKED;
		else if(args[1].equals("2"))
			mode = ENV.UNRANKED;
		else{
			System.out.print("Usage: SearchEngine query_file_path 1/2\nin which 1 means ranked retrieval and 2 mean unranked retrieval\n");
			return;
		}
		
		// Main loop
		String query = "";
		long startTime = System.currentTimeMillis();
		ArrayList<ArrayList<Posting>> allResults = new ArrayList<ArrayList<Posting>>();
		while((query = qbr.readLine()) != null){
			Node root = Parse(query);
			ArrayList<Posting> curPostings = Evaluate(root, mode);
			ArrayList<Posting> results = new ArrayList<Posting>();
			// Ranked retrieval
			if(mode == ENV.RANKED){
				if(curPostings != null){
					// Sort first
					Collections.sort(curPostings);
					for(int i = curPostings.size() - 1; i > 0 && i > curPostings.size() - 101; i--)
						results.add(curPostings.get(i));
					allResults.add(results);
				}
				else{
					curPostings = new ArrayList<Posting>();
					curPostings.add(new Posting(-1,0));
					allResults.add(results);
				}
			}
			// Unranked retrieval
			else{
				if(curPostings != null){
					for(int i = curPostings.size() - 1; i > 0 && i > curPostings.size() - 101; i--)
						results.add(curPostings.get(i));
					allResults.add(results);
				}
				else{
					curPostings = new ArrayList<Posting>();
					curPostings.add(new Posting(-1,0));
					allResults.add(results);
				}
			}
		}
		long endTime = System.currentTimeMillis();
		// Calculate time
		System.out.print(endTime - startTime);
		// Dump results to local file
		Dump(allResults);
	
	}
	
	/**
	 * Parse the query
	 * @param query
	 * @return
	 */
	public static Node Parse(String query) throws Exception{
		ArrayList<String> tokens = new ArrayList<String>();
		Node root = null;
		Node lastNode = null;
		String token;
		int startIndex = 0;
		
		// Default operator is OR
		if(query.charAt(0) != '#'){
			tokens.add("#OR");
		}
		
		// Loop to read next token
		while((token = nextToken(query.substring(startIndex, query.length()))) != null){
			tokens.add(token);
			// Deal with next token index
			query = query.substring(startIndex + token.length(), query.length());
			if(query.length() == 0)
				break;
			int steps = 0;
			while(query.charAt(steps) == ' '){
				steps++;
			}
			query = query.substring(steps, query.length());
			if(query.length() == 0)
				break;
		}
		
		// Delete stopwords
		for(int i = 0; i < tokens.size(); i++){
			if(Stopwords.getInstance().containKey(tokens.get(i))){
				tokens.remove(i);
			}
		}
		
		// Build the expression tree
		root = BuildExprTree(tokens);
		
		return root;
	}
	
	/**
	 * Build the expression tree based on the prefix expression
	 * @param tokens
	 * @return
	 */
	public static Node BuildExprTree(ArrayList<String> tokens){
		Node root = null;
		Node cur = null;
		Node parentNode = null;
		boolean isLastOp = false;
		
		// Loop to take next token
		for(int i = 0; i < tokens.size(); i++){
			// Skip some nodes
			if(tokens.get(i).equals("("))
				continue;
			else if(tokens.get(i).equals(")")){
				parentNode = parentNode.parent;
				if(parentNode == null)
					break;
				else
					continue;
			}
			
			// Root node
			if(i == 0){
				root = GiveMeNode(tokens.get(i));
				parentNode = root;
				isLastOp = true;
			}
			// non-root node
			else{
				cur = GiveMeNode(tokens.get(i));
				cur.parent = parentNode;
				parentNode.children.add(cur);
				if(cur.type > 0)
					parentNode = cur;
			}			
		}
		
		return root;
	}
	
	/**
	 * Generate a node based on token
	 * @param token
	 * @return
	 */
	public static Node GiveMeNode(String token){
		Node node = null;
		
		// AND op
		if(token.equals("#AND"))
			node = new Node(1, null);
		// OR op
		else if(token.equals("#OR"))
			node = new Node(2, null);
		// NEAR/n op
		else if(token.startsWith("#NEAR"))
			node = new Node(3 + Integer.parseInt(token.substring(token.indexOf('/') + 1, token.length())), null);
		// Term
		else{
			// If title field term
			if(token.endsWith(".title"))
				node = new Node(0, token, ENV.TITLE, null);
			// Else bod field term
			else{
				// Take "A-B" as "#NEAR/1(A B)"
				if(token.contains("-")){
					node = new Node(4, null);
					String[] terms = token.split("-");
					for(int i = 0; i < terms.length; i++){
						Node term = new Node(0, terms[i], ENV.BODY, null);
						term.parent = node;
						node.children.add(term);
					}
				}
				// Take "A\B" and "A/B" as "#OR(A B)"
				else if(token.contains("/") || token.contains("\\")){
					node = new Node(2, null);
					String[ ] terms = null;
					if(token.contains("/"))
						terms = token.split("/");
					else
						terms = token.split("\\");
					for(int i = 0; i < terms.length; i++){
						Node term = new Node(0, terms[i], ENV.BODY, null);
						term.parent = node;
						node.children.add(term);
					}
				}
				// Normal node
				else
					node = new Node(0, token, ENV.BODY, null);
			}
		}
		
		return node;
	}
	
	
	/**
	 * Return next token in the string
	 * @param str
	 * @param startIndex
	 * @return
	 */
	public static String nextToken(String str){
		// If string is empty
		if(str.length() == 0)
			return null;
		
		String token = null;
		char startChar = str.charAt(0);
		
		// If next token is an operator
		if(startChar == '#'){
			if(str.startsWith("#AND"))
				token = "#AND";
			else if(str.startsWith("#OR"))
				token = "#OR";
			else{
				int endIndex = str.indexOf('(');
				token = str.substring(0, endIndex);
			}
		}
		// If next token is (
		else if(startChar == '(')
			token = "(";
		// If next token is )
		else if(startChar == ')')
			token = ")";
		// If next token is a term
		else{
			if(str.indexOf(' ') < 0 && str.indexOf(')') < 0)
				token = str.substring(0, str.length());
			else if(str.indexOf(' ') > 0 && str.indexOf(')') > 0){
				int endIndex = (str.indexOf(' ') < str.indexOf(')')) ? str.indexOf(' ') : str.indexOf(')');
				token = str.substring(0, endIndex);
			}
			else if(str.indexOf(' ') > 0)
				token = str.substring(0, str.indexOf(' '));
			else
				token = str.substring(0, str.indexOf(')'));
		}
		
		return token;
	}
	
	/**
	 * Dump all results to local file inorder to evaluate
	 * @param finals
	 */
	public static void Dump(ArrayList<ArrayList<Posting>> allResults) throws Exception{
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new FileWriter("report.txt"));
		}catch(Exception e){
			return;
		}
		
		// Dump query by query
		for(int i = 0; i < allResults.size(); i++){
			for(int j = 0; j < allResults.get(i).size(); j++){
				int queryNum = i + 1;
				int postNum = j + 1;
				String line = queryNum + " Q0 " + allResults.get(i).get(j).docID + " " 
						+ postNum + " " + allResults.get(i).get(j).freq + " run-1";
				bw.write(line);
				bw.newLine();
			}
		}
		bw.close();
	}
	
	/**
	 * Evaluate the query, as well as sort the results if required
	 * @param root: the root of the query tree
	 * @param mode: ranked = true; unranked = false
	 * @return Ranked/Unranked results, according to "mode" parameter
	 * @throws Exception
	 */
	public static ArrayList Evaluate(Node root, boolean mode) throws Exception{
		ArrayList mergedPostings = null;
		// Base case
		if(root.children == null){
			if(root.parent.type < 3)
				mergedPostings = root.ReadPostingFromFile();
			else
				mergedPostings = root.ReadPstPostingFromFile();
		}
		
		// Recursive Cases
		// Ranked mode
		else if(mode == ENV.RANKED){
			// AND node
			if(root.type == 1){
				mergedPostings = Evaluate(root.children.get(0), mode);
				for(int i = 1; i < root.children.size(); i++){
					ArrayList<Posting> nextPostings = Evaluate(root.children.get(i), mode);
					mergedPostings = ANDMergeRanked(mergedPostings, nextPostings);
					int k = 0;
					k++;
				}
				//Collections.sort(mergedPostings);
			}
			// OR node
			else if(root.type == 2){				
				mergedPostings = Evaluate(root.children.get(0), mode);
				for(int i = 1; i < root.children.size(); i++){
					ArrayList<Posting> nextPostings = Evaluate(root.children.get(i), mode);
					mergedPostings = ORMergeRanked(mergedPostings, nextPostings);
				}
				//Collections.sort(mergedPostings);
			}
			// NEAR/n operation
			else{
				int n = root.type - 3;
				ArrayList<PstPosting> tempPostings1 = Evaluate(root.children.get(0), mode);
				ArrayList<PstPosting> tempPostings2 = null;
				for(int i = 1; i < root.children.size(); i++){
					tempPostings2 = Evaluate(root.children.get(i), mode);
					tempPostings1 = NEARMerge(tempPostings1, tempPostings2, n);
				}
				if(tempPostings1 == null)
					return null;
				mergedPostings = new ArrayList<Posting>();
				for(int i = 0; i < tempPostings1.size(); i++)
					mergedPostings.add(new Posting(tempPostings1.get(i).docID, tempPostings1.get(i).freq));	
				//Collections.sort(mergedPostings);
			}
		}
		// Unranked mode
		else{
			// AND node
			if(root.type == 1){
				mergedPostings = Evaluate(root.children.get(0), mode);
				for(int i = 1; i < root.children.size(); i++){
					ArrayList<Posting> nextPostings = Evaluate(root.children.get(i), mode);
					mergedPostings = ANDMergeUnranked(mergedPostings, nextPostings);
					int k = 0;
					k++;
				}
			}
			// OR node
			else if(root.type == 2){
				mergedPostings = Evaluate(root.children.get(0), mode);
				for(int i = 1; i < root.children.size(); i++){
					ArrayList<Posting> nextPostings = Evaluate(root.children.get(i), mode);
					mergedPostings = ORMergeUnranked(mergedPostings, nextPostings);
				}
			}
			// NEAR/n operation
			else{
				int n = root.type - 3;
				ArrayList<PstPosting> tempPostings1 = Evaluate(root.children.get(0), mode);
				ArrayList<PstPosting> tempPostings2 = null;
				for(int i = 1; i < root.children.size(); i++){
					tempPostings2 = Evaluate(root.children.get(i), mode);
					tempPostings1 = NEARMergeUnranked(tempPostings1, tempPostings2, n);
				}
				mergedPostings = new ArrayList<Posting>();
				for(int i = 0; i < tempPostings1.size(); i++)
					mergedPostings.add(new Posting(tempPostings1.get(i).docID, tempPostings1.get(i).freq));	
			}
		}
		
		//Collections.sort(mergedPostings);
		return mergedPostings;
	}
	
	/**
	 * AND operation
	 * Merge the intermediate postings with next postings, rank results
	 * @param tempPostings
	 * @param nextPostings
	 * @return
	 */
	public static ArrayList<Posting> ANDMergeRanked(ArrayList<Posting> tempPostings, ArrayList<Posting> nextPostings){
		if(tempPostings == null || nextPostings == null)
			return null;
		
		ArrayList<Posting> mergedPostings  = new ArrayList<Posting>();
		
		// Loop to merge
		// Take MIN way to score
		for(int i = 0, j = 0; i < tempPostings.size() && j < nextPostings.size(); ){
			if(tempPostings.get(i).docID == nextPostings.get(j).docID){
				tempPostings.get(i).freq = (tempPostings.get(i).freq < nextPostings.get(j).freq) ? tempPostings.get(i).freq : nextPostings.get(j).freq;
				mergedPostings.add(tempPostings.get(i));
				i++;
			}
			else if(tempPostings.get(i).docID < nextPostings.get(j).docID)
				i++;
			else
				j++;			
		}
		
		return mergedPostings;
	}
	
	/**
	 * OR operation
	 * Merge the intermediate postings with next postings, rank results
	 * @param tempPostings
	 * @param nextPostings
	 * @return
	 */
	public static ArrayList<Posting> ORMergeRanked(ArrayList<Posting> tempPostings, ArrayList<Posting> nextPostings){
		if(tempPostings == null && nextPostings != null)
			return nextPostings;
		else if(tempPostings != null && nextPostings == null)
			return tempPostings;
		else if(tempPostings == null && nextPostings == null)
			return null;
		
		ArrayList<Posting> mergedPostings  = new ArrayList<Posting>();
		
		// Loop to merge
		// Take MAX way to score
		int i, j;
		for(i = 0, j = 0; i < tempPostings.size() && j < nextPostings.size(); ){
			if(tempPostings.get(i).docID == nextPostings.get(j).docID){
				tempPostings.get(i).freq = (tempPostings.get(i).freq > nextPostings.get(j).freq) ? tempPostings.get(i).freq : nextPostings.get(j).freq;
				mergedPostings.add(tempPostings.get(i));
				i++;
				j++;
			}
			else if(tempPostings.get(i).docID < nextPostings.get(j).docID){
				mergedPostings.add(tempPostings.get(i));
				i++;
			}
			else{
				mergedPostings.add(nextPostings.get(j));
				j++;
			}
		}
		while(i < tempPostings.size())
			mergedPostings.add(tempPostings.get(i++));
		while(j < nextPostings.size())
			mergedPostings.add(nextPostings.get(j++));

		return mergedPostings;
	}
	
	/**
	 * NEAR/n operation
	 * Merge the intermediate postings with next postings, rank result
	 * @param tempPostings
	 * @param nextPostings
	 * @return 
	 */
	public static ArrayList<PstPosting> NEARMerge(ArrayList<PstPosting> tempPostings1, ArrayList<PstPosting> tempPostings2, int n){
		if(tempPostings1 == null || tempPostings2 == null)
			return null;
		
		ArrayList<PstPosting> intPostings  = new ArrayList<PstPosting>();
		
		// Loop to merge
		// Take number of NEAR matches to score
		for(int i = 0, j = 0; i < tempPostings1.size() && j < tempPostings2.size(); ){
			if(tempPostings1.get(i).docID == tempPostings2.get(j).docID){
				// Calculate frequency
				int freq = 0;
				PstPosting cur = null;
				ArrayList<Integer> posList1 = tempPostings1.get(i).positions;
				ArrayList<Integer> posList2 = tempPostings2.get(j).positions;
				for(int p = 0, q = 0; p < posList1.size() && q < posList2.size(); ){
					if(posList2.get(q) - posList1.get(p) <= n && posList2.get(q) - posList1.get(p) > 0){
						freq++;
						if(freq == 1){
							cur = new PstPosting(tempPostings1.get(i).docID, 1);
							cur.positions.add(posList2.get(q));
							intPostings.add(cur);
						}
						else{
							cur.positions.add(posList2.get(q));	
							cur.freq++;
						}
						p++;
					}
					else if(posList1.get(p) < posList2.get(q))
						p++;
					else
						q++;
				}
				if(freq > 0)
				i++;
				j++;
			}
			else if(tempPostings1.get(i).docID < tempPostings2.get(j).docID)
				i++;
			else
				j++;
		}
		
		return intPostings;
	}
	
	/**
	 * AND operation
	 * Merge the intermediate postings with next postings, do NOT rank results
	 * @param tempPostings
	 * @param nextPostings
	 * @return
	 */
	public static ArrayList<Posting> ANDMergeUnranked(ArrayList<Posting> tempPostings, ArrayList<Posting> nextPostings){
		if(tempPostings == null || nextPostings == null)
			return null;
		
		ArrayList<Posting> mergedPostings  = new ArrayList<Posting>();
		
		// Loop to merge
		for(int i = 0, j = 0; i < tempPostings.size() && j < nextPostings.size(); ){
			if(tempPostings.get(i).docID == nextPostings.get(j).docID){
				mergedPostings.add(tempPostings.get(i));
				i++;
			}
			else if(tempPostings.get(i).docID < nextPostings.get(j).docID)
				i++;
			else
				j++;			
		}
		
		return mergedPostings;
	}
	
	/**
	 * OR operation
	 * Merge the intermediate postings with next postings, do NOT rank results
	 * @param tempPostings
	 * @param nextPostings
	 * @return
	 */
	public static ArrayList<Posting> ORMergeUnranked(ArrayList<Posting> tempPostings, ArrayList<Posting> nextPostings){
		if(tempPostings == null && nextPostings != null)
			return nextPostings;
		else if(tempPostings != null && nextPostings == null)
			return tempPostings;
		else if(tempPostings == null && nextPostings == null)
			return null;
		
		ArrayList<Posting> mergedPostings  = new ArrayList<Posting>();
		
		// Loop to merge
		int i, j;
		for(i = 0, j = 0; i < tempPostings.size() && j < nextPostings.size(); ){
			if(tempPostings.get(i).docID == nextPostings.get(j).docID){
				mergedPostings.add(tempPostings.get(i));
				i++;
				j++;
			}
			else if(tempPostings.get(i).docID < nextPostings.get(j).docID){
				mergedPostings.add(tempPostings.get(i));
				i++;
			}
			else{
				mergedPostings.add(nextPostings.get(j));
				j++;
			}
		}
		while(i < tempPostings.size())
			mergedPostings.add(tempPostings.get(i++));
		while(j < nextPostings.size())
			mergedPostings.add(nextPostings.get(j++));

		return mergedPostings;
	}
	
	/**
	 * NEAR/n operation
	 * Merge the intermediate postings with next postings, do NOT rank result
	 * @param tempPostings
	 * @param nextPostings
	 * @return 
	 */
	public static ArrayList<PstPosting> NEARMergeUnranked(ArrayList<PstPosting> tempPostings1, ArrayList<PstPosting> tempPostings2, int n){
		ArrayList<PstPosting> intPostings  = new ArrayList<PstPosting>();
		
		// Loop to merge
		for(int i = 0, j = 0; i < tempPostings1.size() && j < tempPostings2.size(); ){
			if(tempPostings1.get(i).docID == tempPostings2.get(j).docID){
				// Calculate frequency
				int freq = 0;
				PstPosting cur = null;
				ArrayList<Integer> posList1 = tempPostings1.get(i).positions;
				ArrayList<Integer> posList2 = tempPostings2.get(j).positions;
				for(int p = 0, q = 0; p < posList1.size() && q < posList2.size(); ){
					if(posList2.get(q) - posList1.get(p) <= n && posList2.get(q) - posList1.get(p) > 0){
						freq++;
						if(freq == 1){
							cur = new PstPosting(tempPostings1.get(i).docID, 1);
							cur.positions.add(posList2.get(q));
							intPostings.add(cur);
						}
						else{
							cur.positions.add(posList2.get(q));	
							cur.freq++;
						}
						p++;
					}
					else if(posList1.get(p) < posList2.get(q))
						p++;
					else
						q++;
				}
				if(freq > 0)
				i++;
				j++;
			}
			else if(tempPostings1.get(i).docID < tempPostings2.get(j).docID)
				i++;
			else
				j++;
		}
		
		return intPostings;
	}


}
