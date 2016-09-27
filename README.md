Type Ahead (A Predictive Text Editor)
=====================================

Is an editor that always tries to predict what you want to
type a good thing? That's what this project aims to find out.

September 2016 - Added Lucene 6.2 code for sentence prediction. _P.J. Lawrence_

* Right click on the on a word to see sentence suggestions.
_For example type "I'm" and right click on "I'm"._
* Sentences contained in the file TypeAheadSentences.txt in folder dict.


_Note to test the Lucene 6.2 document search test code_
To build the document index, i.e. the first time you use this command
java -cp TypeAhead.jar com.tikalucene.LuceneTikaSearchIndex <search phrase><folder_path>

Then to search without building the indexes type
java -cp TypeAhead.jar com.tikalucene.LuceneTikaSearchIndex "search phrase"