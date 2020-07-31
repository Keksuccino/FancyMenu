package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.popup.NotificationPopup;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.Screen;
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
					if (Minecraft.getInstance().world != null) {
						if (!MinecraftForge.EVENT_BUS.post(new ClientChatEvent(finalAction))) {
							Minecraft.getInstance().player.sendChatMessage(finalAction);
						}
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("quitgame")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					Minecraft.getInstance().shutdown();
				});
			}
			if (buttonaction.equalsIgnoreCase("joinserver")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					Minecraft.getInstance().displayGuiScreen(new ConnectingScreen(Minecraft.getInstance().currentScreen, Minecraft.getInstance(), new ServerData("", finalAction, true)));
				});
			}
			if (buttonaction.equalsIgnoreCase("loadworld")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					if (Minecraft.getInstance().getSaveLoader().canLoadWorld(finalAction)) {
						Minecraft.getInstance().launchIntegratedServer(finalAction, finalAction, (WorldSettings)null);
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
						Minecraft.getInstance().displayGuiScreen(CustomGuiLoader.getGui(finalAction, Minecraft.getInstance().currentScreen, null));
					}
				});
			}
			if (buttonaction.equalsIgnoreCase("opengui")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					Screen s = GuiConstructor.tryToConstruct(finalAction);
					if (s != null) {
						Minecraft.getInstance().displayGuiScreen(s);
					} else {
						PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("custombuttons.action.opengui.cannotopengui")));
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

	public void render(Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		
		this.button.x = x;
		this.button.y = y;

		if (this.button.isHovered()) {
			if (this.hoverLabel != null) {
				this.button.setMessage(this.hoverLabel);
			}
			if ((this.hoverSound != null) && !this.hover) {
				this.hover = true;
				SoundHandler.resetSound(this.hoverSound);
				SoundHandler.playSound(this.hoverSound);
			}
		} else {
			this.button.setMessage(this.value);
			this.hover = false;
		}
		
		this.button.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
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
	
	public Long getId() {
		int ori = 0;
		if (this.orientation.equalsIgnoreCase("original")) {
			ori = 1;
		} else if (this.orientation.equalsIgnoreCase("top-left")) {
			ori = 2;
		} else if (this.orientation.equalsIgnoreCase("mid-left")) {
			ori = 3;
		} else if (this.orientation.equalsIgnoreCase("bottom-left")) {
			ori = 4;
		} else if (this.orientation.equalsIgnoreCase("top-centered")) {
			ori = 5;
		} else if (this.orientation.equalsIgnoreCase("mid-centered")) {
			ori = 6;
		} else if (this.orientation.equalsIgnoreCase("bottom-centered")) {
			ori = 7;
		} else if (this.orientation.equalsIgnoreCase("top-right")) {
			ori = 8;
		} else if (this.orientation.equalsIgnoreCase("mid-right")) {
			ori = 9;
		} else if (this.orientation.equalsIgnoreCase("bottom-right")) {
			ori = 10;
		}

		String idRaw = "00" + ori + "" + Math.abs(this.posX) + "" + Math.abs(this.posY) + "" + Math.abs(this.width);
		long id = 0;
		if (MathUtils.isLong(idRaw)) {
			id = Long.parseLong(idRaw);
		}
		
		return id;
	}

}
