package Tools;

import java.util.Arrays;

public class Phrase {

	private String characters;
	private String[] shengmus;
	private String[] yunmus;
	private int[] tone;

	public Phrase(String characters, String[] shengmus, String[] yunmus, int[] tones) {
		this.characters = characters;
		this.shengmus = shengmus;
		this.yunmus = yunmus;
		this.tone = tones;
	}

	public String getCharacters() {
		return characters;
	}

	public void setCharacters(String characters) {
		this.characters = characters;
	}

	public String[] getShengmus() {
		return shengmus;
	}

	public void setShengmus(String[] shengmus) {
		this.shengmus = shengmus;
	}

	public String[] getYunmus() {
		return yunmus;
	}

	public void setYunmus(String[] yunmus) {
		this.yunmus = yunmus;
	}

	public int[] getTone() {
		return tone;
	}

	public void setTone(int[] tone) {
		this.tone = tone;
	}

	@Override
	public String toString() {
		return "Phrase [characters=" + characters + ", shengmus=" + Arrays.toString(shengmus) + ", yunmus="
				+ Arrays.toString(yunmus) + ", tone=" + Arrays.toString(tone) + "]";
	}

}
