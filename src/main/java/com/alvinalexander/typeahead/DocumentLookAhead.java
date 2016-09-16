package com.alvinalexander.typeahead;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.print.DocFlavor.URL;

import com.lucene.PhraseSuggestion.PhraseSuggestion;

/**
 * @author Alvin Alexander, AlvinAlexander.com
 * 
 * This is a very heavily-modified version of a class found in the
 * book, Core Swing.
 * 
 */
public class DocumentLookAhead implements LookAheadTextPane.TextLookAhead {

	private TreeSet<String> setOfWords = new TreeSet<String>();
	private String text;
	private List<String> dictionaryWords;
	private PhraseSuggestion dictionarySentenaces = new  PhraseSuggestion();
	
	boolean UseSentenceSuggestor=false;

	public DocumentLookAhead() {
		try {
			// TODO a kludge so i can edit the dictionary as desired
			//String homeDir = System.getProperty("user.home");
			//dictionaryWords = FileUtilities.getFileAsListOfStrings(homeDir + "/TypeAheadDictionary.txt");
			//PhraseSuggestion.buildSentenceDatabase(homeDir + "/TypeAheadSentances.txt");
			
			//Get file from resources folder
			dictionaryWords = FileUtilities.getFileAsListOfStrings("dict/TypeAheadDictionary.txt");

			if (dictionarySentenaces.buildSentenceDatabase("dict/TypeAheadSentences.txt")) 
			{
				dictionarySentenaces.buildFizzyPhrases();
			}
		} catch (IOException ioe) {
			System.err.println("IOException occurred trying to read dictionary.");
			System.err.println(ioe.getMessage());
		}
	}

	public String doLookAhead(String key) {
		if (key == null) return null;
		if (key.trim().equals("")) return null;

		Iterator<String> it = setOfWords.iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			if (s.startsWith(key) == true) {
				return s;
			}
		}

		// no match locally? try the dictionary.
		Iterator<String> dictionaryIterator = dictionaryWords.iterator();
		while (dictionaryIterator.hasNext()) {
			String s = (String) dictionaryIterator.next();
			if (s.startsWith(key) == true) {
				return s;
			}
		}

		// No match found - return null
		return null;
	}

	public String doLookAheadSentence(String sentenceKey) {
		if (sentenceKey == null) return null;
		if (sentenceKey.trim().equals("")) return null;

		try {
			String[] Suggestions = dictionarySentenaces.getFuzzySuggestions(sentenceKey,1);
			if (Suggestions!=null && Suggestions.length>0) {
				return (Suggestions[0]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}

		// No match found - return null
		return null;
	}
	
	public String getText() {
		return text;
	}

	public void setText(final String document) {
		setOfWords = new TreeSet<String>();
		this.text = document;
		text = text.replaceAll("\\s", " ");
		text = text.replaceAll("\\.", " ");
		text = text.replaceAll("\\?", " ");
		text = text.replaceAll("\\(", " ");
		text = text.replaceAll("\\)", " ");
		StringTokenizer st = new StringTokenizer(text, " ", false);
		int numTokens = st.countTokens();
		int i = 0;
		// never add the last token
		for (i = 0; i < numTokens - 1; i++) {
			String s = st.nextToken();
			setOfWords.add(s);
		}
	}

	public void addWord(String word) {
		setOfWords.add(word);
	}
}
