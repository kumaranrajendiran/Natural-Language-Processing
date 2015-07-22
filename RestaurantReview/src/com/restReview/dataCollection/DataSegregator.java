package com.restReview.dataCollection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Class used to segregate the restaurant reviews from yelp dataset
 * 
 * @author kumaran_rajendiran
 *
 */
public class DataSegregator {
	public static Set<String> restaurantIdSet;
	public static MaxentTagger tagger;

	public final static String BUSINESS_ID = "business_id";
	public final static String CATEGORIES = "categories";
	public final static String TEXT = "text";
	public final static String STARS = "stars";

	public static void setup(String modelFolderPath) {
		tagger = new MaxentTagger(modelFolderPath + File.separator
				+ "english-left3words-distsim.tagger");
		restaurantIdSet = new HashSet<String>();
	}

	public static void cleanup() {
		restaurantIdSet = null;
	}

	public static List<String> tokenizeText(String text) {
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
		List<String> sentences = new LinkedList<String>();
		iterator.setText(text);
		int start = iterator.first();
		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
				.next()) {
			sentences.add(text.substring(start, end));
		}
		return sentences;
	}

	public static void populateRestaurantId(String businessInfoFilePath)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new FileReader(new File(
				businessInfoFilePath)));
		String line = "";
		while ((line = buffReader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			} else {
				JsonElement jelement = new JsonParser().parse(line);
				JsonObject jobject = jelement.getAsJsonObject();
				JsonElement businessIdElement = jobject.get(BUSINESS_ID);
				JsonArray categoriesArray = jobject.get(CATEGORIES)
						.getAsJsonArray();
				for (int iterator = 0; iterator < categoriesArray.size(); iterator++) {
					if (categoriesArray.get(iterator).getAsString()
							.toLowerCase().contains("restaurants")
							|| categoriesArray.get(iterator).getAsString()
									.toLowerCase().contains("nightlife")) {
						restaurantIdSet.add(businessIdElement.getAsString());
						break;
					}
				}
			}
		}
		buffReader.close();
	}

	public static void seggregateRestaurantReviews(String reviewFilePath,
			String outputFolderPath) throws IOException {
		BufferedReader buffReader = new BufferedReader(new FileReader(new File(
				reviewFilePath)));
		FileWriter badRevfileWriter = new FileWriter(new File(outputFolderPath
				+ File.separator + "Bad.txt"));
		FileWriter modRevfileWriter = new FileWriter(new File(outputFolderPath
				+ File.separator + "Moderate.txt"));
		FileWriter goodRevfileWriter = new FileWriter(new File(outputFolderPath
				+ File.separator + "Good.txt"));
		String line = "";
		while ((line = buffReader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			} else {
				JsonElement jelement = new JsonParser().parse(line);
				JsonObject jobject = jelement.getAsJsonObject();
				JsonElement businessIdElement = jobject.get(BUSINESS_ID);
				if (restaurantIdSet.contains(businessIdElement.getAsString())) {
					JsonElement starsElement = jobject.get(STARS);
					JsonElement reviewElement = jobject.get(TEXT);
					if (starsElement.getAsInt() < 3) {
						List<String> sentences = tokenizeText(reviewElement
								.getAsString());
						for (String sentence : sentences) {
							sentence = sentence.replaceAll("[^A-Za-z0-9-]", " ");
							sentence = sentence.replace("\\s+", " ");
							String taggedSentence = tagger.tagString(sentence);
							badRevfileWriter.append(taggedSentence+"\n");
						}

					} else if (starsElement.getAsInt() == 3) {
						List<String> sentences = tokenizeText(reviewElement
								.getAsString());
						for (String sentence : sentences) {
							sentence = sentence.replaceAll("[^A-Za-z0-9-]", " ");
							sentence = sentence.replace("\\s+", " ");
							String taggedSentence = tagger.tagString(sentence);
							modRevfileWriter.append(taggedSentence+"\n");
						}
					} else {
						List<String> sentences = tokenizeText(reviewElement
								.getAsString());
						for (String sentence : sentences) {
							sentence = sentence.replaceAll("[^A-Za-z0-9-]", " ");
							sentence = sentence.replace("\\s+", " ");
							String taggedSentence = tagger.tagString(sentence);
							goodRevfileWriter.append(taggedSentence+"\n");
						}
					}
				} else {
					continue;
				}
			}
		}
		badRevfileWriter.close();
		modRevfileWriter.close();
		goodRevfileWriter.close();
		buffReader.close();
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out
					.println("USAGE: DataSegregator <BasePath>");
			System.exit(0);
		}
		setup(args[0]+File.separator+"models");
		populateRestaurantId(args[0]+File.separator+"RawData"+File.separator+"yelp_dataset"+File.separator+"business.json");
		seggregateRestaurantReviews(args[0]+File.separator+"RawData"+File.separator+"yelp_dataset"+File.separator+"review.json", args[0]+File.separator+"RawData"+File.separator+"DataSegFolder");
		cleanup();

	}
}
