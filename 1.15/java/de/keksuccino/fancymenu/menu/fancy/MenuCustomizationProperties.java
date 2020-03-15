package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.properties.PropertiesSection;
import de.keksuccino.properties.PropertiesSerializer;
import de.keksuccino.properties.PropertiesSet;

public class MenuCustomizationProperties {
	
	private static List<PropertiesSet> properties = new ArrayList<PropertiesSet>();
	
	public static void loadProperties() {
		properties.clear();
		
		File f = FancyMenu.getCustomizationPath();
		if (!f.exists()) {
			f.mkdirs();
		}
		
		for (File f2 : f.listFiles()) {
			PropertiesSet s = PropertiesSerializer.getProperties(f2.getAbsolutePath());
			if ((s != null) && s.getPropertiesType().equalsIgnoreCase("menu")) {
				List<PropertiesSection> l = s.getPropertiesOfType("type-meta");
				if (!l.isEmpty()) {
					String s2 = l.get(0).getEntryValue("identifier");
					if (s2 != null) {
						properties.add(s);
					}
				}
			}
		}
	}
	
	public static List<PropertiesSet> getProperties() {
		return properties;
	}
	
	public static List<PropertiesSet> getPropertiesWithIdentifier(String identifier) {
		List<PropertiesSet> l = new ArrayList<PropertiesSet>();
		for (PropertiesSet s : getProperties()) {
			List<PropertiesSection> l2 = s.getPropertiesOfType("type-meta");
			String s2 = l2.get(0).getEntryValue("identifier");
			if (s2.equalsIgnoreCase(identifier)) {
				l.add(s);
			}
		}
		return l;
	}

}
