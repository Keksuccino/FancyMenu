package de.keksuccino.fancymenu.menu.systemtray;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;

public class FancyMenuTray {
	
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			init = true;
			
			System.setProperty("java.awt.headless", "false");
			try {
				Field f = GraphicsEnvironment.class.getDeclaredField("headless");
				f.setAccessible(true);
				f.set(GraphicsEnvironment.getLocalGraphicsEnvironment(), false);
			} catch (Exception e) {
				e.printStackTrace();
			}

			SystemTray tray = SystemTray.getSystemTray();
			PopupMenu pop = new PopupMenu();

			MenuItem item = new MenuItem("Reload Menu");
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					CustomizationHelper.getInstance().onReloadButtonPress();
				}
			});
			pop.add(item);
			
			MenuItem item2 = new MenuItem("Toggle Button Info");
			item2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					CustomizationHelper.getInstance().onInfoButtonPress();
				}
			});
			pop.add(item2);
			
			MenuItem item3 = new MenuItem("Toggle Menu Info");
			item3.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					CustomizationHelper.getInstance().onMoreInfoButtonPress();
				}
			});
			pop.add(item3);

			byte[] b = null;
			try {
				InputStream in = Minecraft.getInstance().getPackFinder().getVanillaPack().getResourceStream(ResourcePackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_16x16.png"));
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = in.read(data, 0, data.length)) != -1) {
				  buffer.write(data, 0, nRead);
				}
				b = buffer.toByteArray();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			TrayIcon icon = new TrayIcon(Toolkit.getDefaultToolkit().createImage(b), "FancyMenu", pop);
			icon.setImageAutoSize(true);
			
			try {
				tray.add(icon);
			} catch (AWTException e1) {
				e1.printStackTrace();
			}
		}
	}

}
