package de.keksuccino.fancymenu.localization;

import java.util.HashMap;
import java.util.Map;

public class LocalizationPackage {
	
	private final String language;
	private final Map<String, String> locals = new HashMap<String, String>();
	
	protected LocalizationPackage(String language) {
		this.language = language;
	}
	
	public String getLanguage() {
		return this.language;
	}
	
	public boolean isEmpty() {
		return this.locals.isEmpty();
	}
	
	public boolean containsKey(String key) {
		return locals.containsKey(key);
	}
	
	public void addLocalizedString(String key, String value) {
		this.removeLocalizedString(key);
		locals.put(key, value);
	}
	
	public void removeLocalizedString(String key) {
		if (locals.containsKey(key)) {
			locals.remove(key);
		}
	}
	
	public String getLocalizedString(String key) {
		if (locals.containsKey(key)) {
			return locals.get(key);
		}
		return null;
	}
}
