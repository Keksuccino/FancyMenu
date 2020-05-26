package de.keksuccino.core.web;

import java.net.HttpURLConnection;
import java.net.URL;

import de.keksuccino.core.input.CharacterFilter;

public class WebUtils {
	
	public static boolean isValidUrl(String url) {
		if ((url == null) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
			return false;
		}
		
		try {
			URL u = new URL(url);
			HttpURLConnection c = (HttpURLConnection) u.openConnection();

			c.setRequestMethod("HEAD");
			int r = c.getResponseCode();
			if (r == 200) {
				return true;
			}
		} catch (Exception e) {
			System.out.println("Unable to check for valid url via HEAD request!");
			System.out.println("Trying alternative method..");
			try {
				URL u = new URL(url);
				HttpURLConnection c = (HttpURLConnection) u.openConnection();
				
				int r = c.getResponseCode();
				
				if (r == 200) {
					return true;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return false;
	}
	
	public static String filterURL(String url) {
		if (url == null) {
			return null;
		}
		CharacterFilter f = new CharacterFilter();
		f.addAllowedCharacters("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
				"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r","s", "t", "u", "v", "w", "x", "y", "z",
				"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"-", ".", "_", "~", ":", "/", "?", "#", "[", "]", "@", "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "%", "=");
		return f.filterForAllowedChars(url);
	}

}
