/**
 * @author Peter Lawrence
 * 
 * Test code for generating sentence suggestions
 * Used lucene 6
 * 
 */

package com.lucene.PhraseSuggestion;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class PhraseSuggestion {
	public static final String REC_FIELD_NAME = "recommendation";
	
	// === Lucene Suggester ===
	public static void indexDictionaryPhrases(Directory dir, Directory spellDir,String ... phrases) throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		IndexWriter writer = new IndexWriter(dir, iwc);

		for (int i = 0; i < phrases.length; i++) {
			addRecommendation(phrases[i], writer);
		}

		writer.close();

		SpellChecker phraseRecommender = new SpellChecker(spellDir);

		IndexReader reader = DirectoryReader.open(dir);
		
		LuceneDictionary aLuceneDictionary= new LuceneDictionary(reader,REC_FIELD_NAME);
		
		IndexWriterConfig iwc2 = new IndexWriterConfig(new StandardAnalyzer());

		phraseRecommender.indexDictionary(aLuceneDictionary, iwc2, true);
		phraseRecommender.close();
		reader.close();
	}
	
	public static String getSuggestion(String query, SpellChecker phraseRecommender) throws IOException {
		String[] suggestions = phraseRecommender.suggestSimilar(query, 5);
		if (suggestions.length > 0) 
			return suggestions[0];
		else 
			return null;
	}
	
	public static String[] getSuggestions(String query, int MaxSuggestion,SpellChecker phraseRecommender) throws IOException {
		String[] suggestions = phraseRecommender.suggestSimilar(query, MaxSuggestion);
		if (suggestions.length > 0) 
			return suggestions;
		else 
			return null;
	}
	
	// === Fuzzy Suggester ===
	public static FuzzySuggester indexFuzzyPhrases(Directory SourceDir, Directory spellDir,String ... phrases) throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		IndexWriter writer = new IndexWriter(SourceDir, iwc);
		for (int i = 0; i < phrases.length; i++) {
			addRecommendation(phrases[i], writer);
		}
		writer.close();

		StandardAnalyzer autosuggestAnalyzer = new StandardAnalyzer();
		FuzzySuggester suggestor = new FuzzySuggester(spellDir,"tmp",autosuggestAnalyzer);

		IndexReader reader = DirectoryReader.open(SourceDir);
		LuceneDictionary aLuceneDictionary= new LuceneDictionary(reader,REC_FIELD_NAME);
		
		suggestor.build(aLuceneDictionary);
		
		reader.close();
		
		return (suggestor);
	}
	
	private static String[] getFuzzySuggestions(FuzzySuggester aSuggestor, String query, int num) throws IOException
	{
		List<LookupResult> suggestionsList = aSuggestor.lookup(CharBuffer.wrap(query),false,num);
		if (suggestionsList!=null)
		{
			if (suggestionsList.size()>0)
			{
				String[] SuggestionList = new String[suggestionsList.size()];
				for (int i = 0; i < suggestionsList.size(); i++) {
					LookupResult aResult= suggestionsList.get(i);
					if (aResult!=null)
					{
						SuggestionList[i]=aResult.key.toString();
					}
				}
				return (SuggestionList);
			}
		}
		return (null);
	}

	// === Fuzzy Suggester ===
	public static AnalyzingInfixSuggester indexAnalyzingInfixSuggesterPhrases(Directory SourceDir, Directory spellDir,String ... phrases) throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		IndexWriter writer = new IndexWriter(SourceDir, iwc);
		for (int i = 0; i < phrases.length; i++) {
			addRecommendation(phrases[i], writer);
		}
		writer.close();

		StandardAnalyzer autosuggestAnalyzer = new StandardAnalyzer();
		AnalyzingInfixSuggester suggestor = new AnalyzingInfixSuggester(spellDir,autosuggestAnalyzer);

		IndexReader reader = DirectoryReader.open(SourceDir);
		LuceneDictionary aLuceneDictionary= new LuceneDictionary(reader,REC_FIELD_NAME);
		
		suggestor.build(aLuceneDictionary);
		
		reader.close();
		
		return (suggestor);
	}
	
	private static String[] getAnalyzingInfixSuggestions(AnalyzingInfixSuggester aSuggestor, String query, int num) throws IOException
	{
		List<LookupResult> suggestionsList = aSuggestor.lookup(CharBuffer.wrap(query),false,num);
		if (suggestionsList!=null)
		{
			if (suggestionsList.size()>0)
			{
				String[] SuggestionList = new String[suggestionsList.size()];
				for (int i = 0; i < suggestionsList.size(); i++) {
					LookupResult aResult= suggestionsList.get(i);
					if (aResult!=null)
					{
						SuggestionList[i]=aResult.key.toString();
					}
				}
				return (SuggestionList);
			}
		}
		return (null);
	}
	
	// === Utility Functions ===
	private static void addRecommendation(String phrase, IndexWriter writer)
			throws CorruptIndexException, IOException {
		Document doc = new Document();

		FieldType ft = new FieldType(StringField.TYPE_NOT_STORED);
		ft.setOmitNorms(false);
		Field f = new Field(REC_FIELD_NAME, phrase, ft);
		doc.add(f);

		writer.addDocument(doc);
	}
	
	public static void OutputSuggestions(String ... SuggestionList) {
		if (SuggestionList!=null && SuggestionList.length>0) {
			if (SuggestionList.length==1) {
				System.out.println("Found 1 Suggestion");
			}
			else {
				System.out.println("Found " + SuggestionList.length + " Suggestions");
			}
			for (int i = 0; i < SuggestionList.length; i++) {
				System.out.println("[" + (i+1) + "] " + SuggestionList[i]);
			}
		}
		else {
			System.out.println("Found 0 Suggestions");
		}
	}
	
	// === Test Function ===
	public static void main(String[] args) throws IOException {
		RAMDirectory phrasesDir = new RAMDirectory();
		RAMDirectory spellDir = new RAMDirectory();
		
		String TestData[] = { "What have the Romans ever done for us?",
				"This parrot is no more.",
				"A tiger... in Africa?",
				"That Rabbit's Dynamite!!",
				"Lovely spam! Wonderful spam!",
				"Spam spam spam spam...",
				"Spam is good for you",
				"A duck",
				"Strange ladies lying in pools distributing swords is no basis for government",
				"Nobody expects the Spanish Inquisition" };
		
		System.out.println("==== Lucene Dictionary Suggestor ====");
		indexDictionaryPhrases(phrasesDir, spellDir,TestData);
		SpellChecker phraseRecommender = new SpellChecker(spellDir);
		phraseRecommender.setAccuracy(0.3f);
		
		OutputSuggestions(getSuggestions("spam spam",5,phraseRecommender));
		OutputSuggestions(getSuggestions("A tiger",5,phraseRecommender));
		OutputSuggestions(getSuggestions("Romans  ever done for us",5,phraseRecommender));
	
		System.out.println("==== Fuzzy Suggestor ===");
		FuzzySuggester aSuggestor = indexFuzzyPhrases(phrasesDir, spellDir,TestData);
		
		OutputSuggestions(getFuzzySuggestions(aSuggestor, "spam",5));
		OutputSuggestions(getFuzzySuggestions(aSuggestor, "lovely",5));
		OutputSuggestions(getFuzzySuggestions(aSuggestor, "a tger",5));
		OutputSuggestions(getFuzzySuggestions(aSuggestor, "tiger",5));
		
		System.out.println("==== AnalyzingInfix Suggestor ===");
		AnalyzingInfixSuggester anInfixSuggestor = indexAnalyzingInfixSuggesterPhrases(phrasesDir, spellDir,TestData);
		
		OutputSuggestions(getAnalyzingInfixSuggestions(anInfixSuggestor, "spam",5));
		OutputSuggestions(getAnalyzingInfixSuggestions(anInfixSuggestor, "lovely",5));
		OutputSuggestions(getAnalyzingInfixSuggestions(anInfixSuggestor, "a tger",5));
		OutputSuggestions(getFuzzySuggestions(aSuggestor, "a tiger",5));
	}
} 
