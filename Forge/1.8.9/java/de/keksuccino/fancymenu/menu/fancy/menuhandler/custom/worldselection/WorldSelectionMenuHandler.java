package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WorldSelectionMenuHandler extends MenuHandlerBase {

	private WorldSelectionMenuList list;
	private List<GuiButton> buttonList;
	private String guiLabel = "";
	
	public WorldSelectionMenuHandler() {
		super(GuiSelectWorld.class.getName());
	}
	
	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (this.shouldCustomize(e.gui) && MenuCustomization.isMenuCustomizable(e.gui)) {
			try {
				this.guiLabel = I18n.format("selectWorld.title", new Object[0]);
				
				this.list = new WorldSelectionMenuList((GuiSelectWorld) e.gui, this);
				this.list.registerScrollButtons(4, 5);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	@SubscribeEvent
	public void onDrawScreenPre(DrawScreenEvent.Post e) {
		if (this.shouldCustomize(e.gui) && MenuCustomization.isMenuCustomizable(e.gui)) {
			
			if (this.list != null) {
				
				this.list.drawScreen(e.mouseX, e.mouseY, e.renderPartialTicks);

				e.gui.drawCenteredString(Minecraft.getMinecraft().fontRendererObj, this.guiLabel, e.gui.width / 2, 20, 16777215);

				if (this.buttonList == null) {
					this.buttonList = getButtonList(e.gui);
				}
				if (this.buttonList != null) {
					for (int i = 0; i < this.buttonList.size(); ++i) {
						((GuiButton)this.buttonList.get(i)).drawButton(Minecraft.getMinecraft(), e.mouseX, e.mouseY);
					}
				}
				
			}
			
		}
	}
	
	@SubscribeEvent
	public void onMouseInputPre(MouseInputEvent.Pre e) {
		if (this.shouldCustomize(e.gui) && MenuCustomization.isMenuCustomizable(e.gui)) {
			if (this.list != null) {
				this.list.handleMouseInput();
			}
		}
	}
	
	@SubscribeEvent
	public void onActionPerformedPre(ActionPerformedEvent.Pre e) {
		if (this.shouldCustomize(e.gui) && MenuCustomization.isMenuCustomizable(e.gui)) {
			if (this.list != null) {
				this.list.actionPerformed(e.button);
			}
		}
	}
	
	private static List<GuiButton> getButtonList(GuiScreen s) {
		List<GuiButton> l = new ArrayList<GuiButton>();
		try {
			Field f = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
			if (f != null) {
				l = (List<GuiButton>) f.get(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}

}
