package smoothing;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class LMMercer {
public static String index = "/home/lennard/Schreibtisch/TIR_uebung/uebung1/Index";
	
	public static void main(String[] args) {	
		String line = "";
		for(int i = 0; i < args.length - 2; i++) {
			line = line.concat(args[i]);
			if(i < args.length - 3)line = line.concat(" ");
		}		 
		int numberOfResults = Integer.parseInt(args[args.length - 2]);
		final float alpha = Float.parseFloat(args[args.length-1]);
		
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer();	
			
			//System.out.println("Number of Documents: " + reader.numDocs());
			QueryParser parser = new QueryParser("contents", analyzer);
					   	
			Query query = null;
			   	
			if(line.matches(".+@\\d{4}-\\d{4}")){
				String keyword = line.split("@")[0];
				Query keywordQuery = parser.parse(keyword);
				
				BytesRef lowerBound = new BytesRef(line.split("@")[1].split("-")[0].trim());
				BytesRef upperBound = new BytesRef(line.split("@")[1].split("-")[1].trim());
				if(Integer.parseInt(lowerBound.utf8ToString()) <= Integer.parseInt(upperBound.utf8ToString())) {
					
					Query dateRangeQuery = new TermRangeQuery("dates", lowerBound, upperBound, true, true);
					
					BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
					bqBuilder.add(keywordQuery, BooleanClause.Occur.MUST);
					bqBuilder.add(dateRangeQuery, BooleanClause.Occur.MUST);
					query = bqBuilder.build();
					
					//System.out.println("Searching for: " + keyword + " from " + lowerBound.utf8ToString() + " to " + upperBound.utf8ToString());
				}	
				else {
			   		//System.out.println("The lower Bound must be smaller then the upper Bound!");
			   		return;
			   	}
				   	
		   	}
		   	
		   	else {
		   		query = parser.parse(line);
		   		//System.out.println("Searching for: " + line);
		   	}

			
			Similarity sim2 = new LMSimilarity(){

				@Override
				public String getName() {
					return "Mercer Smoothing";
				}

				@Override
				protected float score(BasicStats stats, float freq,
						float len) {
					return alpha * (freq/len) + (1 - alpha) * collectionModel.computeProbability(stats);
				}
				
			};
			
			
/*
sync3-20110712040101_2855
lk-20110817040101_3433
lk-20111209040101_365
sync3-20120327040102_2327
sync3-20120327040102_2070
*/		
		   	//searcher.setSimilarity(new LMJelinekMercerSimilarity(alpha));
			searcher.setSimilarity(sim2);		
		   	TopDocs results = searcher.search(query, numberOfResults);
		   	if(results.scoreDocs.length > 0) {
		   		for(ScoreDoc scoreDoc : results.scoreDocs){
		   			System.out.println(searcher.doc(scoreDoc.doc).get("id"));
		   		}
		   		//System.out.println("Number of results: " + searcher.count(query));
		   	} else {
		   		//System.out.println("No Document found");
		   	}		  	
			
		} catch (IOException | ParseException e) {
			
			e.printStackTrace();
		}

	}

}
