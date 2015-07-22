package com.restReview.sentimentAnalysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.stanford.nlp.trees.TypedDependency;
/**
 * Class used to extract the opinions from a given sentence
 * @author kumaran_rajendiran
 *
 */
public class OpinionExtractor {
	
   private Set<String> verbs; //list of frequently and commonly used verbs
   
   public void setup(){
	     verbs = new HashSet<String>();
		 verbs.add("can");
		 verbs.add("could");
		 verbs.add("may");
		 verbs.add("might");
		 verbs.add("will");
		 verbs.add("would");
		 verbs.add("must");
		 verbs.add("shall");
		 verbs.add("should");
		 verbs.add("ought");
		 verbs.add("have");
		 verbs.add("had");
		 verbs.add("has");
		 verbs.add("is");
		 verbs.add("am");
		 verbs.add("are");
		 verbs.add("was");
		 verbs.add("gave");
		 verbs.add("thought");
		 verbs.add("gives");
		 verbs.add("thinks");
   }
   /**
    * Method to extract the opinions from the typed dependency collection
    * @param typedDependencies
    * @return
    */
   public Set<String> extractOpinion(Collection<TypedDependency> typedDependencies){
	   Set<String> opinions = new HashSet<String>();
	   Set<String> amod = new HashSet<String>();
	   Set<String> advmod = new HashSet<String>();
	   Set<String> nsubj = new HashSet<String>();
	   Set<String> prep = new HashSet<String>();
	   Set<String> neg = new HashSet<String>();

	  
	   for(TypedDependency dependency: typedDependencies){
		   String dependencyString  = dependency.toString();
		   if(dependencyString.startsWith("nsubj")){	
			   String tmp =dependencyString.substring(dependencyString.indexOf("(") + 1, dependencyString.indexOf(")"));
			   String[] pair =tmp.split(",");
			   String word= pair[0].split("-")[0].trim();
			   if(!verbs.contains(word)){
				 nsubj.add(word);
			   }			  
		   }else if(dependencyString.startsWith("amod")){
			   String tmp =dependencyString.substring(dependencyString.indexOf("(") + 1, dependencyString.indexOf(")"));
			   String[] pair =tmp.split(",");
			   advmod.add(pair[0].split("-")[0].trim());
			   advmod.add(pair[1].split("-")[0].trim());
		   }else if(dependencyString.startsWith("advmod")){
			   String tmp =dependencyString.substring(dependencyString.indexOf("(") + 1, dependencyString.indexOf(")"));
			   String[] pair =tmp.split(",");
			   amod.add(pair[0].split("-")[0].trim());
			   amod.add(pair[1].split("-")[0].trim());
		   }else if(dependencyString.startsWith("neg")){
			   String tmp =dependencyString.substring(dependencyString.indexOf("(") + 1, dependencyString.indexOf(")"));
			   String[] pair =tmp.split(",");
			   neg.add(pair[0].split("-")[0].trim());
		   }else if(dependencyString.startsWith("prep")){
			   String tmp =dependencyString.substring(dependencyString.indexOf("(") + 1, dependencyString.indexOf(")"));
			   String[] pair =tmp.split(",");
			   prep.add(pair[1].split("-")[0].trim());
		   }
	   }
	   
	   for(String s : nsubj){
		   if(verbs.contains(s)){
			   continue;
		   }
		   if(neg.contains(s)){
			   opinions.add("~"+s);
		   }else{
			   opinions.add(s);  
		   }
	   }
	   for(String s : amod){
		   if(verbs.contains(s)){
			   continue;
		   }
		   if(neg.contains(s)){
			   opinions.add("~"+s);
		   }else{
			   opinions.add(s);  
		   }
	   }
	   for(String s : advmod){
		   if(verbs.contains(s)){
			   continue;
		   }
		   if(neg.contains(s)){
			   opinions.add("~"+s);
		   }else{
			   opinions.add(s);  
		   }
	   }
	   for(String s : prep){
		   if(verbs.contains(s)){
			   continue;
		   }
		   if(neg.contains(s)){
			   opinions.add("~"+s);
		   }else{
			   opinions.add(s);  
		   }
	   }
	   return opinions;
	   
   }
}
