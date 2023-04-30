package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.customization.customizationgui.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.KeyMapping;

public class KeyMappings {

	public static final KeyMapping KEY_MAPPING_RELOAD_MENU = new KeyMapping("fancymenu.keybinding.keybind.reload_mod", 82, "fancymenu.keybinding.category");
	public static final KeyMapping KEY_MAPPING_TOGGLE_OVERLAY = new KeyMapping("fancymenu.keybinding.keybind.toggle_overlay", 67, "fancymenu.keybinding.category");

	public static final KeyMapping[] KEY_MAPPINGS = new KeyMapping[] {
			KEY_MAPPING_RELOAD_MENU,
			KEY_MAPPING_TOGGLE_OVERLAY
	};
	
	public static boolean initialized = false;

	public static void init() {
		if (!initialized) {
			initGuiClickActions();
			initialized = true;
		}
	}

//	@SubscribeEvent
//	public static void registerKeyBinds(RegisterKeyMappingsEvent e) {
//
//		if (!initialized) {
//			KEYBIND_RELOAD_MENU = new KeyMapping("Reload Menu | CTRL + ALT + ", 82, "FancyMenu");
//			KEYBIND_TOGGLE_OVERLAY = new KeyMapping("Toggle Customization Overlay | CTRL + ALT + ", 67, "FancyMenu");
//			initGuiClickActions();
//			initialized = true;
//		}
//
//		e.register(KEYBIND_RELOAD_MENU);
//		e.register(KEYBIND_TOGGLE_OVERLAY);
//
//	}
	
	private static void initGuiClickActions() {
		//It's not possible in GUIs to check for keypresses via Keybinding.isPressed(), so I'm doing it on my own
		KeyboardHandler.addKeyPressedListener((c) -> {
			if ((Services.PLATFORM.getKeyMappingKey(KEY_MAPPING_RELOAD_MENU).getValue() == c.keycode) && KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed()) {
				CustomizationOverlay.reloadSystemAndMenu();
			}
			if ((Services.PLATFORM.getKeyMappingKey(KEY_MAPPING_TOGGLE_OVERLAY).getValue() == c.keycode) && KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed()) {
				try {
					if (FancyMenu.getConfig().getOrDefault("showcustomizationbuttons", true)) {
						FancyMenu.getConfig().setValue("showcustomizationbuttons", false);
					} else {
						FancyMenu.getConfig().setValue("showcustomizationbuttons", true);
					}
					FancyMenu.getConfig().syncConfig();
					CustomizationOverlay.updateUI();
				} catch (InvalidValueException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
