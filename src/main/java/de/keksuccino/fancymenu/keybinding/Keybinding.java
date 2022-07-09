package de.keksuccino.fancymenu.keybinding;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class Keybinding {

	public static KeyMapping KeyReloadMenu;
	public static KeyMapping KeyToggleHelper;
	
	public static boolean initialized = false;

	public static void init() {

		FMLJavaModLoadingContext.get().getModEventBus().register(Keybinding.class);

	}

	@SubscribeEvent
	public static void registerKeyBinds(RegisterKeyMappingsEvent e) {

		if (!initialized) {
			KeyReloadMenu = new KeyMapping("Reload Menu | CTRL + ALT + ", 82, "FancyMenu");
			KeyToggleHelper = new KeyMapping("Toggle Customization Overlay | CTRL + ALT + ", 67, "FancyMenu");
			initGuiClickActions();
			initialized = true;
		}

		e.register(KeyReloadMenu);
		e.register(KeyToggleHelper);

	}
	
	private static void initGuiClickActions() {
		
		//It's not possible in GUIs to check for keypresses via Keybinding.isPressed(), so I'm doing it on my own
		KeyboardHandler.addKeyPressedListener((c) -> {
			if ((KeyReloadMenu.getKey().getValue() == c.keycode) && KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed()) {
				CustomizationHelper.reloadSystemAndMenu();
			}

			if ((KeyToggleHelper.getKey().getValue() == c.keycode) && KeyboardHandler.isCtrlPressed() && KeyboardHandler.isAltPressed()) {
				try {
					if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
						FancyMenu.config.setValue("showcustomizationbuttons", false);
					} else {
						FancyMenu.config.setValue("showcustomizationbuttons", true);
					}
					FancyMenu.config.syncConfig();
					CustomizationHelper.updateUI();
				} catch (InvalidValueException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
