package de.keksuccino.core.gui.notifications;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NotificationHandler {
	
	private static String[] notification;
	private static String title;
	private static String branding;
	
	private static int tick = 1000;
	private static int animation = -100;
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new NotificationHandler());
	}
	
	public static void displayNotification(String title, String branding, String... notification) {
		NotificationHandler.branding = branding;
		NotificationHandler.notification = notification;
		NotificationHandler.title = title;
		tick = 0;
		animation = -100;
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (tick >= 1000) {
			return;
		}
		
		FontRenderer font = Minecraft.getInstance().fontRenderer;
		
		int length = 0;
		if (notification != null) {
			length = notification.length;
		}
		int height = 43 + (10*length);
		int width = 200;
		int x = (e.getGui().width / 2) - (width / 2);
		int y = animation;
		
		RenderSystem.enableBlend();

		IngameGui.fill(x, y, x + width, y + height, new Color(0, 0, 0, 240).getRGB());
		IngameGui.fill(x, y, x + width, y + 10, new Color(0, 0, 0, 240).getRGB());

		if (branding != null) {
			RenderSystem.pushMatrix();
			RenderSystem.scalef(0.8F, 0.8F, 0.8F);
			font.drawStringWithShadow(branding, ((e.getGui().width / 2) - (font.getStringWidth(branding) / 2)) / 0.8F, y + 2, Color.WHITE.getRGB());
			RenderSystem.popMatrix();
		}
		
		if (title != null) {
			font.drawStringWithShadow(title, (e.getGui().width / 2) - (font.getStringWidth(title) / 2), y + 13, Color.WHITE.getRGB());
		}
		
		int i = y + 33;
		if (notification != null) {
			for (String s : notification) {
				font.drawStringWithShadow(s, (e.getGui().width / 2) - (font.getStringWidth(s) / 2), i, Color.WHITE.getRGB());
				i += 10;
			}
		}
		
		if (animation < 0) {
			animation += 10;
		}
		
		tick++;
		
		
	}

}
