package de.keksuccino.core.input;

import java.util.Arrays;
import java.util.List;

public class StringUtils {
	
	/**
	 * Returns the given {@link String} with converted format codes.
	 */
	public static String convertFormatCodes(String input, String oldPrefix, String newPrefix) {
		return input.replaceAll(oldPrefix + "0", newPrefix + "0")
				.replaceAll(oldPrefix + "1", newPrefix + "1")
				.replaceAll(oldPrefix + "2", newPrefix + "2")
				.replaceAll(oldPrefix + "3", newPrefix + "3")
				.replaceAll(oldPrefix + "4", newPrefix + "4")
				.replaceAll(oldPrefix + "5", newPrefix + "5")
				.replaceAll(oldPrefix + "6", newPrefix + "6")
				.replaceAll(oldPrefix + "7", newPrefix + "7")
				.replaceAll(oldPrefix + "8", newPrefix + "8")
				.replaceAll(oldPrefix + "9", newPrefix + "9")
				.replaceAll(oldPrefix + "a", newPrefix + "a")
				.replaceAll(oldPrefix + "b", newPrefix + "b")
				.replaceAll(oldPrefix + "c", newPrefix + "c")
				.replaceAll(oldPrefix + "d", newPrefix + "d")
				.replaceAll(oldPrefix + "e", newPrefix + "e")
				.replaceAll(oldPrefix + "f", newPrefix + "f")
				.replaceAll(oldPrefix + "k", newPrefix + "k")
				.replaceAll(oldPrefix + "l", newPrefix + "l")
				.replaceAll(oldPrefix + "m", newPrefix + "m")
				.replaceAll(oldPrefix + "n", newPrefix + "n")
				.replaceAll(oldPrefix + "o", newPrefix + "o")
				.replaceAll(oldPrefix + "r", newPrefix + "r");
	}
	
	public static String replaceAllExceptOf(String in, String replaceWith, String... keepChars) {
		String s = "";
		List<String> l = Arrays.asList(keepChars);

		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (l.contains(String.valueOf(c))) {
				s += c;
			} else {
				s += replaceWith;
			}
		}
		
		return s;
	}

}
