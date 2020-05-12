package de.keksuccino.fancymenu.menu.fancy.item;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.core.resources.ExternalTextureHandler;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.WorldSettings;

public class ButtonCustomizationItem extends CustomizationItemBase {

	public AdvancedButton button;
	
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
			String finalAction = actionvalue;
			if (buttonaction.equalsIgnoreCase("openlink")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					this.openWebLink(finalAction);
				});
			}
			if (buttonaction.equalsIgnoreCase("sendmessage")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					if (Minecraft.getMinecraft().world != null) {
						Minecraft.getMinecraft().player.sendChatMessage(finalAction);
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
						this.openFile(new File(finalAction));
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
			
			if ((this.button != null) && (backNormal != null) && (backHover != null)) {
				File f = new File(backNormal.replace("\\", "/"));
				File f2 = new File(backHover.replace("\\", "/"));
				if (f.isFile() && f.exists() && f2.isFile() && f2.exists()) {
					this.button.setBackgroundTexture(ExternalTextureHandler.getResource(backNormal.replace("\\", "/")), ExternalTextureHandler.getResource(backHover.replace("\\", "/")));
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
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			    Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	private void openFile(File f) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				Desktop.getDesktop().open(f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
