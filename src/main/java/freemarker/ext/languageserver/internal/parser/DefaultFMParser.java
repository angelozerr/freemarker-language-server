package freemarker.ext.languageserver.internal.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import freemarker.ext.languageserver.model.FMDocument;
import freemarker.ext.languageserver.model.IFMParser;
import freemarker.ext.languageserver.model.Node;

public class DefaultFMParser implements IFMParser {

	private static final IFMParser INSTANCE = new DefaultFMParser();

	private DefaultFMParser() {
	}

	public static IFMParser getInstance() {
		return INSTANCE;
	}

	@Override
	public FMDocument parse(String text) {
		Scanner scanner = FMScanner.createScanner(text);
		FMDocument fmDocument = new FMDocument(text);

		Node curr = fmDocument;
		int endTagStart = -1;
		String pendingAttribute = null;
		TokenType token = scanner.scan();
		while (token != TokenType.EOS) {
			switch (token) {
			case StartTagOpen:
				Node child = new Node(scanner.getTokenOffset(), text.length(), new ArrayList<>(), curr);
				curr.children.add(child);
				curr = child;
				break;
			case StartTag:
				curr.tag = scanner.getTokenText();
				break;
			case StartTagClose:
				curr.end = scanner.getTokenEnd(); // might be later set to end tag position
				if (curr.tag != null && isEmptyElement(curr.tag) && curr.parent != null) {
					curr.closed = true;
					curr = curr.parent;
				}
				break;
			case EndTagOpen:
				endTagStart = scanner.getTokenOffset();
				break;
			case EndTag:
				String closeTag = scanner.getTokenText().toLowerCase();
				while (!curr.isSameTag(closeTag) && curr.parent != null) {
					curr.end = endTagStart;
					curr.closed = false;
					curr = curr.parent;
				}
				if (curr != fmDocument) {
					curr.closed = true;
					curr.endTagStart = endTagStart;
				}
				break;
			case StartTagSelfClose:
				if (curr.parent != null) {
					curr.closed = true;
					curr.end = scanner.getTokenEnd();
					curr = curr.parent;
				}
				break;
			case EndTagClose:
				if (curr.parent != null) {
					curr.end = scanner.getTokenEnd();
					curr = curr.parent;
				}
				break;
			case AttributeName: {
				String attributeName = pendingAttribute = scanner.getTokenText();
				Map<String, String> attributes = curr.attributes;
				if (attributes == null) {
					curr.attributes = attributes = new HashMap<>();
				}
				attributes.put(pendingAttribute, null); // Support valueless attributes such as 'checked'
				break;
			}
			case AttributeValue: {
				String value = scanner.getTokenText();
				Map<String, String> attributes = curr.attributes;
				if (attributes != null && pendingAttribute != null) {
					attributes.put(pendingAttribute, value);
					pendingAttribute = null;
				}
				break;
			}
			}
			token = scanner.scan();
		}
		while (curr.parent != null) {
			curr.end = text.length();
			curr.closed = false;
			curr = curr.parent;
		}
		return fmDocument;
	}

	private static boolean isEmptyElement(String tag) {
		return false;
	}
}
