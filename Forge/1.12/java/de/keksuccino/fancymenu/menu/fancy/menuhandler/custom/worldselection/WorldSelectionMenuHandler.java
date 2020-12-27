package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WorldSelectionMenuHandler extends MenuHandlerBase {

	public WorldSelectionMenuHandler() {
		super(GuiWorldSelection.class.getName());
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			try {

				WorldSelectionMenuList l = new WorldSelectionMenuList((GuiWorldSelection) e.getGui(), Minecraft.getMinecraft(), e.getGui().width, e.getGui().height, 32, e.getGui().height - 64, 36, this);
				
				Field f = ReflectionHelper.findField(GuiWorldSelection.class, "field_184866_u", "selectionList");
				f.set(e.getGui(), l);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		super.onButtonsCached(e);
	}

}
