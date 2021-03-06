// This file is part of AceWiki.
// Copyright 2008-2013, AceWiki developers.
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

package ch.uzh.ifi.attempto.acewiki.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nextapp.echo.app.Column;
import nextapp.echo.app.Extent;
import nextapp.echo.app.Font;
import nextapp.echo.app.Insets;
import nextapp.echo.app.event.ActionEvent;
import nextapp.echo.app.event.ActionListener;
import ch.uzh.ifi.attempto.acewiki.Wiki;
import ch.uzh.ifi.attempto.acewiki.core.LanguageUtils;
import ch.uzh.ifi.attempto.acewiki.core.OntologyElement;
import ch.uzh.ifi.attempto.acewiki.core.ModuleElement;
import ch.uzh.ifi.attempto.echocomp.LocaleResources;
import ch.uzh.ifi.attempto.echocomp.SolidLabel;
import ch.uzh.ifi.attempto.echocomp.VSpace;

/**
 * This class represents an page that shows an index of all articles that exist in the wiki.
 * 
 * @author Tobias Kuhn
 */
public class IndexPage extends WikiPage implements ActionListener {

	private static final long serialVersionUID = 6061966610996079528L;
	
	private static final int pageSize = 50;

	// Group by first letter if there are more then this number of articles:
	private static final int simpleViewSize = 100;

	private String chosenChar = "";
	private int chosenPage = 0;
	private HashMap<String, OntologyElement> entries = new HashMap<String, OntologyElement>();
	
	private Column indexColumn = new Column();
	private IndexBar letterIndexBar;
	private IndexBar numberIndexBar;
	
	/**
	 * Creates a new index page.
	 * 
	 * @param wiki The wiki instance.
	 */
	public IndexPage(Wiki wiki) {
		super(wiki);
	}
	
	protected void doUpdate() {

		removeAll();

		setTabRow(TabRow.getMainTabRow(TabRow.TAB_INDEX, getWiki()));

		add(new Title(getWiki().getGUIText("acewiki_page_index"), true));
		addHorizontalLine();
		add(new VSpace(20));
		
		if (getWiki().getOntologyElements().size() > simpleViewSize) {
			if (chosenChar.isEmpty()) chosenChar = "A";
			letterIndexBar = new IndexBar(this);
			letterIndexBar.setActiveButton(chosenChar.charAt(0) - 65);
			add(letterIndexBar);
		}
		
		numberIndexBar = new IndexBar(0, this);
		add(numberIndexBar);
		
		indexColumn.setInsets(new Insets(10, 5, 5, 20));
		indexColumn.setCellSpacing(new Extent(2));
		add(indexColumn);
		
		indexColumn.removeAll();
		
		entries.clear();
		boolean gi = getWiki().getConfig().isGrammarIntegrationEnabled();
		for (OntologyElement e : getWiki().getOntologyElements()) {
			if (!gi && e instanceof ModuleElement) {
				continue;
			}
			for (String indexWord : e.getHeadwords()) {
				if (indexWord.toUpperCase().startsWith(chosenChar)) {
					entries.put(indexWord, e);
				}
			}
		}
		
		if (entries.size() == 0) {
			numberIndexBar.setVisible(false);
			indexColumn.add(new SolidLabel(
					getWiki().getGUIText("acewiki_list_empty"),
					Font.ITALIC,
					10
				));
		} else {
			int i = ((entries.size()-1) / pageSize) + 1;
			if (chosenPage > i) chosenPage = 0;
			numberIndexBar.setNumbers(i);
			numberIndexBar.setActiveButton(chosenPage);
			updatePage();
		}
	}
	
	private void updatePage() {
		indexColumn.removeAll();
		
		List<String> indexWords = new ArrayList<String>(entries.keySet());
		Collections.sort(indexWords, LocaleResources.getCollator());
		
		numberIndexBar.setVisible(entries.size() > pageSize);
		
		int max = entries.size();
		if (max > (chosenPage + 1) * pageSize) max = (chosenPage + 1) * pageSize;
		
		for (int i = chosenPage * pageSize; i < max; i++) {
			String t = indexWords.get(i);
			OntologyElement el = entries.get(t);
			t = LanguageUtils.getPrettyPrinted(t);
			indexColumn.add(new ListItem(new WikiLink(el, t, getWiki(), false)));
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == letterIndexBar) {
			chosenChar = e.getActionCommand();
			log("page", "pressed: first letter " + chosenChar);
			chosenPage = 0;
			update();
		} else if (e.getSource() == numberIndexBar) {
			chosenPage = Integer.parseInt(e.getActionCommand()) - 1;
			log("page", "pressed: page " + (chosenPage+1));
			updatePage();
		}
	}
	
	public boolean equals(Object obj) {
		return obj instanceof IndexPage;
	}
	
	public String toString() {
		return "-INDEX-";
	}

}
