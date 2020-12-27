package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.gui.screen.Screen;

public class VanillaButtonCustomizationItem extends CustomizationItemBase {

	private ButtonData parent;
	
	private String normalLabel = "";
	private boolean hovered = false;

	public VanillaButtonCustomizationItem(PropertiesSection item, ButtonData parent) {
		super(item);
		this.parent = parent;

		if ((this.action != null) && (this.parent != null)) {
			if (this.action.equalsIgnoreCase("setbuttontexture")) {
				String backNormal = item.getEntryValue("backgroundnormal");
				String backHover = item.getEntryValue("backgroundhovered");
				backNormal = MenuCustomization.convertString(backNormal);
				backHover = MenuCustomization.convertString(backHover);

				ButtonCache.convertToAdvancedButton(this.parent.getId(), true);
				
				if (this.parent.getButton() instanceof AdvancedButton) {
					((AdvancedButton)this.parent.getButton()).setBackgroundTexture(TextureHandler.getResource(backNormal.replace("\\", "/")), TextureHandler.getResource(backHover.replace("\\", "/")));
				}
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
					if (this.value != null) {
						this.value = MenuCustomization.convertString(this.value);
					}
					this.normalLabel = this.parent.getButton().getMessage();
				}
			}

			if (this.action.equalsIgnoreCase("setbuttonclicksound")) {
				String path = item.getEntryValue("path");
				path = MenuCustomization.convertString(path);
				File f = new File(path);
				
				if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
					ButtonCache.convertToAdvancedButton(this.parent.getId(), true);
					
					if (this.parent.getButton() instanceof AdvancedButton) {
						SoundHandler.registerSound(f.getPath(), f.getPath());
						((AdvancedButton)this.parent.getButton()).setClickSound(f.getPath());
					}
				}
			}
		}
	}

	@Override
	public void render(Screen menu) throws IOException {
		if (this.parent != null) {
			if (this.action.equals("addhoversound")) {
				if (this.parent.getButton().isHovered() && !hovered && (this.value != null)) {
					SoundHandler.resetSound(this.value);
					SoundHandler.playSound(this.value);
					this.hovered = true;
				}
				if (!this.parent.getButton().isHovered()) {
					this.hovered = false;
				}
			}
			
			if (this.action.equals("sethoverlabel")) {
				if (this.value != null) {
					if (this.parent.getButton().isHovered()) {
						this.parent.getButton().setMessage(this.value);
					} else {
						this.parent.getButton().setMessage(this.normalLabel);
					}
				}
			}
			
		}
	}

}
