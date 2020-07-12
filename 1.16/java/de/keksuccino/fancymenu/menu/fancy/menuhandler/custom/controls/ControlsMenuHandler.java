package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.controls;

import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ControlsMenuHandler extends MenuHandlerBase {

	public ControlsMenuHandler() {
		super(ControlsScreen.class.getName());
	}
	
	@Override
	public void onInitPost(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			try {
				ControlsList l = new ControlsList((ControlsScreen) e.getGui(), Minecraft.getInstance(), this);
				Field f = ObfuscationReflectionHelper.findField(ControlsScreen.class, "field_146494_r");
				e.getGui().children().remove(f.get(e.getGui()));
				f.set(e.getGui(), l);
				addChildren(e.getGui(), l);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		super.onInitPost(e);
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
