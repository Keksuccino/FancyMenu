package de.keksuccino.fancymenu.keybinding;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class Keybinding {

	public static KeyBinding KeyReloadMenu;
	public static KeyBinding KeyToggleHelper;
	
	//TODO übernehmen
//	private static boolean isCtrlDown = false;
	
	public static void init() {
		//TODO übernehmen
		KeyReloadMenu = new KeyBinding("Reload Menu | CTRL + ALT + ", 82, "FancyMenu");
		ClientRegistry.registerKeyBinding(KeyReloadMenu);

		//TODO übernehmen
		KeyToggleHelper = new KeyBinding("Toggle Customization Overlay | CTRL + ALT + ", 67, "FancyMenu");
		ClientRegistry.registerKeyBinding(KeyToggleHelper);
		
		initGuiClickActions();
	}
	
	private static void initGuiClickActions() {
		//TODO übernehmen
//		KeyboardHandler.addKeyReleasedListener((c) -> {
//			if (c.keycode == 341) {
//				isCtrlDown = false;
//			}
//		});
//		KeyboardHandler.addKeyPressedListener((c) -> {
//			if (c.keycode == 341) {
//				isCtrlDown = true;
//			}
//		});
		
		//It's not possible in GUIs to check for keypresses via Keybinding.isPressed(), so I'm doing it on my own
		KeyboardHandler.addKeyPressedListener((c) -> {
			//TODO übernehmen (ctrl + alt)
			if ((KeyReloadMenu.getKey().getKeyCode() == c.keycode) && KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed()) {
				CustomizationHelper.reloadSystemAndMenu();
			}

			//TODO übernehmen (ctrl + alt)
			if ((KeyToggleHelper.getKey().getKeyCode() == c.keycode) && KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed()) {
				try {
					if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
						FancyMenu.config.setValue("showcustomizationbuttons", false);
					} else {
						FancyMenu.config.setValue("showcustomizationbuttons", true);
					}
					FancyMenu.config.syncConfig();
					//TODO übernehmen
					CustomizationHelper.updateUI();
				} catch (InvalidValueException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
