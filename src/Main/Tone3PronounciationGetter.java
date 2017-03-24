package Main;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.w3c.dom.*;

import Tools.JDBCTools;
import Tools.Phrase;
import Tools.PinyinParser;

import javax.xml.xpath.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.*;

public class Tone3PronounciationGetter {

	public static String path;
	public static Connection connection;
	public static String sql;
	public static QueryRunner queryRunner;

	public static List<Phrase> mandarinPronounciationDatabase;

	public static DocumentBuilderFactory domFactory;
	public static DocumentBuilder builder;
	public static XPath xpath;

	public static XPathExpression inputStringExpression;
	public static XPathExpression outputStringExpression;

	public static List<Phrase> database;
	public static int maxLength;

	public static boolean isChineseCharacter(String str) {
		return str.codePoints()
				.allMatch(codepoint -> Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
	}

	public static boolean contains(List<Phrase> database, String characters) {
		for (Phrase phrase : database)
			if (phrase.getCharacters().equals(characters))
				return true;
		return false;
	}

	public static void scanAndInput(String fileName) throws Exception {

		Document doc = builder.parse(new File(path + "/" + fileName));

		NodeList phrasePinyins = (NodeList) inputStringExpression.evaluate(doc, XPathConstants.NODESET);
		NodeList phraseCharacters = (NodeList) outputStringExpression.evaluate(doc, XPathConstants.NODESET);

		if (phrasePinyins.getLength() == phraseCharacters.getLength()) {

			// Get phrases from XML
			for (int i = 0; i < phrasePinyins.getLength(); i++) {

				// Only find phrase with pinyin with tone 3
				String[] pinyins = phrasePinyins.item(i).getNodeValue().split("\\s+");
				boolean flag = true;

				// All pinyin end with tone 3
				for (String pinyin : pinyins)
					if (!pinyin.endsWith("3")) {
						flag = false;
						break;
					}

				if (!flag)
					continue;

				String characters = phraseCharacters.item(i).getNodeValue();

				// Check if this phrase is valid
				if (isChineseCharacter(characters) && !contains(database, characters)
						&& characters.length() == 2) {

					Phrase phrase = PinyinParser.parse(phrasePinyins.item(i).getNodeValue(), characters);
					database.add(phrase);

					if (phrase.getCharacters().length() > maxLength)
						maxLength = phrase.getCharacters().length();
				}

				System.out.println("Parsing dictionary:" + fileName + " " + i + "/" + phraseCharacters.getLength());
			}

		} else {
			System.out.println("File " + fileName + " content error!");
		}
	}

	static void restore() throws Exception {

		// Create a new table
		sql = "CREATE TABLE " + "tone3length2" + "( characters VARCHAR (" + maxLength + ") NOT NULL, length INT("
				+ maxLength + ") NOT NULL,";

		for (int i = 1; i <= maxLength; i++) {
			sql += "last_initial_" + i + " VARCHAR (10)," + "last_final_" + i + " VARCHAR (15)," + "last_tone_" + i
					+ " INT(1),";
		}

		sql += "PRIMARY KEY (characters) );";
		queryRunner.update(connection, sql);

		Object[][] params = new Object[database.size()][];
		int index = 0;
		// Input phrases
		for (Phrase phrase : database) {
			sql = "INSERT INTO " + "tone3length2" + " (characters, length,";

			int length = phrase.getCharacters().length();

			for (int i = 1; i <= maxLength; i++) {
				sql += " last_initial_" + i + ", last_final_" + i + ", last_tone_" + i + ",";
			}
			sql = sql.substring(0, sql.length() - 1);
			sql += ") VALUES (";

			for (int i = 1; i <= maxLength * 3 + 2; i++) {
				sql += "?,";
			}

			sql = sql.substring(0, sql.length() - 1) + ")";

			List<String> phraseParams = new ArrayList<String>();

			for (int i = 0; i < length; i++) {
				phraseParams.add(phrase.getShengmus()[i]);
				phraseParams.add(phrase.getYunmus()[i]);
				phraseParams.add(phrase.getTone()[i] + "");
			}

			for (int i = 0; i < (maxLength - length) * 3; i++) {
				phraseParams.add(null);
			}

			phraseParams.add(0, length + "");
			phraseParams.add(0, phrase.getCharacters());

			params[index] = phraseParams.toArray();
			index++;
		}
		queryRunner.batch(connection, sql, params);
	}

	static class HexResultSetHandler implements ResultSetHandler {

		@Override
		public Object handle(ResultSet resultSet) throws SQLException {
			if (resultSet.next())
				return resultSet.getString(3);
			else
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		path = "D:/wwwwtest";

		database = new ArrayList<Phrase>();
		maxLength = 0;

		domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);

		builder = domFactory.newDocumentBuilder();

		xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {

			@Override
			public Iterator getPrefixes(String namespaceURI) {
				return Collections.singleton("ns1").iterator();
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return "ns1";
			}

			@Override
			public String getNamespaceURI(String prefix) {
				return "http://www.microsoft.com/ime/dctx";
			}
		});

		inputStringExpression = xpath.compile("//ns1:InputString/text()");
		outputStringExpression = xpath.compile("//ns1:OutputString/text()");

		// Get connection
		connection = JDBCTools.getConnection();
		sql = "";
		queryRunner = new QueryRunner();

		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();

		System.out.println(listOfFiles.length);

		int index = 0;

		for (File file : listOfFiles) {

			System.out.println("Parsing dictionary:" + file.getName() + " " + index + "/" + listOfFiles.length);
			index++;

			// Check if file is empty
			BufferedReader br = new BufferedReader(new FileReader(file));
			if (br.readLine() == null || br.readLine() == null) {
				continue;
			}

			// Add it into phrase database
			else {
				scanAndInput(file.getName());
			}

		}

		restore();

	}

}
