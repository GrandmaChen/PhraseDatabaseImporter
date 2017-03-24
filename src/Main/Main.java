package Main;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.w3c.dom.*;

import Tools.JDBCTools;
import Tools.Phrase;
import Tools.PhraseGetter;
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

public class Main {

	public static String path;
	public static Connection connection;
	public static String sql;
	public static QueryRunner queryRunner;

	public static List<String> tone3length2;

	public static DocumentBuilderFactory domFactory;
	public static DocumentBuilder builder;
	public static XPath xpath;

	public static XPathExpression inputStringExpression;
	public static XPathExpression outputStringExpression;

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

	public static Phrase pronunciationFilter(Phrase phrase) {

		int[] tones = phrase.getTone();
		String characters = phrase.getCharacters();

		// If any sub phrase meets any of double tone3 phrase
		for (int i = 0; i < tones.length; i++) {

			if (i + 1 < tones.length && tone3length2.contains(characters.substring(i, i + 1))) {
				tones[i] = 2;
				i++;
			}

		}

		// Clean all double tone3 phrase pronunciation

		for (int i = 0; i < tones.length; i++) {

			if (i + 1 < tones.length && tones[i] == 3 && tones[i + 1] == 3) {

				tones[i] = 2;

			}

		}

		return new Phrase(characters, phrase.getShengmus(), phrase.getYunmus(), tones);

	}

	public static void scanAndInput(String fileName, String hex) throws Exception {

		List<Phrase> database = new ArrayList<Phrase>();

		Document doc = builder.parse(new File(path + "/" + fileName));

		NodeList phrasePinyins = (NodeList) inputStringExpression.evaluate(doc, XPathConstants.NODESET);
		NodeList phraseCharacters = (NodeList) outputStringExpression.evaluate(doc, XPathConstants.NODESET);

		int maxLength = 0;

		if (phrasePinyins.getLength() == phraseCharacters.getLength()) {

			// Get phrases from XML
			for (int i = 0; i < phrasePinyins.getLength(); i++) {

				String characters = phraseCharacters.item(i).getNodeValue();

				// Check if this phrase is valid
				if (isChineseCharacter(characters) && !contains(database, characters)) {

					// Filter
					Phrase phrase = pronunciationFilter(
							PinyinParser.parse(phrasePinyins.item(i).getNodeValue(), characters));
					database.add(phrase);

					if (phrase.getCharacters().length() > maxLength)
						maxLength = phrase.getCharacters().length();
				}

				System.out.println("Parsing dictionary:" + fileName + " " + i + "/" + phraseCharacters.getLength());
			}

			// Add this into catalogue
			sql = "INSERT INTO catalogue (hex_code, name, quantity, source, max_length) VALUES (?, ?, ?, ?, ?)";
			queryRunner.update(connection, sql, hex, fileName.substring(0, fileName.length() - 4), database.size(),
					"搜狗细胞词库", maxLength);

			// Create a new table
			sql = "CREATE TABLE " + hex + "( characters VARCHAR (" + maxLength + ") NOT NULL, length INT(" + maxLength
					+ ") NOT NULL,";

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
				sql = "INSERT INTO " + hex + " (characters, length,";

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
				// queryRunner.update(connection, sql, params.toArray());
			}
			queryRunner.batch(connection, sql, params);

		} else {
			System.out.println("File " + fileName + " content error!");
		}
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

		path = "D:/phrasedatabase/result";

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

		PhraseGetter pg = new PhraseGetter();
		pg.getDictionaryContent("tone3length2", connection);

		tone3length2 = pg.getCharactersStringList();

		sql = "";
		queryRunner = new QueryRunner();

		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();

		int index = 0;

		// Input file into database
		for (File file : listOfFiles) {

			System.out.println("Parsing dictionary:" + file.getName() + " " + index + "/" + listOfFiles.length);
			index++;

			// Check if file is empty
			BufferedReader br = new BufferedReader(new FileReader(file));
			if (br.readLine() == null || br.readLine() == null) {
				continue;
			}

			// Hash phraseDatabase's name
			String hex = "g" + DigestUtils.md5Hex(file.getName());
			sql = "SELECT * FROM CATALOGUE WHERE hex_code = '" + hex + "';";

			// If this database exists, skip it
			if (queryRunner.query(connection, sql, new HexResultSetHandler()) != null)
				continue;

			// Add it into phrase database
			else {
				scanAndInput(file.getName(), hex);
			}
		}

	}

}
