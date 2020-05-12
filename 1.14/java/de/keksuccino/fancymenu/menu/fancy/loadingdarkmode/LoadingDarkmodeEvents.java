package de.keksuccino.fancymenu.menu.fancy.loadingdarkmode;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LoadingDarkmodeEvents {
	
	private static boolean init = false;
	private static volatile boolean startedTicking = false;
	
	public static void init() {
		if (!init) {
			init = true;
			MinecraftForge.EVENT_BUS.register(new LoadingDarkmodeEvents());
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!startedTicking) {
						try {
							tick();
							Thread.sleep(20);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}).start();
		}
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		startedTicking = true;
		tick();
//		System.out.println("test");
	}
	
	protected static void tick() {
		if (FancyMenu.config.getOrDefault("loadingscreendarkmode", false)) {
			if ((Minecraft.getInstance().loadingGui != null) && (Minecraft.getInstance().loadingGui instanceof ResourceLoadProgressGui) && !(Minecraft.getInstance().loadingGui instanceof DarkResourceLoadingScreen)) {
				ResourceLoadProgressGui screen = (ResourceLoadProgressGui) Minecraft.getInstance().loadingGui;
				Minecraft.getInstance().setLoadingGui(new DarkResourceLoadingScreen(Minecraft.getInstance(), DarkResourceLoadingScreen.getReloader(screen), DarkResourceLoadingScreen.getCallback(screen), DarkResourceLoadingScreen.getReloading(screen)));
			}
		}
	}

}
