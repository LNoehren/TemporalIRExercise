package org.deeplearning4j.examples.nlp.word2vec;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;


public class CreateEmbeddings {

    public static void main(String[] args) throws Exception {
    	int dimension = Integer.parseInt(args[0]);
    	int WindowSize = Integer.parseInt(args[1]);
    	String algorithm = args[2];//SkipGram oder CBOW
    	buildModel(dimension, WindowSize, algorithm);
      
    }
    
    public static Word2Vec buildModel(int dimension, int windowSize, String alg) throws FileNotFoundException{
    	
    	String filePath = "/home/lennard/Schreibtisch/TIR_uebung/bonusAssData.txt";
        
        SentenceIterator iter = new BasicLineIterator(filePath);
        TokenizerFactory t = new DefaultTokenizerFactory();

        t.setTokenPreProcessor(new CommonPreprocessor());

        Word2Vec vec;
        vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(1)
                .layerSize(dimension)
                .seed(42)
                .windowSize(windowSize)
                .iterate(iter)
                .tokenizerFactory(t)
                .elementsLearningAlgorithm("org.deeplearning4j.models.embeddings.learning.impl.elements." + alg)
                .build();

        vec.fit();

        //String outPath = "/home/lennard/Schreibtisch/TIR_uebung/bonusModel_d" + dimension + "_w" + windowSize + "_alg=" + alg + ".txt";
        
        // Write word vectors to file
        //WordVectorSerializer.writeFullModel(vec, outPath);
        
        return vec;
    }
    
    public static Collection<String> get5Similarwords(String query) throws FileNotFoundException{
    	Pattern pattern = Pattern.compile("contents:(.*?)\\W");
		Matcher matcher = pattern.matcher(query);   	
    	
		Word2Vec vec = buildModel(500, 5, "SkipGram");
		Collection<String> newList = vec.wordsNearest("", 0);
		while(matcher.find()){
			newList.addAll(vec.wordsNearest(matcher.group(1), 5));
		}
		System.out.println("Nearest words to " + query + ": " + newList);
		
		return newList;
	
    }
}
