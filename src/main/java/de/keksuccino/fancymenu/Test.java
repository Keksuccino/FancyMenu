package de.keksuccino.fancymenu;

import java.awt.Color;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Test {

	int w = 0;
	int h = 0;
	
//	@SubscribeEvent
//	public void onTick(ClientTickEvent e) {
//		Screen s = Minecraft.getInstance().currentScreen;
//		if (s != null) {
//			double mcScale = Minecraft.getInstance().getMainWindow().calcGuiScale((int) Minecraft.getInstance().getMainWindow().getGuiScaleFactor(), Minecraft.getInstance().gameSettings.forceUnicodeFont);
//			float baseUIScale = 1.0F;
//			float sc = (float) (((double)baseUIScale) * (((double)baseUIScale) / mcScale));
//			
//			int wi = (int) (s.width / sc);
//			int hei = (int) (s.height / sc);
//			if (wi != w) {
//				System.out.println("width: " + wi);
//				w = wi;
//			}
//			if (hei != h) {
//				System.out.println("height: " + hei);
//				h = hei;
//			}
//		}
//	}

}
