package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.controls;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ControlsMenuHandler extends MenuHandlerBase {

	public ControlsMenuHandler() {
		super(GuiControls.class.getName());
	}
	
	@Override
	public void onInitPost(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			try {
				ControlsList l = new ControlsList((GuiControls) e.getGui(), Minecraft.getMinecraft(), this);
				Field f = ReflectionHelper.findField(GuiControls.class, "keyBindingList", "field_146494_r");
				f.set(e.getGui(), l);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		super.onInitPost(e);
	}

}
