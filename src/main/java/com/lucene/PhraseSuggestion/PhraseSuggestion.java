/**
 * @author Peter Lawrence
 * 
 * Test code for generating sentence suggestions
 * Using lucene 6 (http://lucene.apache.org/core/6_2_0/)
 * 
 */

package com.lucene.PhraseSuggestion;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

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
	private static final String REC_FIELD_NAME = "recommendation";
	
	static RAMDirectory phrasesDir = new RAMDirectory(); // Source Directory
	static RAMDirectory spellDir = new RAMDirectory(); // index Directory
	
	public static boolean buildSentenceDatabase(String aFileName) throws IOException
	{
		boolean Success=false;
		IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		IndexWriter writer = new IndexWriter(phrasesDir, iwc);
		BufferedReader textBuffer=null;
		FileReader textFile=null;
		try {
			textFile = new FileReader(aFileName);
			textBuffer = new BufferedReader(textFile);
			
			String aLineOfText;
			while ((aLineOfText = textBuffer.readLine())!=null)
			{
		        BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.UK);
		        boundary.setText(aLineOfText);
		        int start = boundary.first();
		        for (int end = boundary.next();end != BreakIterator.DONE; start = end, end = boundary.next())
		        {
		        	System.out.println(aLineOfText.substring(start,end));
		        	addRecommendation(aLineOfText.substring(start,end), writer);
		        }
			}
			Success=true;
		}
		catch (IOException e)
		{
			Success=false;
		}
		finally
		{
			if (textBuffer!=null) {
				textBuffer.close();
			}
			if (textFile!=null) {
				textFile.close();
			}
			writer.close();
		}
		return (Success);
	}
	
	public static boolean buildSentenceDatabase(String ... phrases) throws IOException
	{
		IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
		IndexWriter writer = new IndexWriter(phrasesDir, iwc);
		for (int i = 0; i < phrases.length; i++) {
			addRecommendation(phrases[i], writer);
		}
		writer.close();
		return (true);
	}
	
	// === Lucene Dictionary Suggester ===
    static SpellChecker m_phraseRecommender=null;
	
	public static boolean buildDictionaryPhrases() throws IOException
	{
		if (m_phraseRecommender!=null) {
			m_phraseRecommender=null;
		}
		indexDictionaryPhrases(phrasesDir, spellDir);
		m_phraseRecommender = new SpellChecker(spellDir);
		if (m_phraseRecommender==null) {
			return (false);
		}
		m_phraseRecommender.setAccuracy(0.3f);
		return (true);
	}
	
	private static void indexDictionaryPhrases(Directory SourceDir, Directory spellDir) throws IOException
	{		
		SpellChecker phraseRecommender = new SpellChecker(spellDir);

		IndexReader reader = DirectoryReader.open(SourceDir);
		
		LuceneDictionary aLuceneDictionary= new LuceneDictionary(reader,REC_FIELD_NAME);
		
		IndexWriterConfig iwc2 = new IndexWriterConfig(new StandardAnalyzer());

		phraseRecommender.indexDictionary(aLuceneDictionary, iwc2, true);
		phraseRecommender.close();
		reader.close();
	}
	
	public static String getSuggestion(String query) throws IOException
	{
		if (m_phraseRecommender==null)
		{
			return null;
		}
		String[] suggestions = m_phraseRecommender.suggestSimilar(query, 5);
		if (suggestions.length > 0) 
			return suggestions[0];
		else 
			return null;
	}
	
	public static String[] getSuggestions(String query, int MaxSuggestion) throws IOException
	{
		if (m_phraseRecommender==null)
		{
			return null;
		}
		String[] suggestions = m_phraseRecommender.suggestSimilar(query, MaxSuggestion);
		if (suggestions.length > 0) 
			return suggestions;
		else 
			return null;
	}
	
	// === Fuzzy Suggester ===
	private static FuzzySuggester m_aFizzySuggestor=null; 
	
	public static boolean buildFizzyPhrases() throws IOException
	{
		if (m_aFizzySuggestor!=null) {
			m_aFizzySuggestor=null;
		}
		m_aFizzySuggestor = indexFuzzyPhrases(phrasesDir, spellDir);
		if (m_aFizzySuggestor==null) {
			return (false);
		}
		return (true);
	}
	
	private static FuzzySuggester indexFuzzyPhrases(Directory SourceDir, Directory spellDir) throws IOException 
	{
		StandardAnalyzer autosuggestAnalyzer = new StandardAnalyzer();
		FuzzySuggester suggestor = new FuzzySuggester(spellDir,"tmp",autosuggestAnalyzer);

		IndexReader reader = DirectoryReader.open(SourceDir);
		LuceneDictionary aLuceneDictionary= new LuceneDictionary(reader,REC_FIELD_NAME);
		
		suggestor.build(aLuceneDictionary);
		
		reader.close();
		
		return (suggestor);
	}
	
	public static String[] getFuzzySuggestions(String query, int num) throws IOException
	{
		if (m_aFizzySuggestor==null)
		{
			return null;
		}
		List<LookupResult> suggestionsList = m_aFizzySuggestor.lookup(CharBuffer.wrap(query),false,num);
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

	// === AnalyzingInfix  Suggester ===
	private static AnalyzingInfixSuggester m_AnalyzingInfixSuggester = null;
	
	public static boolean buildAnalyzingInfixPhrases() throws IOException
	{
		if (m_AnalyzingInfixSuggester!=null) {
			m_AnalyzingInfixSuggester=null;
		}
		m_AnalyzingInfixSuggester = indexAnalyzingInfixSuggesterPhrases(phrasesDir, spellDir);
		if (m_AnalyzingInfixSuggester==null) {
			return (false);
		}
		return (true);
	}
	
	private static AnalyzingInfixSuggester indexAnalyzingInfixSuggesterPhrases(Directory SourceDir, Directory spellDir) throws IOException 
	{
		StandardAnalyzer autosuggestAnalyzer = new StandardAnalyzer();
		AnalyzingInfixSuggester suggestor = new AnalyzingInfixSuggester(spellDir,autosuggestAnalyzer);

		IndexReader reader = DirectoryReader.open(SourceDir);
		LuceneDictionary aLuceneDictionary= new LuceneDictionary(reader,REC_FIELD_NAME);
		
		suggestor.build(aLuceneDictionary);
		
		reader.close();
		
		return (suggestor);
	}
	
	public static String[] getAnalyzingInfixSuggestions(String query, int num) throws IOException
	{
		if (m_AnalyzingInfixSuggester==null)
		{
			return null;
		}
		List<LookupResult> suggestionsList = m_AnalyzingInfixSuggester.lookup(CharBuffer.wrap(query),false,num);
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
	
	public static void outputSuggestions(String ... SuggestionList) {
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
		
		buildSentenceDatabase(TestData);
		
		//buildSentenceDatabase("E:\\Thirdparty\\TypeAheadProject\\TypeAhead\\docs\\Sentences.txt");
		
		System.out.println("==== Lucene Dictionary Suggestor ====");
		buildDictionaryPhrases();
		
		outputSuggestions(getSuggestions("spam spam",5));
		outputSuggestions(getSuggestions("A tiger",5));
		outputSuggestions(getSuggestions("Romans  ever done for us",5));
	
		System.out.println("==== Fuzzy Suggestor ===");
		buildFizzyPhrases();
				
		outputSuggestions(getFuzzySuggestions("spam",5));
		outputSuggestions(getFuzzySuggestions("lovely",5));
		outputSuggestions(getFuzzySuggestions("a tger",5));
		outputSuggestions(getFuzzySuggestions("tiger",5));
		
		System.out.println("==== AnalyzingInfix Suggestor ===");
		buildAnalyzingInfixPhrases();
		
		outputSuggestions(getAnalyzingInfixSuggestions("spam",5));
		outputSuggestions(getAnalyzingInfixSuggestions("lovely",5));
		outputSuggestions(getAnalyzingInfixSuggestions("a tger",5));
		outputSuggestions(getAnalyzingInfixSuggestions("a tiger",5));
	}
} 
