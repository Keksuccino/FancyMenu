package de.keksuccino.fancymenu.customization.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.konkrete.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Legacy("Button scripts got replaced by the new action system. Remove this in the future.")
@Deprecated
public class ButtonScriptHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final File BUTTONSCRIPT_DIR = de.keksuccino.fancymenu.util.file.FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/buttonscripts"));

	private static final Map<String, ButtonScript> SCRIPTS = new HashMap<>();
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			init = true;
			EventHandler.INSTANCE.registerListenersOf(new ButtonScriptHandler());
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

	@Nullable
	public static ButtonScript getScript(@NotNull String scriptName) {
		return SCRIPTS.get(scriptName);
	}
	
	@EventListener
	public void onReloadFancyMenu(ModReloadEvent e) {
		updateButtonScripts();
	}

	public static class ButtonScript {

		public final File scriptFile;
		public final List<ActionInstance> actions = new ArrayList<>();

		public ButtonScript(File scriptFile) {
			this.scriptFile = scriptFile;
			for (String s : FileUtils.getFileLines(scriptFile)) {
				String identifier;
				String value = "";
				if (s.contains(":")) {
					identifier = s.split(":", 2)[0].replace(" ", "");
					value = s.split(":", 2)[1];
				} else {
					identifier = s.replace(" ", "");
				}
				Action a = ActionRegistry.getAction(identifier);
				if (a != null) {
					this.actions.add(new ActionInstance(a, value));
				}
			}
		}

		public void runScript() {
			for (ActionInstance a : this.actions) {
				a.execute();
			}
		}

	}

}
