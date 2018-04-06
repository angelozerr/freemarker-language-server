package freemarker.ext.languageserver.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;

/**
 * A manager for simple text documents
 */
public class TextDocuments {

	private final Map<String, TextDocumentItem> documents;

	public TextDocuments() {
		documents = new HashMap<>();
	}

	/**
	 * Returns the document for the given URI. Returns undefined if the document is
	 * not mananged by this instance.
	 *
	 * @param uri The text document's URI to retrieve.
	 * @return the text document or `undefined`.
	 */
	public TextDocumentItem get(String uri) {
		return documents.get(uri);
	}

	public void onDidOpenTextDocument(DidOpenTextDocumentParams params) {
		TextDocumentItem document = params.getTextDocument();
		documents.put(document.getUri(), document);
	}

	public void onDidChangeTextDocument(DidChangeTextDocumentParams params) {
		List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
		// like vscode does, get the last changes
		// see
		// https://github.com/Microsoft/vscode-languageserver-node/blob/master/server/src/main.ts
		TextDocumentContentChangeEvent last = changes.size() > 0 ? changes.get(changes.size() - 1) : null;
		if (last != null) {
			TextDocumentItem document = get(params.getTextDocument().getUri());
			if (document != null) {
				document.setVersion(params.getTextDocument().getVersion());
				document.setText(last.getText());
			}
		}
	}

	public void onDidCloseTextDocument(DidCloseTextDocumentParams params) {
		documents.remove(params.getTextDocument().getUri());
	}
}
