package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class VanillaButtonCustomizationItem extends CustomizationItemBase {
	
	private GuiButton parent;
	private AdvancedButton button;
	
	private String normalLabel = "";
	private boolean hovered = false;
	
	public VanillaButtonCustomizationItem(PropertiesSection item, GuiButton parent) {
		super(item);
		this.parent = parent;

		if (this.action != null) {
			if (this.action.equalsIgnoreCase("setbuttontexture")) {
				String backNormal = item.getEntryValue("backgroundnormal");
				String backHover = item.getEntryValue("backgroundhovered");
				this.button = new AdvancedButton(parent.x, parent.y, parent.width, parent.height, parent.displayString, true, (press) -> {
					click();
				});
				this.button.setBackgroundTexture(TextureHandler.getResource(backNormal.replace("\\", "/")), TextureHandler.getResource(backHover.replace("\\", "/")));
			}
			
			if (this.action.equalsIgnoreCase("addhoversound")) {
				this.value = item.getEntryValue("path");
				if (this.value != null) {
					File f = new File(this.value);
					if (f.exists() && f.isFile()) {
						if (!SoundHandler.soundExists(this.value)) {
							MenuCustomization.registerSound(this.value, this.value);
						}
					} else {
						System.out.println("################### ERROR ###################");
						System.out.println("[FancyMenu] Soundfile '" + this.value + "'for 'addhoversound' customization action not found!");
						System.out.println("#############################################");
						this.value = null;
					}
				}
			}
			
			if (this.action.equalsIgnoreCase("sethoverlabel")) {
				this.value = item.getEntryValue("label");
				if (this.parent != null) {
					this.normalLabel = this.parent.displayString;
				}
			}
		}
	}

	@Override
	public void render(GuiScreen menu) throws IOException {
		if (this.parent != null) {
			
			//Sync custom button with its parent and render it
			if (this.button != null) {
				if (this.button.isMouseOver() && !this.parent.isMouseOver()) {
					this.setHovered(this.parent, true);
				}
				if (!this.button.isMouseOver() && this.parent.isMouseOver()) {
					this.setHovered(this.parent, false);
				}
				
				this.button.x = this.parent.x;
				this.button.y = this.parent.y;
				this.button.width = this.parent.width;
				this.button.height = this.parent.height;
				this.button.displayString = this.parent.displayString;

				this.button.displayString = this.parent.displayString;
				
				this.button.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
			}

			//Everything for "setbuttontexture" is done in the button sync above
			
			if (this.action.equals("addhoversound")) {
				if (this.parent.isMouseOver() && !hovered && (this.value != null)) {
					SoundHandler.resetSound(this.value);
					SoundHandler.playSound(this.value);
					this.hovered = true;
				}
				if (!this.parent.isMouseOver()) {
					this.hovered = false;
				}
			}
			
			if (this.action.equals("sethoverlabel")) {
				if (this.value != null) {
					if (this.parent.isMouseOver()) {
						this.parent.displayString = this.value;
					} else {
						this.parent.displayString = this.normalLabel;
					}
				}
			}
			
		}
	}

	private void setHovered(GuiButton w, boolean hovered) {
		try {
			Field f = ReflectionHelper.findField(GuiButton.class, "field_146123_n", "hovered");
			f.set(w, hovered);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void click() {
		this.parent.visible = true;
		this.parent.mousePressed(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY());
		this.parent.visible = false;
		
		try {
			Method m = ReflectionHelper.findMethod(GuiScreen.class, "actionPerformed", "func_146284_a", GuiButton.class);
			m.invoke(Minecraft.getMinecraft().currentScreen, this.parent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
