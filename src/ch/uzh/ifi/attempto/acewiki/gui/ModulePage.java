package ch.uzh.ifi.attempto.acewiki.gui;

import nextapp.echo.app.Color;
import nextapp.echo.app.Column;
import nextapp.echo.app.Component;
import nextapp.echo.app.Extent;
import nextapp.echo.app.Font;
import nextapp.echo.app.Grid;
import nextapp.echo.app.Insets;
import nextapp.echo.app.Row;
import nextapp.echo.app.event.ActionEvent;
import ch.uzh.ifi.attempto.acewiki.Wiki;
import ch.uzh.ifi.attempto.acewiki.core.Comment;
import ch.uzh.ifi.attempto.acewiki.core.ModuleElement;
import ch.uzh.ifi.attempto.acewiki.core.ModuleElement.InvalidSyntaxException;
import ch.uzh.ifi.attempto.acewiki.core.OntologyElement;
import ch.uzh.ifi.attempto.acewiki.gf.GfEngine;
import ch.uzh.ifi.attempto.acewiki.gf.TypeGfModule;
import ch.uzh.ifi.attempto.echocomp.GeneralButton;
import ch.uzh.ifi.attempto.echocomp.HSpace;
import ch.uzh.ifi.attempto.echocomp.MessageWindow;
import ch.uzh.ifi.attempto.echocomp.SimpleErrorMessageWindow;
import ch.uzh.ifi.attempto.echocomp.SolidLabel;
import ch.uzh.ifi.attempto.echocomp.Style;
import ch.uzh.ifi.attempto.echocomp.VSpace;
import ch.uzh.ifi.attempto.gfservice.GfModule;
import ch.uzh.ifi.attempto.gfservice.GfServiceException;

import com.google.common.base.Splitter;

/**
 * TODO: localize strings
 *
 * @author Kaarel Kaljurand
 * @author Tobias Kuhn
 */
public class ModulePage extends ArticlePage {

	private static final long serialVersionUID = -5592272938081004472L;

	private static final String ACTION_EDIT = "Edit module";
	private static final String ACTION_CHECK = "Check module";

	public final static Splitter SPLITTER_NL = Splitter.on('\n');

	private final ModuleElement mElement;
	private final Wiki mWiki;

	private Row mFormattedModuleContent;

	public ModulePage(ModuleElement element, Wiki wiki) {
		super(wiki, element);
		mElement = element;
		mWiki = wiki;
		getTitle().setColor(Style.specialForeground);
	}

	public OntologyElement getOntologyElement() {
		return mElement;
	}


	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (ACTION_EDIT.equals(e.getActionCommand())) {
			if (!getWiki().isEditable()) {
				getWiki().showLoginWindow();
			} else {
				EditorDialog.Builder editor = new EditorDialog.Builder(mElement.getModuleContent(), this)
				.setTitle("Module Editor")
				.setSize(600, 600)
				.setFont(new Font(Font.MONOSPACE, Font.PLAIN, new Extent(12)))
				.setPositiveButton(new Executable() {
					@Override
					public void execute(Object... args) {
						boolean success = parse();
						// If there were no syntax errors then push the grammar to the server.
						if (success) {
							GfEngine engine = (GfEngine) mWiki.getEngine();
							try {
								engine.getGfGrammar().upload(
										new GfModule(mElement.getWord(),
												mElement.getModuleContent().getText()));
							} catch (GfServiceException e) {
								mWiki.showWindow(new MessageWindow("Error", "Push to server failed."));
								e.printStackTrace();
							}
						}
					}
				});
				mWiki.showWindow(editor.create());
			}
		} else if (ACTION_CHECK.equals(e.getActionCommand())) {
			if (!getWiki().isEditable()) {
				getWiki().showLoginWindow();
			} else {
				boolean success = parse();
				if (success) {
					mWiki.showWindow(new MessageWindow("Success", "There are no syntax errors."));
				}
			}
		}
	}


	protected void doUpdate() {
		setTabRow(TabRow.getEmptyTabRow());

		Column textColumn = getTextColumn();
		textColumn.removeAll();

		Grid referencesGrid = new Grid(5);
		referencesGrid.setInsets(new Insets(0, 2, 8, 2));
		for (ModuleElement oe : mWiki.getOntology().getOntologyElements(ModuleElement.class)) {
			if (oe != mElement && oe.references(mElement)) {
				referencesGrid.add(new WikiLink(oe, oe.getWord(), mWiki, false));
			}
		}

		Row buttonRow = new Row();
		buttonRow.setCellSpacing(new Extent(10));
		buttonRow.add(new GeneralButton(ACTION_EDIT, this));
		buttonRow.add(new GeneralButton(ACTION_CHECK, this));


		textColumn.add(referencesGrid);
		textColumn.add(new VSpace(20));
		textColumn.add(buttonRow);
		textColumn.add(new VSpace(20));

		Comment grammarContent = mElement.getModuleContent();
		if (grammarContent == null || grammarContent.getText().isEmpty()) {
			mElement.replaceModuleContent(mElement.getDefaultContent());
			grammarContent = mElement.getModuleContent();
		}
		mFormattedModuleContent = getModuleColumn(grammarContent.getText());
		textColumn.add(mFormattedModuleContent);

		getTitle().setText(mElement.getWord());
	}


	private boolean parse() {
		// TODO: this blocks, do it in the background
		try {
			mElement.parse();
		} catch (InvalidSyntaxException iex) {
			Integer l = iex.getLine();
			Integer c = iex.getColumn();
			mWiki.showWindow(new SimpleErrorMessageWindow(
					"Error at line/column = " + (l == null ? "?" : l) + "/" + (c == null ? "?" : c),
					iex.getText()));
			if (l != null) highlightSyntaxError(l);
			return false;
		} catch (Exception ex) {
			mWiki.showWindow(new SimpleErrorMessageWindow("Error", ex.getMessage()));
			return false;
		}
		return true;
	}


	private void highlightSyntaxError(int nth1) {
		if (mFormattedModuleContent != null &&
				mFormattedModuleContent.getComponent(0) != null &&
				nth1 <= mFormattedModuleContent.getComponent(0).getComponentCount()) {
			mFormattedModuleContent.getComponent(0).getComponent(nth1 - 1).setBackground(Color.PINK);
		}
	}


	private Row getModuleColumn(String text) {
		Column colNumbers = new Column();
		Column colLines = new Column();
		int lineNumber = 0;
		for (String s : SPLITTER_NL.split(text)) {
			lineNumber++;
			Row rowNumber = new Row();
			rowNumber.add(makeLineNumber(lineNumber));
			colNumbers.add(rowNumber);

			Row rowLine = new Row();
			rowLine.add(markupLine(s));
			colLines.add(rowLine);
		}
		Row row = new Row();
		row.add(colNumbers);
		row.add(new HSpace(20));
		row.add(colLines);
		return row;
	}


	private Component markupLine(String text) {
		StringBuilder sb = new StringBuilder();
		Row row = new Row();
		for (String s : TypeGfModule.tokenizeText(text)) {
			// Check if the element is in the ontology
			OntologyElement oe = mWiki.getOntology().getElement(s); // TODO: reuse the references set from the statement
			if (oe == null) {
				sb.append(s);
			} else {
				if (sb.length() > 0) {
					row.add(makePlainText(sb.toString()));
					sb.setLength(0);
				}
				row.add(new HSpace());
				row.add(new WikiLink(oe, s, mWiki, false));
				row.add(new HSpace());
			}
		}
		row.add(makePlainText(sb.toString()));
		return row;
	}


	/**
	 * Turns the given string into a label.
	 * Empty strings also get a representation, otherwise they would not create a row.
	 */
	private static Component makePlainText(String s) {
		if (s.isEmpty()) {
			s = "\u00A0";
		}
		SolidLabel label = new SolidLabel(s, Font.PLAIN);
		label.setForeground(Style.darkForeground);
		label.setFont(new Font(Font.MONOSPACE, Font.PLAIN, new Extent(12)));
		label.setFormatWhitespace(true); // TODO: maybe this should be part of any SolidLabel
		return label;
	}


	private static SolidLabel makeLineNumber(int lineNumber) {
		SolidLabel label = new SolidLabel(String.format("%4d", lineNumber), Font.PLAIN);
		label.setForeground(new Color(120, 120, 120));
		label.setFont(new Font(Font.MONOSPACE, Font.PLAIN, new Extent(12)));
		label.setFormatWhitespace(true); // TODO: maybe this should be part of any SolidLabel
		return label;
	}

}