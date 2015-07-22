package com.restReview.featureExtraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.restReview.sentimentAnalysis.WordNetUtility;

/**
 * Class to extract the features from the training set
 * 
 * @author kumaran_rajendiran
 *
 */
class ValueComparator implements Comparator<String> {

	Map<String, Integer> map;

	public ValueComparator(Map<String, Integer> base) {
		this.map = base;
	}

	public int compare(String a, String b) {
		if (map.get(a) >= map.get(b)) {
			return -1;
		} else {
			return 1;
		}
	}
}

public class FeatureExtraction {

	public Set<String> nounInBadReviews;
	public Set<String> adjInBadReviews;
	public Set<String> nounInGoodReviews;
	public Set<String> adjInGoodReviews;
	public Set<String> nounInModReviews;
	public Set<String> adjInModReviews;

	public static int MAX = 1000;

	public void setup() {
		nounInBadReviews = new HashSet<String>();
		adjInBadReviews = new HashSet<String>();
		nounInGoodReviews = new HashSet<String>();
		adjInGoodReviews = new HashSet<String>();
		nounInModReviews = new HashSet<String>();
		adjInModReviews = new HashSet<String>();
	}

	public List<String> tokenizeText(String text) {
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

	public TreeMap<String, Integer> SortByValue(HashMap<String, Integer> map) {
		ValueComparator vc = new ValueComparator(map);
		TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(vc);
		sortedMap.putAll(map);
		return sortedMap;
	}

	/**
	 * Method to extract the features from the bad reviews
	 * 
	 * @param dataFolderPath
	 * @param wordNetUtility
	 * @param dictionary
	 * @throws IOException
	 */
	public void extractBadReviewFeatures(String dataFolderPath,
			WordNetUtility wordNetUtility, Map<String, Double> dictionary)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new FileReader(new File(
				dataFolderPath + File.separator + "Bad.txt")));
		String line = "";
		HashMap<String, Integer> nounCnt = new HashMap<String, Integer>();
		HashMap<String, Integer> adjCnt = new HashMap<String, Integer>();
		while ((line = buffReader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			} else {
				String[] words = line.split("\\s+");
				for (int iterator = 0; iterator < words.length; iterator++) {
					if (words[iterator].endsWith("_NN")
							|| words[iterator].endsWith("_NNS")) {
						if (iterator > 0
								&& (words[iterator - 1].endsWith("_PRP$") || words[iterator - 1]
										.endsWith("_PRP$"))) {
							continue;
						} else {
							String[] parts = words[iterator].split("_");
							if (nounCnt.containsKey(parts[0])) {
								int prevCnt = nounCnt.get(parts[0]);
								nounCnt.put(parts[0].toLowerCase(), prevCnt + 1);
							} else {
								nounCnt.put(parts[0].toLowerCase(), 1);
							}
						}
					} else if (words[iterator].endsWith("_NNP")
							|| words[iterator].endsWith("_NNPS")) {
						String[] parts = words[iterator].split("_");
						if (nounCnt.containsKey(parts[0])) {
							int prevCnt = nounCnt.get(parts[0]);
							nounCnt.put(parts[0].toLowerCase(), prevCnt + 1);
						} else {
							nounCnt.put(parts[0].toLowerCase(), 1);
						}
					} else if (words[iterator].endsWith("_JJ")
							|| words[iterator].endsWith("_JJS")
							|| words[iterator].endsWith("_JJR")) {
						String[] parts = words[iterator].split("_");
						if (adjCnt.containsKey(parts[0])) {
							int prevCnt = adjCnt.get(parts[0]);
							adjCnt.put(parts[0].toLowerCase(), prevCnt + 1);
						} else {
							adjCnt.put(parts[0].toLowerCase(), 1);
						}
					}
				}
			}
		}
		TreeMap<String, Integer> sortedNounCnt = SortByValue(nounCnt);
		TreeMap<String, Integer> sortedAdjCnt = SortByValue(adjCnt);

		int count = 0;
		for (Entry<String, Integer> entry : sortedNounCnt.entrySet()) {
			if (count >= MAX) {
				break;
			}
			if (!Pattern.matches("[\\dA-Za-z]+", entry.getKey())) {
				continue;
			}

			nounInBadReviews.add(entry.getKey());
			count++;
		}
		count = 0;
		for (Entry<String, Integer> entry : sortedAdjCnt.entrySet()) {
			if (count >= MAX) {
				break;
			}
			if (!Pattern.matches("[\\dA-Za-z]+", entry.getKey())) {
				continue;
			}
			String adj = entry.getKey();
			if (dictionary.containsKey(adj + "_a")) {
				if (dictionary.get(adj + "_a") <= 0.0D) {
					adjInBadReviews.add(adj);
					count++;
				}
			}
		}
		buffReader.close();
	}

	/**
	 * Method to extract the features from the good reviews
	 * 
	 * @param dataFolderPath
	 * @param wordNetUtility
	 * @param dictionary
	 * @throws IOException
	 */
	public void extractGoodReviewFeatures(String dataFolderPath,
			WordNetUtility wordNetUtility, Map<String, Double> dictionary)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new FileReader(new File(
				dataFolderPath + File.separator + "Good.txt")));
		String line = "";
		HashMap<String, Integer> nounCnt = new HashMap<String, Integer>();
		HashMap<String, Integer> adjCnt = new HashMap<String, Integer>();
		while ((line = buffReader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			} else {

				String[] words = line.split("\\s+");
				for (int iterator = 0; iterator < words.length; iterator++) {
					if (words[iterator].endsWith("_NN")
							|| words[iterator].endsWith("_NNS")) {
						if (iterator > 0
								&& (words[iterator - 1].endsWith("_PRP$") || words[iterator - 1]
										.endsWith("_PRP$"))) {
							continue;
						} else {
							String[] parts = words[iterator].split("_");
							if (nounCnt.containsKey(parts[0])) {
								int prevCnt = nounCnt.get(parts[0]);
								nounCnt.put(parts[0].toLowerCase(), prevCnt + 1);
							} else {
								nounCnt.put(parts[0].toLowerCase(), 1);
							}
						}
					} else if (words[iterator].endsWith("_NNP")
							|| words[iterator].endsWith("_NNPS")) {
						String[] parts = words[iterator].split("_");
						if (nounCnt.containsKey(parts[0])) {
							int prevCnt = nounCnt.get(parts[0]);
							nounCnt.put(parts[0].toLowerCase(), prevCnt + 1);
						} else {
							nounCnt.put(parts[0].toLowerCase(), 1);
						}
					} else if (words[iterator].endsWith("_JJ")
							|| words[iterator].endsWith("_JJS")
							|| words[iterator].endsWith("_JJR")) {
						String[] parts = words[iterator].split("_");
						if (adjCnt.containsKey(parts[0])) {
							int prevCnt = adjCnt.get(parts[0]);
							adjCnt.put(parts[0].toLowerCase(), prevCnt + 1);
						} else {
							adjCnt.put(parts[0].toLowerCase(), 1);
						}
					}
				}
			}
		}
		TreeMap<String, Integer> sortedNounCnt = SortByValue(nounCnt);
		TreeMap<String, Integer> sortedAdjCnt = SortByValue(adjCnt);
		int count = 0;
		for (Entry<String, Integer> entry : sortedNounCnt.entrySet()) {
			if (count >= MAX) {
				break;
			}
			if (!Pattern.matches("[\\dA-Za-z]+", entry.getKey())) {
				continue;
			}
			nounInGoodReviews.add(entry.getKey());
			count++;
		}
		count = 0;
		for (Entry<String, Integer> entry : sortedAdjCnt.entrySet()) {
			if (count >= MAX) {
				break;
			}
			if (!Pattern.matches("[\\dA-Za-z]+", entry.getKey())) {
				continue;
			}
			String adj = entry.getKey();
			if (dictionary.containsKey(adj + "_a")) {
				if (dictionary.get(adj + "_a") > 0.3D) {
					adjInGoodReviews.add(adj);
					count++;
				}
			}

		}
		buffReader.close();
	}

	/**
	 * Method to extract the features from the moderate reviews
	 * 
	 * @param dataFolderPath
	 * @param wordNetUtility
	 * @param dictionary
	 * @throws IOException
	 */
	public void extractModReviewFeatures(String dataFolderPath,
			WordNetUtility wordNetUtility, Map<String, Double> dictionary)
			throws IOException {
		BufferedReader buffReader = new BufferedReader(new FileReader(new File(
				dataFolderPath + File.separator + "Moderate.txt")));
		String line = "";
		HashMap<String, Integer> nounCnt = new HashMap<String, Integer>();
		HashMap<String, Integer> adjCnt = new HashMap<String, Integer>();
		while ((line = buffReader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			} else {
				String[] words = line.split("\\s+");
				for (int iterator = 0; iterator < words.length; iterator++) {
					if (words[iterator].endsWith("_NN")
							|| words[iterator].endsWith("_NNS")) {
						if (iterator > 0
								&& (words[iterator - 1].endsWith("_PRP$") || words[iterator - 1]
										.endsWith("_PRP$"))) {
							continue;
						} else {
							String[] parts = words[iterator].split("_");
							if (nounCnt.containsKey(parts[0])) {
								int prevCnt = nounCnt.get(parts[0]);
								nounCnt.put(parts[0].toLowerCase(), prevCnt + 1);
							} else {
								nounCnt.put(parts[0].toLowerCase(), 1);
							}
						}
					} else if (words[iterator].endsWith("_NNP")
							|| words[iterator].endsWith("_NNPS")) {
						String[] parts = words[iterator].split("_");
						if (nounCnt.containsKey(parts[0])) {
							int prevCnt = nounCnt.get(parts[0]);
							nounCnt.put(parts[0].toLowerCase(), prevCnt + 1);
						} else {
							nounCnt.put(parts[0].toLowerCase(), 1);
						}
					} else if (words[iterator].endsWith("_JJ")
							|| words[iterator].endsWith("_JJS")
							|| words[iterator].endsWith("_JJR")) {
						String[] parts = words[iterator].split("_");
						if (adjCnt.containsKey(parts[0])) {
							int prevCnt = adjCnt.get(parts[0]);
							adjCnt.put(parts[0].toLowerCase(), prevCnt + 1);
						} else {
							adjCnt.put(parts[0].toLowerCase(), 1);
						}
					}
				}

			}
		}
		TreeMap<String, Integer> sortedNounCnt = SortByValue(nounCnt);
		TreeMap<String, Integer> sortedAdjCnt = SortByValue(adjCnt);

		int count = 0;
		for (Entry<String, Integer> entry : sortedNounCnt.entrySet()) {
			if (count >= MAX) {
				break;
			}
			if (!Pattern.matches("[\\dA-Za-z]+", entry.getKey())) {
				continue;
			}
			nounInModReviews.add(entry.getKey());
			count++;
		}
		count = 0;
		for (Entry<String, Integer> entry : sortedAdjCnt.entrySet()) {
			if (count >= MAX) {
				break;
			}
			if (!Pattern.matches("[\\dA-Za-z]+", entry.getKey())) {
				continue;
			}
			String adj = entry.getKey();
			if (dictionary.containsKey(adj + "_a")) {
				if (dictionary.get(adj + "_a") <= 0.3D
						&& dictionary.get(adj + "_a") > 0.0D) {
					adjInModReviews.add(adj);
					count++;
				}
			}
		}
		buffReader.close();
	}
}
