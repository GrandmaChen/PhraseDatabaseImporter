package Tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class PhraseGetter {

	public static List<Phrase> result;

	static public String[] initials;
	public static String[] finals;
	public static int[] tones;

	public static QueryRunner queryRunner = new QueryRunner();
	public static QueryRunner pinyinRunner = new QueryRunner();

	class PinyinResultSetHandler implements ResultSetHandler {

		@Override
		public Object handle(ResultSet resultSet) throws SQLException {
			if (resultSet.next())
				return resultSet.getString(3);
			else
				return null;
		}
	}

	public List<String> getCharactersStringList() {

		List<String> strArr = new ArrayList<String>();

		for (Phrase phrase : result)
			strArr.add(phrase.getCharacters());

		return strArr;

	}

	public static void test() {

		for (String s : initials)
			System.out.println("initials: " + s);

		for (String s : finals)
			System.out.println("finals: " + s);

		for (int i : tones)
			System.out.println("tones: " + i);

	}

	public static void getDictionaryContent(String dictionaryName, java.sql.Connection connection) throws SQLException {

		result = new ArrayList<Phrase>();
		queryRunner.query(connection, "SELECT * FROM " + dictionaryName, new ResultSetHandler<List<Phrase>>() {

			@Override
			public List<Phrase> handle(ResultSet resultSet) throws SQLException {

				while (resultSet.next()) {
					String characters = resultSet.getString(1);
					int length = resultSet.getInt(2);

					String sql = "SELECT LAST_INITIAL_1, LAST_FINAL_1, LAST_TONE_1, LAST_INITIAL_2, LAST_FINAL_2, LAST_TONE_2 FROM "
							+ dictionaryName + " WHERE CHARACTERS='" + characters + "'";

					pinyinRunner.query(connection, sql, new ResultSetHandler<String[]>() {

						@Override
						public String[] handle(ResultSet arg0) throws SQLException {

							PhraseGetter.initials = new String[2];
							PhraseGetter.finals = new String[2];
							PhraseGetter.tones = new int[2];

							PhraseGetter.initials[0] = resultSet.getString(3);
							PhraseGetter.initials[1] = resultSet.getString(6);

							PhraseGetter.finals[0] = resultSet.getString(4);
							PhraseGetter.finals[1] = resultSet.getString(7);

							PhraseGetter.tones[0] = resultSet.getInt(5);
							PhraseGetter.tones[1] = resultSet.getInt(8);

							return null;
						}
					});

					Phrase phrase = new Phrase(characters, initials, finals, tones);
					result.add(phrase);

				}

				return null;
			}
		});

	}

}
