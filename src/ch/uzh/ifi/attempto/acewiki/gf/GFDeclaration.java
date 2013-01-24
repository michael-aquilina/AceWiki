// This file is part of AceWiki.
// Copyright 2008-2012, AceWiki developers.
//
// AceWiki is free software: you can redistribute it and/or modify it under the terms of the GNU
// Lesser General Public License as published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
//
// AceWiki is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License along with AceWiki. If
// not, see http://www.gnu.org/licenses/.

package ch.uzh.ifi.attempto.acewiki.gf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.uzh.ifi.attempto.acewiki.core.Declaration;
import ch.uzh.ifi.attempto.acewiki.core.LanguageHandler;
import ch.uzh.ifi.attempto.acewiki.core.MultilingualSentence;
import ch.uzh.ifi.attempto.acewiki.core.OntologyElement;
import ch.uzh.ifi.attempto.acewiki.core.SentenceDetail;
import ch.uzh.ifi.attempto.base.DefaultTextOperator;
import ch.uzh.ifi.attempto.base.MultiTextContainer;
import ch.uzh.ifi.attempto.base.TextContainer;
import ch.uzh.ifi.attempto.base.TextElement;
import ch.uzh.ifi.attempto.base.TextOperator;
import ch.uzh.ifi.attempto.echocomp.LocaleResources;
import ch.uzh.ifi.attempto.gfservice.GfServiceException;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This class represents a declaration statement for the GF AceWiki engine.
 * The "declaration" is a tree set that can be linearized into multiple
 * languages.
 *
 * TODO: store also the language in whose context this declaration (treeset) was created,
 * knowing this can be useful in certain situations
 *
 * TODO: move the HTML-formatting out of this class
 * 
 * @author Kaarel Kaljurand
 */
public class GFDeclaration extends MultilingualSentence implements Declaration {

	final Logger logger = LoggerFactory.getLogger(GFDeclaration.class);

	private final Joiner mLinsetJoiner = Joiner.on(" // ").skipNulls();

	private final GFGrammar mGfGrammar;

	// maps a language identifier to the set of linearizations (text containers) in this language
	private final Map<String, MultiTextContainer> textContainers = new HashMap<String, MultiTextContainer>();

	private TreeSet mTreeSet;
	private final String mLang;

	public GFDeclaration(String lang, GFGrammar gfGrammar) {
		mGfGrammar = gfGrammar;
		mLang = lang;
		try {
			mTreeSet = new TreeSet(gfGrammar.random());
		} catch (GfServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new GF declaration object from a parse state.
	 * 
	 * @param treeSet The tree set.
	 * @param gfGrammar The grammar object.
	 */
	public GFDeclaration(TreeSet treeSet, String lang, GFGrammar gfGrammar) {
		mTreeSet = treeSet;
		mGfGrammar = gfGrammar;
		mLang = lang;
	}

	/**
	 * Creates a new GF declaration object from a text in a given language.
	 *
	 * TODO: the input text should probably be in the form of a token list
	 * 
	 * @param text The declaration text.
	 * @param lh The language handler.
	 * @param gfGrammar The grammar object.
	 */
	public GFDeclaration(String text, LanguageHandler lh, GFGrammar gfGrammar) {
		String tokenText = "";
		for (String t : lh.getTextOperator().splitIntoTokens(text)) {
			tokenText += t + " ";
		}
		mGfGrammar = gfGrammar;
		mLang = lh.getLanguage();
		try {
			mTreeSet = new TreeSet(getGFGrammar().parse(tokenText, mLang));
			if (mTreeSet.size() == 0) {
				// TODO this should be done properly; see GfTextOperator
				// If parsing fails: first char to lower case
				tokenText = DefaultTextOperator.firstCharToLowerCase(tokenText);
				mTreeSet = new TreeSet(getGFGrammar().parse(tokenText, mLang));
			}
		} catch (GfServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public MultiTextContainer getTextContainer(String language) {
		MultiTextContainer mtc = textContainers.get(language);
		Set<String> seen = Sets.newHashSet();
		if (mtc == null) {
			Set<TextContainer> tmp = new HashSet<TextContainer>();
			for (String tree : mTreeSet.getTrees()) {
				Set<String> lins = getLins(tree, language);

				if (lins == null) {
					logger.info("getTextContainerSet: null {}: {}", language, tree);
					// TODO do it properly
					TextContainer tc = new TextContainer();
					tc.addElement(new TextElement("-NULL-"));
					tmp.add(tc);
				} else if (lins.isEmpty()) {
					logger.info("getTextContainerSet: 0 els {}: {}", language, tree);
					// TODO do it properly
					TextContainer tc = new TextContainer();
					tc.addElement(new TextElement("-EMPTY-"));
					tmp.add(tc);
				} else if (seen.contains(lins.iterator().next())) {
					// Don't show the same linearization twice
				} else {
					// TODO: limitation: we only work with the first linearization
					String lin = lins.iterator().next();
					seen.add(lin);
					TextOperator to = getTextOperator(language);
					TextContainer tc = new TextContainer(to);
					for (String s : to.splitIntoTokens(lin)) {
						tc.addElement(new TextElement(s));
					}
					tmp.add(tc);
				}
			}
			mtc = new MultiTextContainer(tmp);
			textContainers.put(language, mtc);
		}
		return mtc;
	}

	/**
	 * TODO
	 */
	public boolean contains(OntologyElement e) {
		return false;
	}


	/**
	 * Returns the details of this tree set:
	 * 
	 *   - abstract trees;
	 *   - translations;
	 *   - abstract tree diagram;
	 *   - parse tree diagram;
	 *   - word alignment diagram;
	 *   - ...
	 * 
	 * The output highlights the given language.
	 *
	 * TODO: everything should be hyperlinked.
	 */
	public List<SentenceDetail> getDetails(String lang) {
		List<SentenceDetail> l = new ArrayList<SentenceDetail>();

//		for (String tree : mTreeSet.getTrees()) {
//			l.addAll(formatTree(mGfGrammar, tree, lang));
//		}
		l.addAll(formatTree(mGfGrammar, lang));

		return l;
	}


	public boolean isReasonable() {
		return true;
	}


	public int getNumberOfParseTrees() {
		return mTreeSet.size();
	}


	public Set<String> getParseTrees() {
		return mTreeSet.getTrees();
	}


	public void update() {
	}

	public String serialize() {
		return GFGrammar.serialize(mTreeSet);
	}

	/**
	 * Returns the grammar object.
	 * 
	 * @return The grammar object.
	 */
	public GFGrammar getGFGrammar() {
		return mGfGrammar;
	}


	private String getAbstrtreeAsHtml(String tree) {
		try {
			return getImg(getGFGrammar().abstrtree(tree));
		} catch (GfServiceException e) {
			return getError(e.getMessage());
		}
	}


	private String getParsetreeAsHtml(String tree, String language) {
		try {
			return getImg(getGFGrammar().parsetree(tree, language));
		} catch (GfServiceException e) {
			return getError(e.getMessage());
		}
	}


	private String getAlignmentAsHtml(String tree) {
		try {
			return getImg(getGFGrammar().alignment(tree));
		} catch (GfServiceException e) {
			return getError(e.getMessage());
		}
	}


	private String getImg(String dataUri) {
		return "<a href=\"" + dataUri + "\"><img src=\"" + dataUri + "\" style=\"max-height:500px\"/></a>";
	}


	private static String getError(String message) {
		return "<p style=\"color: red\">" + message + "</p>";
	}


	private static SentenceDetail getError(String tree, String lang, String message) {
		return new SentenceDetail("ERROR", getError(tree + ": " + lang + ": " + message));
	}


	private List<SentenceDetail> formatTree(GFGrammar grammar, String lang) {
		String tree = mTreeSet.getTrees().iterator().next();
		List<SentenceDetail> l = new ArrayList<SentenceDetail>();
		l.add(new SentenceDetail("acewiki_details_syntree", getParsetreeAsHtml(tree, lang)));
		String plainTrees = "";
		for (String s : mTreeSet.getTrees()) {
			plainTrees += "<p><code>" + s + "</code></p>";
		}
		l.add(new SentenceDetail(
				LocaleResources.getString("acewiki_details_internal") + " (ASCII)",
				plainTrees
				));
		l.add(new SentenceDetail("acewiki_details_internal", getAbstrtreeAsHtml(tree)));
//		try {
//			Map<String, Set<String>> m = grammar.linearize(tree);
//			StringBuilder sb = new StringBuilder();
//			sb.append("<ul>");
//			for (String key : m.keySet()) {
//				if (key.equals(lang)) {
//					sb.append("<li style='background-color: yellow'><b>" + key + "</b>: " + m.get(key) + "</li>");
//				} else {
//					sb.append("<li><b>" + key + "</b>: " + m.get(key) + "</li>");
//				}
//			}
//			sb.append("</ul>");
//			l.add(new SentenceDetail(
//					"Translations",
//					sb.toString()
//					));
//		} catch (GfServiceException e) {
//			l.add(getError(tree, lang, "linearization failed: " + e.getMessage()));
//		}
//
//		l.add(new SentenceDetail("Word alignment", getAlignmentAsHtml(tree)));

		return l;
	}


	/*
	 * TODO: highlight the linearization that is in the language mLang
	 */
	private List<SentenceDetail> formatTranslations(GFGrammar grammar, TreeSet parseState, String currentLanguage) {
		Multimap<String, Set<String>> mm = HashMultimap.create();

		// Creating the map:
		// lang -> linset1, linset2, ..., linsetN
		// TODO: move this to a library
		for (String tree : parseState.getTrees()) {
			Map<String, Set<String>> m = null;
			try {
				m = grammar.linearize(tree);
			} catch (GfServiceException e) {
			}

			// TODO handle this better
			if (m == null) continue;

			for (String lang : m.keySet()) {
				if (! currentLanguage.equals(lang)) {
					mm.put(lang, m.get(lang));
				}
			}
		}

		// Formatting the map
		List<SentenceDetail> l = new ArrayList<SentenceDetail>();
		List<String> languageList = new ArrayList<String>(mm.keySet());
		Collections.sort(languageList);
		for (String lang : languageList) {
			StringBuilder sb = new StringBuilder();
			sb.append("<ul>");
			for (Set<String> linset : mm.get(lang)) {
				sb.append("<li>" + mLinsetJoiner.join(linset) + "</li>");
			}
			sb.append("</ul>");
			int size = mm.get(lang).size();
			String title = size == 1 ? lang : lang + " (" + size + ")";
			l.add(new SentenceDetail(title, sb.toString()));
		}
		return l;
	}


	// TODO: linearize into all the languages at once for better
	// performance
	private Set<String> getLins(String tree, String language) {
		try {
			return getGFGrammar().linearize(tree, language);
		} catch (GfServiceException e) {
			// TODO find out what happened, i.e.
			// why was the tree not supported by the grammar.
			return null;
		}
	}

}
