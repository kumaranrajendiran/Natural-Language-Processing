package com.restReview.sentimentAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.restReview.featureExtraction.FeatureExtraction;

/**
 * Class for analyzing the sentiment of a given text and summarizing the review
 * 
 * @author kumaran_rajendiran
 *
 */
public class SentimentAnalysisDriver {
	public enum OpinionType {
		GOOD, BAD, MODERATE, UNKNOWN
	}

	private FeatureExtraction featureExtraction;
	private DataClassification dataClassification;
	private TypedDependencies typeDependencies;
	private WordNetUtility wordNetUtility;
	private OpinionExtractor opinionExtractor;
	private String baseFolderPath;

	private static final String FOOD = "food";
	private static final String AMBIENCE = "ambience";
	private static final String SERVICE = "service";
	private static final String PRICE = "price";
	private Map<String, Double> dictionary;

	/**
	 * Utility method to tokenize the text into sentences
	 * @param text
	 * @return
	 */
	private List<String> tokenizeText(String text) {
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
		List<String> sentences = new LinkedList<String>();
		iterator.setText(text);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
				.next()) {
			String sentence = text.substring(start, end);
			sentence = sentence.replaceAll("[^A-Za-z0-9]", " ");
			sentence = sentence.replace("\\s+", " ").toLowerCase();
			sentences.add(sentence + " .");
		}
		return sentences;
	}

	/**
	 * Reference: http://sentiwordnet.isti.cnr.it/code/SentiWordNetDemoCode.java
	 * 
	 * @param baseFolderPath
	 * @throws IOException
	 */
	public void loadScoreDictionary() throws IOException {
		dictionary = new HashMap<String, Double>();
		HashMap<String, HashMap<Integer, Double>> tempDict = new HashMap<String, HashMap<Integer, Double>>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(
				baseFolderPath + File.separator + "ModelBuilder"
						+ File.separator + "SentiWordNet.txt"));
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if (!line.trim().startsWith("#")) {
				String[] data = line.split("\t");
				String type = data[0];
				Double score = Double.parseDouble(data[2])
						- Double.parseDouble(data[3]);
				String[] parts = data[4].split(" ");
				for (String part : parts) {
					String[] rankData = part.split("#");
					String term = rankData[0] + "_" + type;

					int rank = Integer.parseInt(rankData[1]);
					if (!tempDict.containsKey(term)) {
						tempDict.put(term, new HashMap<Integer, Double>());
					}
					tempDict.get(term).put(rank, score);
				}
			}
		}
		bufferedReader.close();

		for (Map.Entry<String, HashMap<Integer, Double>> entry : tempDict
				.entrySet()) {
			String word = entry.getKey();
			Map<Integer, Double> synSetScoreMap = entry.getValue();
			double score = 0.0;
			double sum = 0.0;
			for (Map.Entry<Integer, Double> setScore : synSetScoreMap
					.entrySet()) {
				score += setScore.getValue() / (double) setScore.getKey();
				sum += 1.0 / (double) setScore.getKey();
			}
			score /= sum;
			dictionary.put(word, score);
		}

	}

	/**
	 * Setup method for basic initialization
	 * @param baseFolderPath
	 * @throws IOException
	 */
	public void setup(String baseFolderPath) throws IOException {

		this.baseFolderPath = baseFolderPath;
		loadScoreDictionary();

		System.out.println("Initializing the word net dictionary...");
		wordNetUtility = new WordNetUtility();
		wordNetUtility.setup(baseFolderPath);

		System.out
				.println("Training the data classifier and building the model...");
		dataClassification = new DataClassification();
		dataClassification.train(baseFolderPath + File.separator
				+ "ModelBuilder");

		System.out
				.println("Starting the Feature Extraction for the training data...");
		featureExtraction = new FeatureExtraction();
		featureExtraction.setup();
		System.out.println("Extracting features from bad reviews...");
		featureExtraction.extractBadReviewFeatures(baseFolderPath
				+ File.separator + "DataSegFolder", wordNetUtility, dictionary);
		System.out.println("Extracting features from good reviews...");
		featureExtraction.extractGoodReviewFeatures(baseFolderPath
				+ File.separator + "DataSegFolder", wordNetUtility, dictionary);
		System.out.println("Extracting features from moderate reviews...");
		featureExtraction.extractModReviewFeatures(baseFolderPath
				+ File.separator + "DataSegFolder", wordNetUtility, dictionary);

		System.out.println("Initializing the Lexicalized Parser...");
		typeDependencies = new TypedDependencies();
		typeDependencies.setup();
		
		System.out.println("Initializing the Opinion Extractor...");
		opinionExtractor = new OpinionExtractor();
		opinionExtractor.setup();

	}

	/**
	 * Method to evalaute the opinion and find the sentiment orientation
	 * @param opinion
	 * @return
	 */
	private OpinionType evaluateOpinion(String opinion) {
		boolean negatedWord = false;
		if (opinion.startsWith("~")) {
			negatedWord = true;
			opinion = opinion.substring(1);
		}
		Set<String> realtedWords = wordNetUtility.getRelatedWords(opinion);
		for (String word : realtedWords) {
			if (featureExtraction.adjInBadReviews.contains(word)) {
				if (negatedWord == true) {
					return OpinionType.MODERATE;
				}
				return OpinionType.BAD;
			}
			if (featureExtraction.adjInGoodReviews.contains(word)) {
				if (negatedWord == true) {
					return OpinionType.BAD;
				}
				return OpinionType.GOOD;
			}
			if (featureExtraction.adjInModReviews.contains(word)) {
				if (negatedWord == true) {
					return OpinionType.BAD;
				}
				return OpinionType.MODERATE;
			}

		}
		// If it is a new word, then determine the score using dictionary
		if (dictionary.containsKey(opinion + "_a")) {
			if (dictionary.get(opinion + "_a") <= 0.0D) {
				if (negatedWord == true) {
					return OpinionType.MODERATE;
				}
				return OpinionType.BAD;
			} else if (dictionary.get(opinion + "_a") > 0.0D
					&& dictionary.get(opinion + "_a") <= 0.3D) {
				if (negatedWord == true) {
					return OpinionType.BAD;
				}
				return OpinionType.MODERATE;
			} else {
				if (negatedWord == true) {
					return OpinionType.BAD;
				}
				return OpinionType.GOOD;
			}
		}
		return OpinionType.UNKNOWN;
	}

	/**
	 * Method the analyze the opinions and determine the sentiment of a feature
	 * @param features
	 */
	private void analyzeFeatures(List<String> features) {
		int goodOpinionCnt = 0;
		int badOpinionCnt = 0;
		int modOpinionCnt = 0;
		for (String feature : features) {
			System.out.println("Analyzing the sentence : " + feature);
			Set<String> opinions = opinionExtractor
					.extractOpinion(typeDependencies
							.getTypedDependencies(feature));
			System.out.println("Useful opinions : " + opinions);
			for (String opinion : opinions) {
				OpinionType opinionType = evaluateOpinion(opinion);
				if (opinionType.equals(OpinionType.GOOD)) {
					goodOpinionCnt++;
				} else if (opinionType.equals(OpinionType.BAD)) {
					badOpinionCnt++;
				} else if (opinionType.equals(OpinionType.MODERATE)) {
					modOpinionCnt++;
				}
			}
		}
		if (badOpinionCnt == goodOpinionCnt) {
			System.out.println("Feature has been rated 'MODERATE'");
		} else if (badOpinionCnt >= modOpinionCnt
				&& badOpinionCnt >= goodOpinionCnt) {
			System.out.println("Feature has been rated 'BAD'");
		} else if (modOpinionCnt >= goodOpinionCnt) {
			System.out.println("Feature has been rated 'MODERATE'");
		} else {
			System.out.println("Feature has been rated 'GOOD'");
		}
	}

	/**
	 * Utility method to print the features
	 * @param features
	 */
	private void printFeatures(List<String> features) {
		for (String feature : features) {
			System.out.println(feature);
		}
	}

	/**
	 * Driver  method to determine the summary of the review
	 * @param text
	 * @throws IOException
	 */
	public void summarizeReview(String text) throws IOException {

		List<String> foodFeatures = new LinkedList<String>();
		List<String> serviceFeatures = new LinkedList<String>();
		List<String> ambienceFeatures = new LinkedList<String>();
		List<String> priceFeatures = new LinkedList<String>();

		System.out
				.println("\n\nParsing the text and splitting it into sentences");
		List<String> features = tokenizeText(text);
		for (String feature : features) {
			String category = dataClassification.classifyText(baseFolderPath
					+ File.separator + "ModelBuilder", feature);
			if (category.equals(FOOD)) {
				foodFeatures.add(feature);
			} else if (category.equals(AMBIENCE)) {
				ambienceFeatures.add(feature);
			} else if (category.equals(SERVICE)) {
				serviceFeatures.add(feature);
			} else if (category.equals(PRICE)) {
				priceFeatures.add(feature);
			}
		}

		System.out.println("\nUseful features:\n");
		if (!foodFeatures.isEmpty()) {
			System.out
					.println("The following sentences are tagged to label 'FOOD'");
			printFeatures(foodFeatures);
			System.out.println("");
		}
		if (!ambienceFeatures.isEmpty()) {
			System.out
					.println("The following sentences are tagged to label 'AMBIENCE'");
			printFeatures(ambienceFeatures);
			System.out.println("");
		}
		if (!serviceFeatures.isEmpty()) {
			System.out
					.println("The following sentences are tagged to label 'SERVICE'");
			printFeatures(serviceFeatures);
			System.out.println("");
		}
		if (!priceFeatures.isEmpty()) {
			System.out
					.println("The following sentences are tagged to label 'PRICE'");
			printFeatures(priceFeatures);
			System.out.println("");
		}

		System.out.println("Analyzing the features:\n");
		if (!foodFeatures.isEmpty()) {
			System.out.println("Analyzing the 'FOOD' aspects of the review");
			analyzeFeatures(foodFeatures);
			System.out.println("");
		}
		if (!ambienceFeatures.isEmpty()) {
			System.out
					.println("Analyzing the 'AMBIENCE' aspects of the review");
			analyzeFeatures(ambienceFeatures);
			System.out.println("");
		}
		if (!serviceFeatures.isEmpty()) {
			System.out
					.println("Analyzing the 'SERVICE' aspects of the review'");
			analyzeFeatures(serviceFeatures);
			System.out.println("");
		}
		if (!priceFeatures.isEmpty()) {
			System.out.println("Analyzing the 'PRICE' aspects of the review");
			analyzeFeatures(priceFeatures);
			System.out.println("");
		}
	}

	public static void main(String[] args) throws IOException {
		if(args.length!=1){
			System.out.println("Please provide the base folder path");
			System.exit(0);
		}
		SentimentAnalysisDriver driver = new SentimentAnalysisDriver();
		driver.setup(args[0]);
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(System.in));
		String input;
		while (true) {
			System.out.println("\nEnter the review...");
			input = bufferedReader.readLine();
			if (input != null && !input.isEmpty()) {
				driver.summarizeReview(input);
			}
			System.out.println("\nEnter '1' to continue...");
			input = bufferedReader.readLine();
			if (!input.trim().equals("1")) {
				break;
			}
		}
		System.out.println("Please run the program to analyze the reviews...");
		bufferedReader.close();
	}
}
