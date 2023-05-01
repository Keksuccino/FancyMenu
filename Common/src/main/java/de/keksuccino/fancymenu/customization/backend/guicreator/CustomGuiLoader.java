package de.keksuccino.fancymenu.customization.backend.guicreator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.gui.screens.Screen;

public class CustomGuiLoader {
	
	private static Map<String, PropertiesSection> screens = new HashMap<String, PropertiesSection>();
	
	/**
	 * Can be used for both loading and reloading.
	 */
	public static void loadCustomGuis() {
		screens.clear();
		
		for (String s : FileUtils.getFiles(FancyMenu.getCustomGuisDirectory().getPath())) {
			File f = new File(s);

			String identifier = null;
			String title = null;
			boolean allowesc = false;
			
			for (String s2 : FileUtils.getFileLines(f)) {
				if (s2.contains("=")) {
					String variable = s2.replace(" ", "").split("[=]", 2)[0].toLowerCase();
					String value = "";

					String rawvalue = s2.split("[=]", 2)[1];
					int i = 0;
					while (i < rawvalue.length()) {
						if (rawvalue.charAt(i) != " ".charAt(0)) {
							value = rawvalue.substring(i);
							break;
						}
						i++;
					}

					if (variable.equals("identifier")) {
						identifier = value;
					}
					
					if (variable.equals("title")) {
						title = value;
					}

					if (variable.equals("allowesc")) {
						if (value.equalsIgnoreCase("true")) {
							allowesc = true;
						}
					}
				}
			}
			
			if (identifier != null) {
				PropertiesSection sec = new PropertiesSection("customgui");
				sec.addEntry("identifier", identifier);
				sec.addEntry("title", title);
				sec.addEntry("allowesc", "" + allowesc);
				
				screens.put(identifier, sec);
			}
		}
	}
	
	public static boolean guiExists(String identifier) {
		return screens.containsKey(identifier);
	}
	
	public static CustomGuiBase getGui(String identifier, @Nullable Screen parent, @Nullable Screen overrides) {
		if (guiExists(identifier)) {
			PropertiesSection sec = screens.get(identifier);
			boolean esc = true;
			if (sec.getEntryValue("allowesc").equalsIgnoreCase("false")) {
				esc = false;
			}
			return new CustomGuiBase(sec.getEntryValue("title"), identifier, esc, parent, overrides);
		}
		return null;
	}
	
	public static CustomGuiBase getGui(String identifier, @Nullable Screen parent, @Nullable Screen overrides, Consumer<CustomGuiBase> onClose) {
		if (guiExists(identifier)) {
			PropertiesSection sec = screens.get(identifier);
			boolean esc = true;
			if (sec.getEntryValue("allowesc").equalsIgnoreCase("false")) {
				esc = false;
			}
			return new CustomGuiBase(sec.getEntryValue("title"), identifier, esc, parent, overrides) {
				@Override
				public void removed() {
					onClose.accept(this);
				}
			};
		}
		return null;
	}
	
	public static List<String> getCustomGuis() {
		List<String> l = new ArrayList<String>();
		l.addAll(screens.keySet());
		return l;
	}
	
}
