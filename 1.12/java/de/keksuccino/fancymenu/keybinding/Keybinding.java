package de.keksuccino.fancymenu.keybinding;

import de.keksuccino.core.config.exceptions.InvalidValueException;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class Keybinding {

	public static KeyBinding KeyReloadMenu;
	public static KeyBinding KeyToggleHelper;
	
	private static boolean isCtrlDown = false;
	
	public static void init() {
		KeyReloadMenu = new KeyBinding("Reload Menu | CTRL + ", 19, "FancyMenu");
		ClientRegistry.registerKeyBinding(KeyReloadMenu);
		
		KeyToggleHelper = new KeyBinding("Toggle Customization Overlay | CTRL + ", 46, "FancyMenu");
		ClientRegistry.registerKeyBinding(KeyToggleHelper);
		
		initGuiClickActions();
	}
	
	private static void initGuiClickActions() {
		KeyboardHandler.addKeyReleasedListener((c) -> {
			if (c.keycode == 29) {
				isCtrlDown = false;
			}
		});
		KeyboardHandler.addKeyPressedListener((c) -> {
			if (c.keycode == 29) {
				isCtrlDown = true;
			}
		});
		
		//It's not possible in GUIs to check for keypresses via Keybinding.isPressed(), so I'm doing it on my own
		KeyboardHandler.addKeyPressedListener((c) -> {
			if ((KeyReloadMenu.getKeyCode() == c.keycode) && isCtrlDown) {
				CustomizationHelper.getInstance().onReloadButtonPress();
			}
			
			if ((KeyToggleHelper.getKeyCode() == c.keycode) && isCtrlDown) {
				try {
					if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
						FancyMenu.config.setValue("showcustomizationbuttons", false);
					} else {
						FancyMenu.config.setValue("showcustomizationbuttons", true);
					}
					FancyMenu.config.syncConfig();
					CustomizationHelper.getInstance().updateCustomizationButtons();
				} catch (InvalidValueException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
