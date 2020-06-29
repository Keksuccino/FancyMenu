package de.keksuccino.core.locale;

import java.util.Map;

import net.minecraft.util.text.LanguageMap;

public class LocaleUtils {
	
	/**
	 * Returns the key for the given string or null if no key with the given value was found.
	 */
	public static String getKeyForString(String s) {
		try {
			LanguageMap l = LanguageMap.getInstance();
			Map<String, String> properties = l.getLanguageData();
			for (Map.Entry<String, String> m : properties.entrySet()) {
				if (m.getValue().equals(s)) {
					return m.getKey();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
