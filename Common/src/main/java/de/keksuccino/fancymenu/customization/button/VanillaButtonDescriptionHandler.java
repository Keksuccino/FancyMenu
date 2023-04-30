package de.keksuccino.fancymenu.customization.button;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventPriority;
import de.keksuccino.fancymenu.event.acara.SubscribeEvent;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;

public class VanillaButtonDescriptionHandler {
	
	private static final Map<AbstractWidget, String> DESCRIPTIONS = new HashMap<>();
	
	public static void init() {
		EventHandler.INSTANCE.registerListenersOf(new VanillaButtonDescriptionHandler());
	}

	@SubscribeEvent(priority = EventPriority.HIGHER)
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		DESCRIPTIONS.clear();
	}
	
	@SubscribeEvent(priority = -100)
	public void onDrawScreen(RenderScreenEvent.Post e) {
		for (Map.Entry<AbstractWidget , String> m : DESCRIPTIONS.entrySet()) {
			if (m.getKey().isHoveredOrFocused()) {
				renderDescription(e.getPoseStack(), e.getMouseX(), e.getMouseY(), m.getValue());
				break;
			}
		}
	}
	
	public static void setDescriptionFor(AbstractWidget w, String desc) {
		DESCRIPTIONS.put(w, desc);
	}
	
	private static void renderDescriptionBackground(PoseStack matrix, int x, int y, int width, int height) {
		GuiComponent.fill(matrix, x, y, x + width, y + height, new Color(26, 26, 26, 250).getRGB());
	}
	
	private static void renderDescription(PoseStack matrix, int mouseX, int mouseY, String desc) {

		if (desc == null) return;

		int width = 10;
		int height = 10;

		String[] descArray = StringUtils.splitLines(desc, "%n%");

		//Getting the longest string from the list to render the background with the correct width
		for (String s : descArray) {
			int i = Minecraft.getInstance().font.width(s) + 10;
			if (i > width) {
				width = i;
			}
			height += 10;
		}

		mouseX += 5;
		mouseY += 5;

		if (Minecraft.getInstance().screen.width < mouseX + width) {
			mouseX -= width + 10;
		}

		if (Minecraft.getInstance().screen.height < mouseY + height) {
			mouseY -= height + 10;
		}

		RenderUtils.setZLevelPre(matrix, 600);

		renderDescriptionBackground(matrix, mouseX, mouseY, width, height);

		RenderSystem.enableBlend();

		int i2 = 5;
		for (String s : descArray) {
			GuiComponent.drawString(matrix, Minecraft.getInstance().font, s, mouseX + 5, mouseY + i2, Color.WHITE.getRGB());
			i2 += 10;
		}

		RenderUtils.setZLevelPost(matrix);
		RenderSystem.disableBlend();

	}
	
}
