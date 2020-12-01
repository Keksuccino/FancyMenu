package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public class ButtonCustomizationItem extends CustomizationItemBase {

	public AdvancedButton button;
	private String hoverLabel;
	private String hoverSound;
	private boolean hover = false;
	private boolean onlyMultiplayer = false;
	private boolean onlySingleplayer = false;
	private boolean onlyOutgame = false;
	
	public ButtonCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addbutton")) {
			this.value = item.getEntryValue("label");
			if (this.value == null) {
				this.value = "";
			}
			this.value = MenuCustomization.convertString(this.value);
			
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
			actionvalue = MenuCustomization.convertString(actionvalue);

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
			if (this.hoverLabel != null) {
				this.hoverLabel = MenuCustomization.convertString(this.hoverLabel);
			}

			String onlyX = item.getEntryValue("onlydisplayin");
			if (onlyX != null) {
				if (onlyX.equalsIgnoreCase("outgame")) {
					this.onlyOutgame = true;
				}
				if (onlyX.equalsIgnoreCase("multiplayer")) {
					this.onlyMultiplayer = true;
				}
				if (onlyX.equalsIgnoreCase("singleplayer")) {
					this.onlySingleplayer = true;
				}
			}

			String finalAction = actionvalue;
			this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
				ButtonScriptEngine.runButtonAction(buttonaction, finalAction);
			});

			String click = item.getEntryValue("clicksound");
			if (click != null) {
				click.replace("\\", "/");
				click = MenuCustomization.convertString(click);
				File f = new File(click);
				
				if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
					SoundHandler.registerSound(f.getPath(), f.getPath());
					this.button.setClickSound(f.getPath());
				}
			}
			
			String desc = item.getEntryValue("description");
			if (desc != null) {
				this.button.setDescription(StringUtils.splitLines(desc.replace("§n", "&n"), "&n"));
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

	@Override
	public void render(Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		if (this.onlyOutgame && (Minecraft.getInstance().world != null)) {
			return;
		}

		if (this.onlyMultiplayer && ((Minecraft.getInstance().world == null) || Minecraft.getInstance().isSingleplayer())) {
			return;
		}

		if (this.onlySingleplayer && ((Minecraft.getInstance().world == null) || !Minecraft.getInstance().isSingleplayer())) {
			return;
		}

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		
		this.button.setX(x);
		this.button.setY(y);

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
