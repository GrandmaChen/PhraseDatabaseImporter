package Tools;

public class PinyinParser {

	public static String[] yunmus = new String[] { "iang", "uang", "iong", "ang", "ian", "eng", "iao", "uai", "uan",
			"üan", "ong", "ai", "ei", "ao", "ou", "an", "en", "er", "ia", "ie", "iu", "in", "ua", "uo", "ui", "un",
			"üe", "ün", "i", "u", "ü", "a", "o", "e" };

	public static int getIndex(String pinyin) {

		for (String s : yunmus) {
			if (pinyin.contains(s)) {
				return pinyin.indexOf(s);
			}
		}
		return 0;
	}

	public static String[] parseEach(String pinyin) {

		int splitLine = getIndex(pinyin);
		String[] parsedPinyin = new String[2];

		String shengmu = pinyin.substring(0, splitLine);
		String yunmu = pinyin.substring(splitLine);

		if ((shengmu.equals("j") || shengmu.equals("q") || shengmu.equals("x")) && yunmu.equals("u"))
			yunmu = "ü";
		if ((shengmu.equals("j") || shengmu.equals("q") || shengmu.equals("x")) && yunmu.equals("uan"))
			yunmu = "üan";
		if ((shengmu.equals("z") || shengmu.equals("c") || shengmu.equals("s")) && yunmu.equals("i"))
			yunmu = "-i(z/c/s)";
		if ((shengmu.equals("zh") || shengmu.equals("ch") || shengmu.equals("sh")) && yunmu.equals("i"))
			yunmu = "-i(zh/ch/sh/r)";

		if (shengmu.equals(""))
			parsedPinyin[0] = "null";
		else
			parsedPinyin[0] = shengmu;
		parsedPinyin[1] = yunmu;

		return parsedPinyin;
	}

	public static Phrase parse(String rawPinyinString, String characters) {

		String[] undividedStrings = rawPinyinString.split("\\s+");

		int length = undividedStrings.length;

		String[] shengmus = new String[length];
		String[] yunmus = new String[length];
		int[] tones = new int[length];

		for (int i = 0; i < length; i++) {

			String tempString = undividedStrings[i];

			String[] result = parseEach(tempString.substring(0, tempString.length() - 1));

			// Store shengmus, yunmus and tones in reverse
			shengmus[length - 1 - i] = result[0];
			yunmus[length - 1 - i] = result[1];
			tones[length - 1 - i] = Integer.parseInt(tempString.substring(tempString.length() - 1));
		}

		return new Phrase(characters, shengmus, yunmus, tones);

	}

	// Just to test if yunmus is in a correct order, but will not be execute
	// when running
	public static boolean yunmusTest() {

		boolean flag = true;
		for (int i = 0; i < yunmus.length; i++) {
			for (int j = i + 1; j < yunmus.length; j++) {
				if (yunmus[j].contains(yunmus[i])) {

					System.out.println(yunmus[j] + " " + yunmus[i]);

					flag = false;
					break;
				}
			}
		}
		return flag;
	}

}
