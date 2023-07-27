package de.keksuccino.fancymenu.customization.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.konkrete.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActionExecutor {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final File BUTTONSCRIPT_DIR = de.keksuccino.fancymenu.util.file.FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/buttonscripts"));
	public static final Map<String, ButtonScript> SCRIPTS = new HashMap<>();
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			init = true;
			EventHandler.INSTANCE.registerListenersOf(new ActionExecutor());
			updateButtonScripts();
		}
	}
	
	public static void updateButtonScripts() {
		SCRIPTS.clear();
		File[] files = BUTTONSCRIPT_DIR.listFiles();
		if (files == null) return;
		for (File script : files) {
			if (script.isFile() && script.getPath().toLowerCase().endsWith(".txt")) {
				SCRIPTS.put(Files.getNameWithoutExtension(script.getPath()), new ButtonScript(script));
			}
		}
	}
	
	public static void execute(@NotNull String action, @Nullable String value) {
		try {
			if (value != null) {
				value = PlaceholderParser.replacePlaceholders(value);
			}
			Action c = ActionRegistry.getAction(action);
			if (c != null) {
				if (c.hasValue()) {
					c.execute(value);
				} else {
					c.execute(null);
				}
			}
		} catch (Exception ex) {
			LOGGER.error("################################");
			LOGGER.error("[FANCYMENU] An error happened while trying to execute a button action!");
			LOGGER.error("[FANCYMENU] Action: " + action);
			LOGGER.error("[FANCYMENU] Value: " + value);
			LOGGER.error("################################");
			ex.printStackTrace();
		}
	}
	
	@EventListener
	public void onReloadFancyMenu(ModReloadEvent e) {
		updateButtonScripts();
	}

	public static class ButtonScript {

		public final File script;
		public final List<String> actions = new ArrayList<>();
		public final List<String> values = new ArrayList<>();

		public ButtonScript(File script) {
			this.script = script;
			for (String s : FileUtils.getFileLines(script)) {
				String action;
				String value = "";
				if (s.contains(":")) {
					action = s.split(":", 2)[0].replace(" ", "");
					value = s.split(":", 2)[1];
				} else {
					action = s.replace(" ", "");
				}
				actions.add(action);
				values.add(value);
			}
		}

		public void runScript() {
			if (!this.actions.isEmpty()) {
				for (int i = 0; i <= this.actions.size()-1; i++) {
					execute(this.actions.get(i), this.values.get(i));
				}
			}
		}

	}

	public static class ActionContainer {

		public volatile String action;
		public volatile String value;

		public ActionContainer(@NotNull String action, @Nullable String value) {
			this.action = action;
			this.value = value;
		}

		public void execute() {
			ActionExecutor.execute(this.action, this.value);
		}

	}

}
