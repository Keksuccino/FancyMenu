package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;

public class ButtonCustomizationItem extends CustomizationItemBase {

	public AdvancedButton button;
	private String hoverLabel;
	private String hoverSound;
	private boolean hover = false;
	
	public ButtonCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addbutton")) {
			this.value = item.getEntryValue("label");
			if (this.value == null) {
				this.value = "";
			}
			String buttonaction = item.getEntryValue("buttonaction");
			String actionvalue = item.getEntryValue("value");
			String backNormal = item.getEntryValue("backgroundnormal");
			String backHover = item.getEntryValue("backgroundhovered");

			if (buttonaction == null) {
				return;
			}
			if (actionvalue == null) {
				actionvalue = "";
			}

			this.hoverSound = item.getEntryValue("hoversound");
			if (this.hoverSound != null) {
				this.hoverSound = this.hoverSound.replace("\\", "/");
				File f = new File(this.hoverSound);
				if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
					MenuCustomization.registerSound(this.hoverSound, this.hoverSound);
				} else {
					this.hoverSound = null;
				}
			}

			this.hoverLabel = item.getEntryValue("hoverlabel");
			
			String finalAction = actionvalue;
			if (buttonaction.equalsIgnoreCase("openlink")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					this.openWebLink(finalAction);
				});
			}
			if (buttonaction.equalsIgnoreCase("sendmessage")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					if (Minecraft.getMinecraft().world != null) {
						if (!MinecraftForge.EVENT_BUS.post(new ClientChatEvent(finalAction))) {
							Minecraft.getMinecraft().player.sendChatMessage(finalAction);
						}
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("quitgame")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					Minecraft.getMinecraft().shutdown();
				});
			}
			if (buttonaction.equalsIgnoreCase("joinserver")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					Minecraft.getMinecraft().displayGuiScreen(new GuiConnecting(Minecraft.getMinecraft().currentScreen, Minecraft.getMinecraft(), new ServerData("", finalAction, true)));
				});
			}
			if (buttonaction.equalsIgnoreCase("loadworld")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					if (Minecraft.getMinecraft().getSaveLoader().canLoadWorld(finalAction)) {
						Minecraft.getMinecraft().launchIntegratedServer(finalAction, finalAction, (WorldSettings)null);
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("openfile")) { //for files and folders
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					File f = new File(finalAction.replace("\\", "/"));
					if (f.exists()) {
						this.openFile(f);
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("prevbackground")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					MenuHandlerBase handler = MenuHandlerRegistry.getLastActiveHandler();
					if (handler != null) {
						int cur = handler.getCurrentBackgroundAnimationId();
						if (cur > 0) {
							for (IAnimationRenderer an : handler.backgroundAnimations()) {
								if (an instanceof AdvancedAnimation) {
									((AdvancedAnimation)an).stopAudio();
								}
							}
							handler.setBackgroundAnimation(cur-1);
						}
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("nextbackground")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					MenuHandlerBase handler = MenuHandlerRegistry.getLastActiveHandler();
					if (handler != null) {
						int cur = handler.getCurrentBackgroundAnimationId();
						if (cur < handler.backgroundAnimations().size()-1) {
							for (IAnimationRenderer an : handler.backgroundAnimations()) {
								if (an instanceof AdvancedAnimation) {
									((AdvancedAnimation)an).stopAudio();
								}
							}
							handler.setBackgroundAnimation(cur+1);
						}
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("opencustomgui")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					if (CustomGuiLoader.guiExists(finalAction)) {
						Minecraft.getMinecraft().displayGuiScreen(CustomGuiLoader.getGui(finalAction, Minecraft.getMinecraft().currentScreen, null));
					}
				});
			}
			
			if ((this.button != null) && (backNormal != null) && (backHover != null)) {
				File f = new File(backNormal.replace("\\", "/"));
				File f2 = new File(backHover.replace("\\", "/"));
				if (f.isFile() && f.exists() && f2.isFile() && f2.exists()) {
					this.button.setBackgroundTexture(TextureHandler.getResource(backNormal.replace("\\", "/")), TextureHandler.getResource(backHover.replace("\\", "/")));
				}
			}
		}
	}

	public void render(GuiScreen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		
		this.button.x = x;
		this.button.y = y;
		
		if (this.button.isMouseOver()) {
			if (this.hoverLabel != null) {
				this.button.displayString = this.hoverLabel;
			}
			if ((this.hoverSound != null) && !this.hover) {
				this.hover = true;
				SoundHandler.resetSound(this.hoverSound);
				SoundHandler.playSound(this.hoverSound);
			}
		} else {
			this.button.displayString = this.value;
			this.hover = false;
		}
		
		this.button.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
	}
	
	@Override
	public boolean shouldRender() {
		if (this.button == null) {
			return false;
		}
		return super.shouldRender();
	}
	
	private void openWebLink(String url) {
		try {
			String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			URL u = new URL(url);
			if (!Minecraft.IS_RUNNING_ON_MAC) {
				if (s.contains("win")) {
					Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
				} else {
					if (u.getProtocol().equals("file")) {
						url = url.replace("file:", "file://");
					}
					Runtime.getRuntime().exec(new String[]{"xdg-open", url});
				}
			} else {
				Runtime.getRuntime().exec(new String[]{"open", url});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	private void openFile(File f) {
		try {
			this.openWebLink(f.toURI().toURL().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public AdvancedButton getButton() {
		return this.button;
	}

}
