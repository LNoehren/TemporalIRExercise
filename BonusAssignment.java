package bonus_assignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.deeplearning4j.examples.nlp.word2vec.CreateEmbeddings;

public class BonusAssignment {

	public static String indexPath = "/home/lennard/Schreibtisch/TIR_uebung/uebung1/Index";
	public static String docTextPath = "/home/lennard/Schreibtisch/TIR_uebung/bonusAssData.txt";
	public static String qrelsPath = "/home/lennard/Schreibtisch/TIR_uebung/tir_formalrun_2014080829.qrels";

	public static String[] querys = {"Causes of stress",//0
									 "Weight loss",//1
									 "AIDS in Africa",//2
									 "Waterborne diseases in Africa",//3
									 "Obesity in children",//4
									 "Diabetes",//5
									 "Hair loss or baldness",//6
									 "English as a second language",//7
									 "Playing guitar",//8
									 "PlayStation 4"};//9

	public static String[] qids = {"001", "002", "003", "004", "005", "006", "007", "008", "009", "010"};
	
	public static Query newQuery;
	
	public static void main(String[] args) throws IOException, ParseException{

		String input = querys[Integer.parseInt(args[0])];
		String qid = qids[Integer.parseInt(args[0])];

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
		IndexSearcher searcher = new IndexSearcher(reader);

		//searching top 1000 documents for base query
		String [] arrIn = {input};
		TopDocs firstResult = searchIndex(arrIn, 1000, searcher);

		System.out.println("Precision@5: " + computePrecisionAtK(5, qid, firstResult, searcher));
		System.out.println("Precision@10: " + computePrecisionAtK(10, qid, firstResult, searcher));

		//writing documents in txt file
		Files.delete(Paths.get(docTextPath));
		for(ScoreDoc scoreDoc : firstResult.scoreDocs){
			writeTXT(searcher.doc(scoreDoc.doc).get("contents"));
		}

		//searching 5 similar words to query in word embeddings build with top 1000 docs
		System.out.println("Building word embeddings");
		Collection<String> similarWords = CreateEmbeddings.get5Similarwords(newQuery.toString());
		String[] newArrIn = similarWords.toArray(new String[similarWords.size()+1]);

		//putting original input at beginning of query array
		newArrIn[similarWords.size()] = newArrIn[0];
		newArrIn[0] = input;

		//searching top 10 results for improved query
		TopDocs newResult = searchIndex(newArrIn, 10, searcher);

		//computing precision for results
		System.out.println("Precision@5: " + computePrecisionAtK(5, qid, newResult, searcher));
		System.out.println("Precision@10: " + computePrecisionAtK(10, qid, newResult, searcher));

	}


	public static TopDocs searchIndex(String[] q, int resultCount, IndexSearcher searcher){
		try {

			Analyzer analyzer = new StandardAnalyzer();	

			QueryParser parser = new QueryParser("contents", analyzer);

			Query query = parser.parse(q[0]);
			BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
			bqBuilder.add(query, Occur.SHOULD);
			for(int i = 1; i < q.length; i++){
				query = parser.parse(q[i]);
				bqBuilder.add(query, Occur.SHOULD);
			}

			newQuery = bqBuilder.build();

			Similarity sim2 = new LMSimilarity(){

				@Override
				public String getName() {
					return "Dirichlet Smoothing";
				}

				@Override
				protected float score(BasicStats stats, float freq, float len) {

					return (freq + 1000 * collectionModel.computeProbability(stats)) / (len + 1000);
				}

			};

			searcher.setSimilarity(sim2);
			System.out.println("Searching for top " + resultCount + " results for Query: " + newQuery.toString());
			TopDocs results = searcher.search(newQuery, resultCount);

			return results;


		} catch (IOException | ParseException e) {

			e.printStackTrace();
		}
		return null;
	}

	public static void writeTXT(String text) throws IOException{

		FileOutputStream fos2 = new FileOutputStream(new File(docTextPath), true);
		fos2.write(text.getBytes());			
		fos2.close();
	}

	public static boolean idIsInQuery(String qId, String docId){
		try {
			FileReader fr = new FileReader(qrelsPath);
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			while((currentLine = br.readLine()) != null){
				String[] splitLine = currentLine.split(" ");
				if(splitLine[0].contains(qId) && 
						splitLine[1].equals(docId) &&
						!splitLine[2].equals("L0"))return true;
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static double computePrecisionAtK(int k, String qid, TopDocs results, IndexSearcher searcher) throws IOException{
		if(k > results.totalHits)k = results.totalHits;

		double result = 0.0;
		for(int i = 0; i < k; i++){
			String nmWord = searcher.doc(results.scoreDocs[i].doc).get("id");
			if(idIsInQuery(qid, nmWord))result++;
		}
		return result/k;
	}

}
