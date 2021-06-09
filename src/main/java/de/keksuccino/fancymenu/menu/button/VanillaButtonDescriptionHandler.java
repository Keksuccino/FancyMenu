package de.keksuccino.fancymenu.menu.button;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class VanillaButtonDescriptionHandler {
	
	private static Map<GuiButton, String> descriptions = new HashMap<GuiButton, String>();
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new VanillaButtonDescriptionHandler());
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		descriptions.clear();
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreen(DrawScreenEvent.Post e) {
		for (Map.Entry<GuiButton, String> m : descriptions.entrySet()) {
			if (m.getKey().isMouseOver()) {
				renderDescription(e.getMouseX(), e.getMouseY(), m.getValue());
				break;
			}
		}
	}
	
	public static void setDescriptionFor(GuiButton w, String desc) {
		descriptions.put(w, desc);
	}
	
	private static void renderDescriptionBackground(int x, int y, int width, int height) {
		Gui.drawRect(x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	private static void renderDescription(int mouseX, int mouseY, String desc) {
		if (desc != null) {
			int width = 10;
			int height = 10;

			String[] descArray = StringUtils.splitLines(desc, "%n%");
			
			//Getting the longest string from the list to render the background with the correct width
			for (String s : descArray) {
				int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(s) + 10;
				if (i > width) {
					width = i;
				}
				height += 10;
			}

			mouseX += 5;
			mouseY += 5;
			
			if (Minecraft.getMinecraft().currentScreen.width < mouseX + width) {
				mouseX -= width + 10;
			}
			
			if (Minecraft.getMinecraft().currentScreen.height < mouseY + height) {
				mouseY -= height + 10;
			}

			RenderUtils.setZLevelPre(600);
			
			renderDescriptionBackground(mouseX, mouseY, width, height);

			GlStateManager.enableBlend();

			int i2 = 5;
			for (String s : descArray) {
				Minecraft.getMinecraft().currentScreen.drawString(Minecraft.getMinecraft().fontRenderer, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
				i2 += 10;
			}
			
			RenderUtils.setZLevelPost();
			
			GlStateManager.disableBlend();
		}
	}
	
}
