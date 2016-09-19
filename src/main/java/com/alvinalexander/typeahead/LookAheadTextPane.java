package com.alvinalexander.typeahead;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.*;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;


/**
 * @author Alvin Alexander, AlvinAlexander.com
 * 
 * This is a very heavily-modified version of a class found in the Core
 * Swing book.
 * 
 * I think this can be a drop-in replacement for a TextPane.
 * 
 */
public class LookAheadTextPane extends JTextPane {
	
	class CurrentSentenceString
	{
		public int m_StartPos;
		public int m_EndPos;
		public CurrentSentenceString()
		{
			m_StartPos=m_EndPos=0;
		}
		public boolean isNewLine(char a_char)
		{
			switch (a_char)
			{
			case 0:
			case '\n':
			case '.':
				return (true);
			}
			return (false);
		}
		
		public String getSentenceUnderCurser()
		{
			Document doc = getDocument();
			if (doc != null) {
				try {
					// go back to previous whitespace
					int docLength = doc.getLength();
					String sentenceTxt = new String();
					int caretPosition = getCaretPosition();
					
					// scan backward
					int i;
					String aChar;
					char a_char;
					if (caretPosition>0)
					{
						--caretPosition;
						int LastNonSpace=caretPosition;
						for (i=caretPosition;i>-1;--i)
						{
							aChar=doc.getText(i, 1);
							a_char = aChar.charAt(0);
							if (isNewLine(a_char))
							{
								break;
							}
							sentenceTxt= aChar + sentenceTxt;
							m_StartPos=i;
							if (a_char!=32) {
								LastNonSpace=m_StartPos;
							}
						}
						if (LastNonSpace>-1) {
							int Diff=LastNonSpace-m_StartPos;
							if (Diff>0) {
								m_StartPos=LastNonSpace;
								if (Diff<sentenceTxt.length()) {
									sentenceTxt=sentenceTxt.substring(Diff);
								}
								else {
									sentenceTxt="";
								}
							}
						}
					}
					else {
						m_StartPos=0;
					}
					// scan forward
					for (i=caretPosition;i<docLength;++i)
					{
						aChar=doc.getText(i, 1);
						a_char = aChar.charAt(0);
						if (isNewLine(a_char))
						{
							break;
						}
						sentenceTxt= sentenceTxt + aChar;
						m_EndPos=i;
					}
					if (sentenceTxt.length()>0)
						return (sentenceTxt);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
	}
	
	CurrentSentenceString m_CurrentSentence=new CurrentSentenceString();

	class PopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;
		MouseEvent m_MouseEvent;
		
		ActionListener menuListenerReplaceText = new ActionListener() 
    	{
    	      public void actionPerformed(ActionEvent event)
    	      {
    	    	  int start = m_CurrentSentence.m_StartPos;
    	    	  int end = m_CurrentSentence.m_EndPos+1;
    	    	  StringBuilder strBuilder = new StringBuilder(getText());
    	    	  strBuilder.replace(start, end, event.getActionCommand());
    	    	  setText(strBuilder.toString());
    	      }
    	};
    	
    	ActionListener menuListenerAlternativeText = new ActionListener() 
    	{
    	      public void actionPerformed(ActionEvent event)
    	      {
    	    	  PopUpMenu menu = new PopUpMenu();
    		        
    		        String SentenceText = m_CurrentSentence.getSentenceUnderCurser();
    		        if (SentenceText!=null && SentenceText.length()>1)
    		        {
    		        	JMenuItem item;
    			        String[] suggestionsList = lookAhead.GetAlternaticeSentence(SentenceText);
    			        if (suggestionsList!=null && suggestionsList.length>0) 
    			        {
    			        	for (int i = 0; i < suggestionsList.length; i++) {
    							String aResult= suggestionsList[i];
    							if (aResult!=null)
    							{
    								item = new JMenuItem(aResult);
    								menu.add(item);
    								item.addActionListener(menu.menuListenerReplaceText);
    							}
    						}
    			        }
    			        else {
    			        	 menu.add("No Suggestions found");
    			        }
    			        menu.show(m_MouseEvent.getComponent(),m_MouseEvent.getX(),m_MouseEvent.getY());
    		        }
    	      }
    	};
    	
	    public PopUpMenu(){
	    }
	}
	
	class PopClickListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e){
	        if (e.isPopupTrigger())
	            doPop(e);
	    }

	    public void mouseReleased(MouseEvent e){
	        if (e.isPopupTrigger())
	            doPop(e);
	    }

	    private void doPop(MouseEvent e){
	        PopUpMenu menu = new PopUpMenu();
	        menu.m_MouseEvent=e;
	        
	        String SentenceText = m_CurrentSentence.getSentenceUnderCurser();
	        if (SentenceText!=null && SentenceText.length()>1)
	        {
	        	JMenuItem item;
		        String[] suggestionsList = lookAhead.GetSuggestions(SentenceText);
		        if (suggestionsList!=null && suggestionsList.length>0) 
		        {
		        	for (int i = 0; i < suggestionsList.length; i++) {
						String aResult= suggestionsList[i];
						if (aResult!=null)
						{
							item = new JMenuItem(aResult);
							menu.add(item);
							item.addActionListener(menu.menuListenerReplaceText);
						}
					}
		        }
		        else {
		        	 menu.add("No Suggestions found");
		        }
		        item = new JMenuItem("Alternative Sentences?");
				menu.add(item);
				item.addActionListener(menu.menuListenerAlternativeText);
		        menu.show(e.getComponent(), e.getX(), e.getY());
	        }
	    }
	}
	
	private static final long serialVersionUID = 1L;
	int endOfCurrentSuggestion = 0;

	public LookAheadTextPane() {
		this(null);
		initUi();
	}

	public LookAheadTextPane(int columns) {
		this(null);
		initUi();
	}
	
	private void configureFont() {
		Font font = new Font("Monaco", Font.PLAIN, 13);
        this.setFont(font);
	}
	
	private void initUi() {
		configureFont();
        this.setMargin(new Insets(20, 20, 20, 20));
        this.setBackground(new Color(218, 235, 218));
        this.addKeyListener(new KeyListener(){
            @Override
            public void keyPressed(KeyEvent e){
            	// move to the end of the current suggestion
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_TAB) {
                	e.consume();
                	moveCaretPosition(endOfCurrentSuggestion);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        this.addMouseListener(new PopClickListener());
	}

	public LookAheadTextPane(TextLookAhead lookAhead) {
		initUi();
		setLookAhead(lookAhead);
		this.setPreferredSize(new Dimension(754, 890));
		this.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {}
			public void insertUpdate(DocumentEvent e) {}
			public void removeUpdate(DocumentEvent e) {}
		});

		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {}
			public void focusLost(FocusEvent e) {
				if (e.isTemporary() == false) {
					// Remove any existing selection
					setCaretPosition(getDocument().getLength());
				}
			}
		});
	}

	public void setLookAhead(TextLookAhead lookAhead) {
		this.lookAhead = lookAhead;
	}

	public TextLookAhead getLookAhead() {
		return lookAhead;
	}
	
	public void replaceSelection(String content) {
		super.replaceSelection(content);

		if (isEditable() == false || isEnabled() == false) {
			return;
		}

		Document doc = getDocument();

		int charsToLookBack = 10;
		if (doc != null && lookAhead != null) {
			try {
				// go back to previous whitespace
				int docLength = doc.getLength();
				if (docLength < charsToLookBack)
					charsToLookBack = docLength - 1;
				String recentDocText = null;
				String oldContent = null;
				int caretPosition = getCaretPosition();
				recentDocText = doc.getText(0, caretPosition);

				// pass the look-ahead algorithm all of the doc except the
				// partial word you're currently working on
				// this may be a bad approach; need to look for whitespace at
				// beginning and
				// end of words, periods, etc.
				int textLength=doc.getLength();
				if (textLength > charsToLookBack) {
					lookAhead.setText(doc.getText(0, doc.getLength() - charsToLookBack));
				}

				// find last whitespace character, and make sure I keep looking
				// for that
				// same character throughout the rest of the code
				// REFACTOR THIS SECTION
				int lastFullStop = recentDocText.lastIndexOf(".");
				int lastBlank = recentDocText.lastIndexOf(" ");
				int lastTab = recentDocText.lastIndexOf("\t");
				int lastNewline = recentDocText.lastIndexOf("\n");
				int lastWhitespaceLoc = 0;
				String lastWhitespaceString = "";
				if (lastBlank > lastTab && lastBlank > lastNewline && lastBlank > lastFullStop) {
					lastWhitespaceLoc = lastBlank;
					lastWhitespaceString = " ";
				} else if (lastTab > lastNewline && lastTab > lastFullStop) {
					lastWhitespaceLoc = lastTab;
					lastWhitespaceString = "\t";
				} else if (lastNewline > lastFullStop) {
					lastWhitespaceLoc = lastNewline;
					lastWhitespaceString = "\n";
				}
				else if (lastFullStop>-1){ 
					lastWhitespaceLoc = lastFullStop;
					lastWhitespaceString = ".";
				}
				int LastSentenceBreak=Math.max(lastTab,lastNewline);
				LastSentenceBreak=Math.max(LastSentenceBreak,lastFullStop);
				
				String newContent = null;
				
				if (lastWhitespaceLoc > 0 && doc.getLength() > (charsToLookBack - 1)) {
					// get caret position
					// look at last 10 characters
					int scanBackPosition = caretPosition - charsToLookBack;
					if (scanBackPosition <= 0) return;
					String recentChars = doc.getText(scanBackPosition, charsToLookBack);
					// if any characters are blanks, get the characters since
					// the last blank
					int lastWhitespacePosition = recentChars
							.lastIndexOf(lastWhitespaceString);
					if (lastWhitespacePosition <= 0) return;
					String charsSinceLastBlank = recentChars.substring(lastWhitespacePosition + 1, charsToLookBack);

					newContent = lookAhead.doLookAhead(charsSinceLastBlank);
					
					if (newContent != null) {
						int lengthOfAddedContent = newContent.length() - charsSinceLastBlank.length();
						String newContentSubstring = newContent.substring(
								charsSinceLastBlank.length(),
								newContent.length());
						doc.insertString(caretPosition, newContentSubstring, null);

						// highlight the added text
						endOfCurrentSuggestion = caretPosition + lengthOfAddedContent;
						setCaretPosition(caretPosition + lengthOfAddedContent);
						moveCaretPosition(caretPosition);
					}
				} else {
					oldContent = recentDocText;
					newContent = lookAhead.doLookAhead(oldContent);
					if (newContent != null) {
						int lengthOld = oldContent.length();
						String newContentSubstring = newContent.substring(lengthOld);
						doc.insertString(caretPosition, newContentSubstring, null);

						// highlight the added text
						setCaretPosition(newContent.length());
						moveCaretPosition(oldContent.length());
					}
				}

			} catch (BadLocationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	protected TextLookAhead lookAhead;

	// TODO move this out of here
	public interface TextLookAhead {
		public String doLookAhead(String key);
		public String doLookAheadSentence(String sentenceKey);
		public String[] GetSuggestions(String sentenceKey);
		public String[] GetAlternaticeSentence(String sentenceKey);
		public void setText(String text);
		public void addWord(String word);
	}

}
