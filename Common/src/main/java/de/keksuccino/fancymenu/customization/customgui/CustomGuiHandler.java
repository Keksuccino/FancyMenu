package de.keksuccino.fancymenu.customization.customgui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class CustomGuiHandler {

	public static final File CUSTOM_GUIS_DIR = de.keksuccino.fancymenu.util.file.FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/customguis"));

	private static final Map<String, PropertyContainer> GUIS = new HashMap<>();

	public static void reloadGuis() {
		GUIS.clear();
		for (String s : FileUtils.getFiles(CUSTOM_GUIS_DIR.getPath())) {
			File f = new File(s);
			String identifier = null;
			String title = null;
			boolean allowesc = false;
			for (String s2 : FileUtils.getFileLines(f)) {
				if (s2.contains("=")) {
					String variable = s2.replace(" ", "").split("=", 2)[0].toLowerCase();
					String value = "";
					String rawValue = s2.split("=", 2)[1];
					int i = 0;
					while (i < rawValue.length()) {
						if (rawValue.charAt(i) != ' ') {
							value = rawValue.substring(i);
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
				PropertyContainer sec = new PropertyContainer("customgui");
				sec.putProperty("identifier", identifier);
				sec.putProperty("title", title);
				sec.putProperty("allowesc", "" + allowesc);
				GUIS.put(identifier, sec);
			}
		}
	}
	
	public static boolean guiExists(@NotNull String identifier) {
		return GUIS.containsKey(identifier);
	}

	@Nullable
	public static CustomGuiBase getGui(@NotNull String identifier, @Nullable Screen parent, @Nullable Screen overrides) {
		return getGui(identifier, parent, overrides, null);
	}

	@Nullable
	public static CustomGuiBase getGui(@NotNull String identifier, @Nullable Screen parent, @Nullable Screen overrides, @Nullable Consumer<CustomGuiBase> onRemove) {
		if (guiExists(identifier)) {
			PropertyContainer sec = GUIS.get(identifier);
			boolean esc = sec.getValue("allowesc").equalsIgnoreCase("true");
			return new CustomGuiBase(sec.getValue("title"), identifier, esc, parent, overrides) {
				@Override
				public void removed() {
					if (onRemove != null) onRemove.accept(this);
				}
			};
		}
		return null;
	}

	@NotNull
	public static List<String> getCustomGuis() {
		return new ArrayList<>(GUIS.keySet());
	}
	
}
