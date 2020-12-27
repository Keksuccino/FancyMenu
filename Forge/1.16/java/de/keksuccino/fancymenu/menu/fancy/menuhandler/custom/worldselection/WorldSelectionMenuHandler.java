package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class WorldSelectionMenuHandler extends MenuHandlerBase {

	public WorldSelectionMenuHandler() {
		super(WorldSelectionScreen.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			try {
				Field f = ObfuscationReflectionHelper.findField(WorldSelectionScreen.class, "field_212352_g");
				TextFieldWidget t = (TextFieldWidget) f.get(e.getGui());
				
				WorldSelectionMenuList l = new WorldSelectionMenuList((WorldSelectionScreen) e.getGui(), Minecraft.getInstance(), e.getGui().width, e.getGui().height, 48, e.getGui().height - 64, 36, () -> {
					return t.getText();
				}, null, this);
				
				t.setResponder((s) -> {
					l.func_212330_a(() -> {
						return s;
					}, false);
				});
				
				Field f2 = ObfuscationReflectionHelper.findField(WorldSelectionScreen.class, "field_184866_u");
				e.getGui().getEventListeners().remove(f2.get(e.getGui()));
				
				f2.set(e.getGui(), l);
				addChildren(e.getGui(), l);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		super.onButtonsCached(e);
	}
	
	private static void addChildren(Screen s, IGuiEventListener e) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230705_e_");
			((List<IGuiEventListener>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}
