package de.keksuccino.core.locale;

import java.lang.reflect.Field;
import java.util.Map;

import de.keksuccino.core.reflection.ReflectionHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;

public class LocaleUtils {
	
	/**
	 * Returns the key for the given string or null if no key with the given value was found.
	 */
	public static String getKeyForString(String s) {
		try {
			Field f = ReflectionHelper.findField(I18n.class, "field_135054_a");
			Locale l = (Locale) f.get(I18n.class);
			Field f2 = ReflectionHelper.findField(Locale.class, "field_135032_a");
			@SuppressWarnings("unchecked")
			Map<String, String> properties = (Map<String, String>) f2.get(l);
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
