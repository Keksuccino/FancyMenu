package de.keksuccino.fancymenu.menu.button;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.math.MatrixStack;

public class VanillaButtonDescriptionHandler {
	
	private static Map<AbstractButtonWidget, String> descriptions = new HashMap<AbstractButtonWidget, String>();
	
	public static void init() {
		Konkrete.getEventHandler().registerEventsFrom(new VanillaButtonDescriptionHandler());
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		descriptions.clear();
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreen(DrawScreenEvent.Post e) {
		for (Map.Entry<AbstractButtonWidget, String> m : descriptions.entrySet()) {
			if (m.getKey().isHovered()) {
				renderDescription(e.getMatrixStack(), e.getMouseX(), e.getMouseY(), m.getValue());
				break;
			}
		}
	}
	
	public static void setDescriptionFor(AbstractButtonWidget w, String desc) {
		descriptions.put(w, desc);
	}
	
	private static void renderDescriptionBackground(MatrixStack matrix, int x, int y, int width, int height) {
		DrawableHelper.fill(matrix, x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	private static void renderDescription(MatrixStack matrix, int mouseX, int mouseY, String desc) {
		if (desc != null) {
			int width = 10;
			int height = 10;
			
			String[] descArray = StringUtils.splitLines(desc, "&n");
			
			//Getting the longest string from the list to render the background with the correct width
			for (String s : descArray) {
				int i = MinecraftClient.getInstance().textRenderer.getWidth(s) + 10;
				if (i > width) {
					width = i;
				}
				height += 10;
			}

			mouseX += 5;
			mouseY += 5;
			
			if (MinecraftClient.getInstance().currentScreen.width < mouseX + width) {
				mouseX -= width + 10;
			}
			
			if (MinecraftClient.getInstance().currentScreen.height < mouseY + height) {
				mouseY -= height + 10;
			}
			
			RenderUtils.setZLevelPre(matrix, 600);

			renderDescriptionBackground(matrix, mouseX, mouseY, width, height);

			RenderSystem.enableBlend();

			int i2 = 5;
			for (String s : descArray) {
				DrawableHelper.drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
				i2 += 10;
			}
			
			RenderUtils.setZLevelPost(matrix);
			
			RenderSystem.disableBlend();
		}
	}
	
}
