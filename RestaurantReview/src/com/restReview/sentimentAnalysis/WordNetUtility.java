package com.restReview.sentimentAnalysis;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
/**
 * Class used for wordnet expansion
 * @author kumaran_rajendiran
 *
 */
public class WordNetUtility {
	private WordNetDatabase database;
	public void setup(String baseFolderPath){
		System.setProperty("wordnet.database.dir", baseFolderPath+File.separator+"dict"+File.separator);
		database = WordNetDatabase.getFileInstance();
	}
	
	/**
	 * Method to determine the related words given a word
	 * @param word
	 * @return
	 */
	public Set<String> getRelatedWords(String word){
		Set<String> wordList = new HashSet<String>();
		Synset[] synsets = database.getSynsets(word);
		if (synsets.length > 0)
		{
			for (int i = 0; i < synsets.length; i++)
			{
				String[] wordForms = synsets[i].getWordForms();
				for (int j = 0; j < wordForms.length; j++)
				{					
					wordList.add(wordForms[j]);
				}
			}
		}
		wordList.add(word);
		return wordList;
	}
}
