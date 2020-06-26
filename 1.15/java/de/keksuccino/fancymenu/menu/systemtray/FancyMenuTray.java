package de.keksuccino.fancymenu.menu.systemtray;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;

public class FancyMenuTray {
	
	private static boolean init = false;
	
	public static boolean init() {
		if (!init) {
			try {
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
				InputStream in = Minecraft.getInstance().getPackFinder().getVanillaPack().getResourceStream(ResourcePackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_16x16.png"));
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];

				while ((nRead = in.read(data, 0, data.length)) != -1) {
				  buffer.write(data, 0, nRead);
				}
				b = buffer.toByteArray();
				
				TrayIcon icon = new TrayIcon(Toolkit.getDefaultToolkit().createImage(b), "FancyMenu", pop);
				icon.setImageAutoSize(true);
				
				tray.add(icon);
				
				init = true;
				
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

}
