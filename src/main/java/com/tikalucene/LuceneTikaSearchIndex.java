package com.tikalucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


/**
 * Sample searching an using lucent 6 and tika
 * @author  Peter Lawrence
 */
public class LuceneTikaSearchIndex {	
	/**
	 * @param indexDirectory	the index directory
	 * @param sourceDirectory	the directory to index
	 * @throws IOException 
	 */
	public static void buildIndex(String indexDirectory,String sourceDirectory) throws IOException {

		File docs = new File(sourceDirectory);
		File indexDir = new File(indexDirectory);
		
		Directory directory = FSDirectory.open(indexDir.toPath());
		
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		
		IndexWriter writer = new IndexWriter(directory, conf);
		
		writer.deleteAll(); // clear the database
		
		FieldType aFielType = new FieldType();
        aFielType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        aFielType.setStored(true);
        aFielType.setStoreTermVectors(true);
        aFielType.setTokenized(true);
        aFielType.setStoreTermVectorOffsets(true);
		
		File[] aFileList = docs.listFiles();
		if (aFileList!=null)
		{
			for (File file : aFileList) {
				Metadata metadata = new Metadata();
				ContentHandler handler = new BodyContentHandler();
				ParseContext context = new ParseContext();
				Parser parser = new AutoDetectParser();
				InputStream stream;
				try {
					stream = new FileInputStream(file);
				}
				catch (FileNotFoundException e){
					continue; // move onto the next file
				}
				
				try {
					parser.parse(stream, handler, metadata, context);
				}
				catch (TikaException e) {
					//e.printStackTrace();
					continue; // move onto the next file
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					//e.printStackTrace(); // ignore
				}
				finally {
					stream.close();
				}
				
				String text = handler.toString();
				String fileName = file.getName();		
				
				Document doc = new Document();
				//doc.add(new Field("file", fileName, Store.YES, Index.NO));
				//doc.add(new Field("file", fileName, type /*TextField.TYPE_STORED*/)); 
				doc.add(new Field("file", fileName, TextField.TYPE_STORED));
				
				for (String key : metadata.names()) {
					String name = key.toLowerCase();
					String value = metadata.get(key);
					
					if (StringUtils.isBlank(value)) {
						continue;
					}
					
					if ("keywords".equalsIgnoreCase(key)) {
						for (String keyword : value.split(",?(\\s+)")) {
							//doc.add(new Field(name, keyword, Store.YES, Index.NOT_ANALYZED));
							doc.add(new Field(name, keyword,TextField.TYPE_STORED)); 
						}
					}
					else if ("title".equalsIgnoreCase(key)) {
						//doc.add(new Field(name, value, Store.YES, Index.ANALYZED));
						doc.add(new Field(name, value, TextField.TYPE_STORED)); 
					}
					else {
						//doc.add(new Field(name, fileName, Store.YES, Index.NOT_ANALYZED));
						doc.add(new Field(name, fileName, TextField.TYPE_STORED)); 
					}
				}
				//doc.add(new Field("text", text, Store.NO, Index.ANALYZED));
				//doc.add(new Field("text", text, TextField.TYPE_NOT_STORED));
				doc.add(new Field("text", text, aFielType /*TextField.TYPE_STORED*/));
				writer.addDocument(doc);		
			}
		}
		
		writer.commit();
		writer.deleteUnusedFiles();
		
		System.out.println(writer.maxDoc() + " documents written");
		writer.close();
	}
	
	/**
	 * @param indexDirectory	the index directory
	 * @param searchString   	the search expression
	 * @throws IOException 
	 * @throws InvalidTokenOffsetsException
	 */
	public static void searchIndex(String indexDirectory,String searchString) throws IOException, InvalidTokenOffsetsException {
		File indexDir = new File(indexDirectory);
		Directory directory = FSDirectory.open(indexDir.toPath());
		
        // Build a Query object
		Query query=null;
		PhraseQuery thePhraseQuery=null;
		String[] words = searchString.split(" ");
		
		try {
			if (words!=null && words.length>1)
			{
				thePhraseQuery = new PhraseQuery(1,"text",words);
			}
			else {
				Analyzer analyzer = new StandardAnalyzer();
				QueryParser queryParser = new QueryParser("text", analyzer);
				query = queryParser.parse(searchString);
			}
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
			return;
		}
		
		int hitsPerPage = 10;
		IndexReader reader;
		try {
			reader = DirectoryReader.open(directory);
		}
		catch (IndexNotFoundException e)
		{
			System.out.println("Please generate the index by specifying a source folder.");
			return;
		}
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
		
		QueryScorer queryScorer=null;
		if (query!=null) {
			searcher.search(query, collector);
			queryScorer= new QueryScorer(query);
		}
		else if (thePhraseQuery!=null) {
			searcher.search(thePhraseQuery, collector);
			queryScorer= new QueryScorer(thePhraseQuery);
		}
		
		System.out.println(" === Total hits: " + collector.getTotalHits() + " ===");
		
		Fragmenter fragmenter = new SimpleFragmenter(200);
		Highlighter highlighter = new Highlighter(queryScorer); // Set the best scorer fragments
        highlighter.setTextFragmenter(fragmenter); // Set fragment to highlight
        
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (ScoreDoc hit : hits) {
			Document doc = reader.document(hit.doc);
			
			String title = doc.get("file");
			System.out.println("=============================");
			System.out.println("Found in: " + title + "  (" + hit.score + ")");
			
			Fields aField=reader.getTermVectors(hit.doc);
			if (aField!=null)
			{
				TokenStream tokenStream = TokenSources.getTermVectorTokenStreamOrNull("text", aField, -1);
				
	            if (tokenStream!=null) {
	            	
	            	String theContentsText = doc.get("text");
	            	if (theContentsText!=null)
	            	{
			            /*String fragment = highlighter.getBestFragment(tokenStream, theContentsText);
			            if (fragment!=null) {
			            	System.out.println(fragment);
			            }*/
			            TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, theContentsText, false, 4);
			            if (frag!=null)
			            {
			                for (int j = 0; j < frag.length; j++) {
			                    if ((frag[j] != null) && (frag[j].getScore() > 0)) {
			                    	System.out.println("========");
			                        System.out.println("Text Match: " + (j+1) + " "+ (frag[j].toString()));
			                    }
			                }
			            }
	            	}
	            }
			}
		}
		directory.close();
	}
	
	/**
	 * @param args
	 * @throws InvalidTokenOffsetsException 
	 */
	public static void main(String[] args) throws IOException, InvalidTokenOffsetsException {
		String indexDirectory  = "dict\\index";
		if (args.length>0)
		{
			// only need to build index each time stuff has changed in the source folder.
			if (args.length>1)
			{
				buildIndex(indexDirectory,args[1]);
			}
			searchIndex(indexDirectory,args[0]);
		}
		else {
			System.out.println("Please specify a search term.");
		}
	}
}