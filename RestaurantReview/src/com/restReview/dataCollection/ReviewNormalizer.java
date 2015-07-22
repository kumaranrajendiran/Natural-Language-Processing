package com.restReview.dataCollection;

public class ReviewNormalizer {

	
	public static String normalizeReview(String review){
		review =  review.toLowerCase();
		review = review.replaceAll("[-]", "");
		review = review.replaceAll("won't", "would not");
		review = review.replaceAll("can't", "cannot");
		review = review.replaceAll("wont", "would not");
		review = review.replaceAll("cant", "cannot");
		review = review.replaceAll("n't", " not");
		review = review.replaceAll("'d", " would");
		review = review.replaceAll("'s", " is");
		review = review.replaceAll("'re", " are");
		review = review.replaceAll("'ll", " will");
		review = review.replaceAll("[^A-Za-z0-9\\.]", " ");
		review = review.replaceAll("\\s+", " ");
		return review;
	}
}
