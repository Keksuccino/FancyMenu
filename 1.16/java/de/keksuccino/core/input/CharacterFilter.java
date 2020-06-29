package de.keksuccino.core.input;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class CharacterFilter {
	
	private List<Character> allowed = new ArrayList<Character>();
	private List<Character> forbidden = new ArrayList<Character>();
	
	public boolean isAllowed(char c) {
		if (!this.allowed.isEmpty()) {
			return this.allowed.contains(c);
		}
		if (this.forbidden.contains(c)) {
			return false;
		}
		return true;
	}
	
	public boolean isAllowed(@Nonnull String charString) {
		if ((charString == null) || (charString.length() < 1)) {
			return true;
		}
		return this.isAllowed(charString.charAt(0));
	}
	
	/**
	 * Returns the given {@link String} without forbidden characters.
	 */
	public String filterForAllowedChars(String text) {
		String s = "";
		if (text == null) {
			return s;
		}
		for (int i = 0; i < text.length(); i++) {
			if (this.isAllowed(text.charAt(i))) {
				s += text.charAt(i);
			}
		}
		return s;
	}
	
	public void addAllowedCharacters(char... chars) {
		for (char c : chars) {
			if (!this.allowed.contains(c)) {
				this.allowed.add(c);
			}
		}
	}
	
	public void addAllowedCharacters(@Nonnull String... chars) {
		for (String s : chars) {
			if ((s == null) || (s.length() < 1)) {
				continue;
			}
			if (!this.allowed.contains(s.charAt(0))) {
				this.allowed.add(s.charAt(0));
			}
		}
	}
	
	public void addForbiddenCharacters(char... chars) {
		for (char c : chars) {
			if (!this.forbidden.contains(c)) {
				this.forbidden.add(c);
			}
		}
	}
	
	public void addForbiddenCharacters(@Nonnull String... chars) {
		for (String s : chars) {
			if ((s == null) || (s.length() < 1)) {
				continue;
			}
			if (!this.forbidden.contains(s.charAt(0))) {
				this.forbidden.add(s.charAt(0));
			}
		}
	}
	
	/**
	 * Returns a new {@link CharacterFilter} instance which only allows characters used to write {@link Double} values.
	 */
	public static CharacterFilter getDoubleCharacterFiler() {
		CharacterFilter f = new CharacterFilter();
		f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "-", "+");
		return f;
	}
	
	/**
	 * Returns a new {@link CharacterFilter} instance which only allows characters used to write {@link Integer} values.
	 */
	public static CharacterFilter getIntegerCharacterFiler() {
		CharacterFilter f = new CharacterFilter();
		f.addAllowedCharacters("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-", "+");
		return f;
	}
	
	public static CharacterFilter getBasicFilenameCharacterFilter() {
		CharacterFilter f = new CharacterFilter();
		f.addAllowedCharacters(
				"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
				"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "_", "-");
		return f;
	}

}
