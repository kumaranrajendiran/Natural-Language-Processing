package com.restReview.sentimentAnalysis;

import java.util.Collection;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
/**
 * Class to generate typed dependencies
 * @author kumaran_rajendiran
 *
 */
public class TypedDependencies {

	private LexicalizedParser parser;
	
	public void setup(){
		String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
		parser = LexicalizedParser.loadModel(parserModel);
	}
	/**
	 * Method to determine the typed dependencies of a sentence
	 * @param text
	 * @return
	 */
	public Collection<TypedDependency> getTypedDependencies(String text) {
	    Tree  parse = parser.parse(text);
	    TreebankLanguagePack tlp = parser.treebankLanguagePack(); // PennTreebankLanguagePack for English
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    Collection<TypedDependency> collection = gs.typedDependenciesCCprocessed();
	    return collection;
	 }
}
