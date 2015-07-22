package com.restReview.dataCollection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.util.InvalidFormatException;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * Class used to segregate the restaurant reviews from yelp dataset
 * 
 * @author kumaran_rajendiran
 *
 */
public class CopyOfDataSegregator {
	public static Map<String,String> restaurantIdSet;
	public static int rC =0;

	public final static String BUSINESS_ID = "business_id";
	public final static String CATEGORIES = "categories";
	public final static String TEXT = "text";
	public final static String STARS = "stars";
	public static DoccatModel docCategorizationModel; 
	public static ExecutionEngine execEngine;
	private static final String FOOD = "food";
	private static final String AMBIENCE = "ambience";
	private static final String SERVICE = "service";
	private static final String PRICE = "price";

	public static void setup(String inputFolderPath) throws InvalidFormatException, IOException {
		restaurantIdSet = new HashMap<String,String>();
		DataClassifier dataClassifier = new DataClassifier();
		docCategorizationModel = dataClassifier.getClassificationModel(inputFolderPath);
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( inputFolderPath+File.separator+"review.db" );
		StringLogger s = StringLogger.wrap(new StringBuffer("")) ;
        execEngine = new ExecutionEngine(db, s);
	}

	public static void cleanup() {
		restaurantIdSet = null;
	}

	public static String[] tokenizeText(String text) {
		String[] sentences = text.split("\\.");
		return sentences;
	}
	
	public static void populateDatabase(String userId, String restaurantId, String review, String stars) throws InvalidFormatException, IOException{
		rC++;
		System.out.println(rC);
		review = ReviewNormalizer.normalizeReview(review);
		String[] statements = tokenizeText(review);
		ExecutionResult execResult = execEngine.execute("MERGE (user:User {name:\""+userId+"\"})"
				+" MERGE (restaurant:Restaurant {id:\""+restaurantId+"\",name:\""+restaurantIdSet.get(restaurantId)+"\"})"
				+" MERGE (user)-[r:RATED]->(restaurant)"
				+" ON CREATE SET r.count = "+stars.trim());
		
		for(String statement: statements){
			if(statement.isEmpty()){
				continue;
			}
			String category = DataClassifier.classifyText(docCategorizationModel, statement);
			String classValue= "";
			if (category.equals(FOOD)) {
				classValue = "F";
			} else if (category.equals(AMBIENCE)) {
				classValue = "A";
			} else if (category.equals(SERVICE)) {
				classValue ="S";
			} else if (category.equals(PRICE)) {
				classValue ="P";
			}
			if(!classValue.isEmpty()){
				execResult = execEngine.execute("MATCH (res:Restaurant) WHERE res.id=\""+restaurantId+"\" "
						+ " MERGE (review:Review {value:\""+statement+"\"})"
						+ " MERGE (res)-[r:R_"+classValue+"]->(review)");
			}
			
		}
		
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
				JsonElement nameElement = jobject.get("name");
				JsonElement reviewCntElement = jobject.get("review_count");
				JsonArray categoriesArray = jobject.get(CATEGORIES)
						.getAsJsonArray();
		
				if(Integer.parseInt(reviewCntElement.getAsString().trim())>=500){
					for (int iterator = 0; iterator < categoriesArray.size(); iterator++) {
						if (categoriesArray.get(iterator).getAsString()
								.toLowerCase().contains("restaurants")
								|| categoriesArray.get(iterator).getAsString()
										.toLowerCase().contains("nightlife")) {
							restaurantIdSet.put(businessIdElement.getAsString(), nameElement.getAsString());
							break;
						}
					}
				}
			}
		}
		buffReader.close();
	}

	public static void seggregateRestaurantReviews(String reviewFilePath) throws IOException {
		BufferedReader buffReader = new BufferedReader(new FileReader(new File(
				reviewFilePath)));
		String line = "";
		while ((line = buffReader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			} else {
				JsonElement jelement = new JsonParser().parse(line);
				JsonObject jobject = jelement.getAsJsonObject();
				JsonElement businessIdElement = jobject.get(BUSINESS_ID);
				if (restaurantIdSet.containsKey(businessIdElement.getAsString())) {
					JsonElement starsElement = jobject.get(STARS);
					JsonElement reviewElement = jobject.get(TEXT);
					JsonElement userIdElement = jobject.get("user_id");
					populateDatabase(userIdElement.getAsString(),
							businessIdElement.getAsString(), 
							reviewElement.getAsString(), 
							starsElement.getAsString());
					
				} else{
					continue;
				}
			}
		}
		buffReader.close();
	}

	public static void main(String[] args) throws IOException {
		setup(args[0]);
		populateRestaurantId(args[0]+File.separator+"RawData"+File.separator+"yelp_dataset"+File.separator+"business.json");
		System.out.println(restaurantIdSet.size());
		for(Entry<String, String> entry : restaurantIdSet.entrySet()){
			System.out.println(entry.getKey() +" "+entry.getValue());
		}
		seggregateRestaurantReviews(args[0]+File.separator+"RawData"+File.separator+"yelp_dataset"+File.separator+"review.json");
		cleanup();

	}
}
