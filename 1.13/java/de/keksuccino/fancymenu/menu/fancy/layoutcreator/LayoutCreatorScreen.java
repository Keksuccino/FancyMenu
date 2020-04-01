package de.keksuccino.fancymenu.menu.fancy.layoutcreator;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationButton;
import de.keksuccino.fancymenu.menu.fancy.layoutcreator.content.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.layoutcreator.content.LayoutObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class LayoutCreatorScreen extends GuiScreen {
	
	public final GuiScreen screen;
	private List<LayoutObject> content = new ArrayList<LayoutObject>();

	public LayoutCreatorScreen(GuiScreen screenToCustomize) {
		this.screen = screenToCustomize;
		
		//Get currently cached buttons from ButtonCache
		for (ButtonData b : ButtonCache.getButtons()) {
			content.add(new LayoutButton(b));
		}
	}
	
	@Override
	protected void initGui() {
		System.out.println("lel");
		GuiButton closeButton = new CustomizationButton(this.width - 55, 5, 50, 20, "Close", (onPress) -> {
			Minecraft.getInstance().displayGuiScreen(this.screen);
		});
		this.addButton(closeButton);
	}
	
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		
		for (LayoutObject l : this.content) {
			l.render(mouseX, mouseY);
		}
		
		super.render(mouseX, mouseY, partialTicks);
	}
	
	
	private String getIdentifier() {
		return this.screen.getClass().getName();
	}
	
}
